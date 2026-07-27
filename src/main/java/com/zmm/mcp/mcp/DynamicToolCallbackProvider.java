package com.zmm.mcp.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.auth.ApiKeyContext;
import com.zmm.mcp.domain.entity.ToolDefinition;
import com.zmm.mcp.domain.mapper.ToolDefinitionMapper;
import com.zmm.mcp.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 动态 Tool Provider：
 * 遵循纯粹的 MCP 架构，根据 ApiKeyContext (ThreadLocal) 动态提供当前请求 API-Key 授权绑定的 ToolCallbacks。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicToolCallbackProvider implements ToolCallbackProvider {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;
    private final ApiKeyAuthService apiKeyAuthService;

    @Override
    public ToolCallback[] getToolCallbacks() {
        String apiKey = ApiKeyContext.get();
        List<ToolDefinition> tools;

        if (apiKey != null) {
            // 请求阶段：根据当前上下文的 API-Key 查出授权的 Workflow Tools
            tools = toolDefinitionMapper.findWorkflowToolsByApiKey(apiKey);
            log.debug("动态 ToolProvider 为 API-Key [{}] 查出授权工具 {} 个", apiKey, tools.size());
        } else {
            // 启动初始化阶段：返回所有启用的 WORKFLOW Tool
            tools = toolDefinitionMapper.selectList(
                    new LambdaQueryWrapper<ToolDefinition>()
                            .eq(ToolDefinition::getEnabled, 1)
                            .eq(ToolDefinition::getType, "WORKFLOW")
            );
        }

        return tools.stream()
                .map(this::buildToolCallback)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback buildToolCallback(ToolDefinition toolDef) {
        org.springframework.ai.tool.definition.ToolDefinition definition =
                org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name(toolDef.getName())
                        .description(toolDef.getDescription() != null
                                ? toolDef.getDescription() : toolDef.getName())
                        .inputSchema(buildInputSchema(toolDef.getInputSchema()))
                        .build();

        return new WorkflowToolCallback(definition, toolDef.getId(), workflowEngine, objectMapper, apiKeyAuthService);
    }

    private String buildInputSchema(String inputSchema) {
        if (inputSchema != null && !inputSchema.isBlank()) {
            return inputSchema;
        }
        return """
                {
                  "type": "object",
                  "properties": {},
                  "additionalProperties": true
                }
                """;
    }
}
