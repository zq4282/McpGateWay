package com.zmm.mcp.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.auth.ApiKeyContext;
import com.zmm.mcp.common.Result;
import com.zmm.mcp.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * 单个 Workflow Tool 的回调实现：
 * 当 Agent 调用 tools/call 时，Spring AI 会调用此 callback 的 call() 方法。
 *
 * 安全机制：
 * - 执行前校验当前 API-Key 是否有权调用本工具（防止越权调用）
 * - API-Key 来自 ApiKeyContext（由 ApiKeyFilter 在请求入口写入 ThreadLocal）
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowToolCallback implements ToolCallback {

    private final ToolDefinition toolDefinition;
    private final Long toolId;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;
    private final ApiKeyAuthService apiKeyAuthService;

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        long startTime = System.currentTimeMillis();
        try {
            // ===== 授权校验：验证当前 API-Key 是否有权调用本工具 =====
            String apiKey = ApiKeyContext.get();
            if (apiKey == null) {
                log.warn("Tool [{}] 调用被拒绝：请求未携带 API-Key", toolDefinition.name());
                return toJson(Result.fail("UNAUTHORIZED", "请求未携带有效的 API-Key"));
            }
            if (!apiKeyAuthService.isAllowed(apiKey, toolId)) {
                log.warn("Tool [{}] 调用被拒绝：API-Key [{}] 无权调用此工具", toolDefinition.name(), apiKey);
                return toJson(Result.fail("FORBIDDEN", "当前 API-Key 无权调用工具：" + toolDefinition.name()));
            }
            // ===== 授权校验结束 =====

            log.debug("Tool [{}] 开始执行，入参: {}", toolDefinition.name(), toolInput);

            // 解析 JSON 入参
            Map<String, Object> input = parseInput(toolInput);

            // 执行 Workflow（Groovy 脚本）
            Object result = workflowEngine.execute(toolId, input);

            long elapsed = System.currentTimeMillis() - startTime;
            return toJson(Result.ok(result, elapsed));

        } catch (Exception e) {
            log.error("Tool [{}] 执行失败: {}", toolDefinition.name(), e.getMessage(), e);
            return toJson(Result.fail("EXECUTION_ERROR", e.getMessage()));
        }
    }

    private Map<String, Object> parseInput(String toolInput) {
        if (toolInput == null || toolInput.isBlank() || "{}".equals(toolInput.trim())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(toolInput, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Tool [{}] 入参解析失败，使用空 Map: {}", toolDefinition.name(), e.getMessage());
            return Map.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"success\":false,\"error\":{\"code\":\"SERIALIZATION_ERROR\",\"message\":\"结果序列化失败\"}}";
        }
    }
}
