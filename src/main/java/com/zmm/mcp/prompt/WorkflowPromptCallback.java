package com.zmm.mcp.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.auth.ApiKeyContext;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单个 Prompt 的回调实现类（参考 WorkflowToolCallback 的架构设计）：
 * 当 MCP 客户端发起 prompts/get 请求时执行此 Callback。
 *
 * 功能特性：
 * 1. 模版渲染：解析客户端传入的 arguments 参数，对模版文本中的 {{varName}} 动态占位符进行精准替换。
 * 2. 授权校验：安全匹配 ApiKeyContext (ThreadLocal)，拦截未授权访问。
 * 3. 结果构建：封装返回标准 MCP 规范的 GetPromptResult 消息。
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowPromptCallback implements PromptCallback {

    private final Prompt promptDefinition;
    private final String template;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    @Override
    public Prompt getPromptDefinition() {
        return promptDefinition;
    }

    @Override
    public GetPromptResult get(GetPromptRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Prompt [{}] 开始渲染，入参: {}", promptDefinition.name(), request.arguments());

            // ===== 2. 参数提取与模版渲染 =====
            Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Collections.emptyMap();
            String renderedContent = renderTemplate(template, arguments);

            // ===== 3. 构建 MCP 标准 PromptMessage =====
            PromptMessage userMessage = new PromptMessage(
                    Role.USER,
                    new TextContent(renderedContent)
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Prompt [{}] 渲染完成，耗时: {} ms", promptDefinition.name(), elapsed);

            return new GetPromptResult(
                    promptDefinition.description(),
                    List.of(userMessage)
            );

        } catch (Exception e) {
            log.error("Prompt [{}] 执行/渲染失败: {}", promptDefinition.name(), e.getMessage(), e);
            throw new RuntimeException("Prompt 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模版渲染方法：将 {{key}} 动态替换为 arguments 中对应的值
     */
    private String renderTemplate(String templateStr, Map<String, Object> args) {
        if (templateStr == null || templateStr.isBlank()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateStr);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = args.get(key);
            String replacement = (value != null) ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
