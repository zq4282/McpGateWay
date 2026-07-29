package com.zmm.mcp.prompt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.domain.entity.PromptDefinition;
import com.zmm.mcp.domain.mapper.PromptDefinitionMapper;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 动态 Prompt 数据库装载器：
 * 系统启动后自动从数据库 prompt_definition 表加载已启用的 Prompt 记录，
 * 并将其注册到 PromptRegistry 注册中心。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicPromptLoader {

    private final PromptDefinitionMapper promptDefinitionMapper;
    private final PromptRegistry promptRegistry;
    private final ApiKeyAuthService apiKeyAuthService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void loadPromptsFromDatabase() {
        log.info("开始从数据库动态加载已启用的 Prompt 模板...");
        List<PromptDefinition> list = promptDefinitionMapper.selectList(
                new LambdaQueryWrapper<PromptDefinition>()
                        .eq(PromptDefinition::getEnabled, 1)
        );

        if (list == null || list.isEmpty()) {
            log.info("数据库中无已启用的 Prompt 记录");
            return;
        }

        int count = 0;
        for (PromptDefinition pd : list) {
            try {
                List<PromptArgument> arguments = parseArguments(pd.getArgumentsJson());
                Prompt promptDef = new Prompt(
                        pd.getName(),
                        pd.getDescription(),
                        arguments
                );
                WorkflowPromptCallback callback = new WorkflowPromptCallback(
                        promptDef,
                        pd.getTemplate(),
                        pd.getId(),
                        apiKeyAuthService
                );
                promptRegistry.registerPrompt(callback);
                count++;
            } catch (Exception e) {
                log.error("加载数据库 Prompt [{}] 失败: {}", pd.getName(), e.getMessage(), e);
            }
        }
        log.info("成功从数据库装载动态 Prompts 数量: {} 个", count);
    }

    private List<PromptArgument> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<List<PromptArgument>>() {});
        } catch (Exception e) {
            log.warn("解析 Prompt Arguments JSON 失败，设为空列表: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
