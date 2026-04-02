#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import fitz, sys, os

pdf_path = sys.argv[1]
out_path = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("TMP", "/tmp") + "\\pdf_text_out.txt"

doc = fitz.open(pdf_path)
text = "\n".join(page.get_text() for page in doc)
doc.close()

with open(out_path, "w", encoding="utf-8") as f:
    f.write(text)
print(out_path, end="")