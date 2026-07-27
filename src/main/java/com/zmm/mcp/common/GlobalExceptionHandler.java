package com.zmm.mcp.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理器（仅覆盖 Admin REST 接口）
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.zmm.mcp.admin")
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        log.error("服务内部错误: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "服务内部错误", "message", e.getMessage()));
    }
}
