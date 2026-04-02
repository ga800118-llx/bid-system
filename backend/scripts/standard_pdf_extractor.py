#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import fitz, re, json, sys, urllib.request, os

API_KEY = "sk-cp-rw9VUdvhTcQQ1O7U2wk0lpGMTbZH04NVwLWrLKKmKCh18AqC8jcvfGMBbXMnAEhxD3TqWAAOVDMx_Pd5E27y-wu4lDjydZae_Q64B8VXs4gieBl_ZY-fYpI"
API_URL = "https://api.minimaxi.com/v1/chat/completions"

FIELDS_DEFS = [
    ("projectName",        "projectName",         "项目名称（完整字符串）"),
    ("projectCode",        "projectCode",         "项目编号"),
    ("biddingAgency",      "biddingAgency",       "招标代理机构名称"),
    ("clientUnit",         "clientUnit",          "发标单位/招标人名称"),
    ("bidOpenTime",        "bidOpenTime",         "开标时间，YYYY-MM-DD格式（如2026-03-23），没有则null"),
    ("complaintDeadline",  "complaintDeadline",   "质疑截止时间，YYYY-MM-DD格式，没有则null"),
    ("ceilingPrice",       "ceilingPrice",        "最高投标限价（数字，单位元，不是万元，如64257547.76），没有则null"),
    ("floorPrice",         "floorPrice",          "下限价/成本警戒线（数字，单位元），没有则null"),
    ("contractPayment",    "contractPayment",     "付款方式（简述，最多200字）"),
    ("expertComposition",  "expertComposition",   "评标委员会组成（人数+来源）"),
    ("priceScoreMethod",   "priceScoreMethod",    "价格分计算方法（如"商务部分31分"）"),
    ("subjectiveScoreDetails","subjectiveScoreDetails","技术分/主观分说明（如"技术部分39分"）"),
    ("bidBond",            "bidBond",             "投标保证金（数字，单位元，如480000），没有则null"),
    ("performanceBond",    "performanceBond",     "履约保证金（数字，百分比数值如10表示10%），没有则null"),
]

SYSTEM_PROMPT = """你是一个专业的招标文档信息提取专家。你的任务是从招标文档PDF文本中提取结构化字段信息。

【字段定义】
""" + "\n".join(f"- {k}: {d}" for k,_,d in FIELDS_DEFS) + """

【重要规则】
- ceilingPrice和bidBond必须是数字（单位元），不能有"元"或"万元"字样
- performanceBond是百分比数值（如10表示10%）
- 日期格式必须为 YYYY-MM-DD（如2026-03-23），不是YYYY年MM月DD日
- 没有的字段写null，不要省略字段
- contractPayment最多200字
- 只返回标准JSON对象，不要任何解释、前缀或markdown格式
"""

def extract_by_llm(text):
    prompt = SYSTEM_PROMPT + "\n\n【招标文档文本】\n" + text[:20000]
    payload = {
        "model": "MiniMax-M2.7",
        "messages": [{"role": "user", "content": prompt}],
        "max_tokens": 2048,
        "temperature": 0.05
    }
    req = urllib.request.Request(
        API_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": "Bearer " + API_KEY
        }
    )
    try:
        r = urllib.request.urlopen(req, timeout=120)
        resp = json.loads(r.read().decode("utf-8"))
        content = resp["choices"][0]["message"]["content"].strip()
        # Strip thinking tags
        import re as re2
        content = re2.sub(r"<[^>]*>", "", content)
        # Extract JSON block
        start = content.find("{")
        end = content.rfind("}")
        if start >= 0 and end > start:
            content = content[start:end+1]
        return json.loads(content)
    except Exception as e:
        return {"error": str(e)}

def extract_by_regex(text):
    r = {}

    # projectName - look near projectCode
    for prefix in ["E520103", "2306-"]:
        idx = text.find(prefix)
        if idx >= 0:
            ls = text.rfind("\n", 0, idx)
            ts = text.rfind("\n", 0, ls-1) if ls > 0 else 0
            r["projectName"] = text[ts:ls].strip()[:200]
            break
    else:
        r["projectName"] = ""

    # projectCode
    m = re.search(r"E520103\d+WQ\d+|2306-\d{6}-\d{2}-\d{2}-\d{6}", text)
    r["projectCode"] = m.group(0) if m else ""

    # clientUnit and biddingAgency via section numbers
    lines = text.split("\n")
    cu = ba = ""
    sec = ""
    for line in lines:
        s = line.strip()
        if s == "1.1.2": sec = "client"
        elif s == "1.1.3": sec = "agency"
        elif re.match(r"1\.[12]\.", s): sec = ""
        elif sec and "\u540d\u79f0" in s:
            m2 = re.search(r"\u540d\u79f0\s*[\uff1a:]\s*([^\n]{2,80})", s)
            if m2:
                val = m2.group(1).strip()[:100]
                if sec == "client": cu = val
                else: ba = val
                sec = ""
    r["clientUnit"] = cu
    r["biddingAgency"] = ba

    # bidOpenTime
    idx = text.find("\u6295\u6801\u622a\u6b62\u65f6\u95f4")
    r["bidOpenTime"] = None
    if idx >= 0:
        chunk = text[idx:idx+200]
        m2 = re.search(r"(\d{4})\u5e74(\d+)\u6708[\s\S]{0,30}(\d+)\u65e5\s*(\d+)\u65f6(\d+)\u5206", chunk)
        if m2:
            r["bidOpenTime"] = "{}-{:02d}-{:02d}T{:02d}:{:02d}".format(
                m2.group(1), int(m2.group(2)), int(m2.group(3)), int(m2.group(4)), int(m2.group(5)))

    # complaintDeadline
    m = re.search("\u8d28\u7591\u622a\u6b62[^\d]*(\d{4})\u5e74(\d{1,2})\u6708(\d{1,2})\u65e5", text)
    r["complaintDeadline"] = "{}-{:02d}-{:02d}T09:30".format(m.group(1),int(m.group(2)),int(m.group(3))) if m else None

    # ceilingPrice
    m = re.search("\u6700\u9ad8\u6295\u6801\u9650\u4ef7[^\d]*([0-9,]+\.?\d*)", text)
    r["ceilingPrice"] = float(m.group(1).replace(",","")) if m and m.group(1) else None

    # floorPrice
    m = re.search("\u6210\u672c\u8b66\u793a\u7ebf[^\d]*([0-9,]+\.?\d*)", text)
    r["floorPrice"] = float(m.group(1).replace(",","")) if m and m.group(1) else None

    # contractPayment
    idx = text.find("\u5408\u540c\u91d1\u989d")
    r["contractPayment"] = text[idx:idx+500].replace("\r\n"," ").replace("\n"," ").strip()[:300] if idx >= 0 else ""

    # expertComposition
    idx = text.find("\u8bc4\u6807\u59d4\u5458\u4f1a")
    r["expertComposition"] = text[idx:idx+300].replace("\r\n"," ").replace("\n"," ")[:300] if idx >= 0 else ""

    # priceScoreMethod
    m = re.search("\u5546\u52a1\u90e8\u5206[\uff1a:\s]*(\d+)\u5206", text)
    r["priceScoreMethod"] = "\u5546\u52a1\u90e8\u5206{}".format(m.group(1)) if m else ""

    # subjectiveScoreDetails
    m = re.search("\u6280\u672f\u90e8\u5206[\uff1a:\s]*(\d+)\u5206", text)
    r["subjectiveScoreDetails"] = "\u6280\u672f\u90e8\u5206{}".format(m.group(1)) if m else ""

    # bidBond
    m = re.search("\u6295\u6801\u4fdd\u8bc1\u91d1[^\d\n]*(\d+)", text)
    r["bidBond"] = float(m.group(1)) if m else None

    # performanceBond
    m = re.search("\u5c65\u7ea6\u4fdd\u8bc1\u91d1[^\d]*(\d+)", text)
    if not m:
        m = re.search("\u5408\u540c\u91d1\u989d\s*(\d+)%", text)
    r["performanceBond"] = float(m.group(1)) if m else None

    return r

def extract(text):
    result = extract_by_llm(text)
    if "error" in result:
        print(json.dumps({"error": result["error"]}, ensure_ascii=False), file=sys.stderr)
        result = extract_by_regex(text)
    return result

if __name__ == "__main__":
    doc = fitz.open(sys.argv[1])
    text = "\n".join(page.get_text() for page in doc)
    doc.close()
    result = extract(text)
    print(json.dumps(result, ensure_ascii=False, indent=2))
