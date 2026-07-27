package com.zmm.mcp.common;

import lombok.Data;

/**
 * 统一返回结构
 * 成功: {"success":true, "data":{}, "metadata":{}}
 * 失败: {"success":false, "error":{"code":"xxx","message":"xxx"}}
 */
@Data
public class Result {

    private boolean success;
    private Object data;
    private ErrorInfo error;
    private Metadata metadata;

    public static Result ok(Object data, long executionTimeMs) {
        Result r = new Result();
        r.success = true;
        r.data = data;
        r.metadata = new Metadata(executionTimeMs);
        return r;
    }

    public static Result fail(String code, String message) {
        Result r = new Result();
        r.success = false;
        r.error = new ErrorInfo(code, message);
        return r;
    }

    @Data
    public static class ErrorInfo {
        private final String code;
        private final String message;
    }

    @Data
    public static class Metadata {
        private final long executionTime;
    }
}
