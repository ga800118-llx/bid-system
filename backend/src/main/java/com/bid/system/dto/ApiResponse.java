package com.bid.system.dto;

public class ApiResponse {
    private int code;
    private String msg;
    private Object data;
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public static ApiResponse success() { return success(null); }
    public static ApiResponse success(Object data) {
        ApiResponse resp = new ApiResponse();
        resp.setCode(200); resp.setMsg("操作成功"); resp.setData(data);
        return resp;
    }
    public static ApiResponse error(String msg) {
        ApiResponse resp = new ApiResponse();
        resp.setCode(500); resp.setMsg(msg);
        return resp;
    }
    public static ApiResponse error(int code, String msg) {
        ApiResponse resp = new ApiResponse();
        resp.setCode(code); resp.setMsg(msg);
        return resp;
    }
}