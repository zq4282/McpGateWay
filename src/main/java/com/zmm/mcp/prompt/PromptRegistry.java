package com.zmm.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt Registry 注册中心：
 * 负责注册、管理与检索系统中的所有 MCP Prompt Callbacks，
 * 并对外统一提供 prompts/list 和 prompts/get 执行入口。
 */
@Slf4j
@Component
public class PromptRegistry {

    private final Map<String, PromptCallback> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个新的 Prompt Callback
     */
    public void registerPrompt(PromptCallback callback) {
        if (callback == null || callback.getPromptDefinition() == null) {
            log.warn("跳过注册空的 PromptCallback");
            return;
        }
        String promptName = callback.getPromptDefinition().name();
        registry.put(promptName, callback);
        log.info("PromptRegistry 成功注册 Prompt: [{}]", promptName);
    }

    /**
     * 根据名称查找 Prompt Callback
     */
    public Optional<PromptCallback> getPromptCallback(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(name));
    }

    /**
     * 获取所有已注册 Prompt 的定义列表
     */
    public List<Prompt> listPrompts() {
        return registry.values().stream()
                .map(PromptCallback::getPromptDefinition)
                .toList();
    }

    /**
     * 执行指定名称的 Prompt，并返回 GetPromptResult
     */
    public GetPromptResult executePrompt(GetPromptRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("prompts/get 请求缺少必要的 name 参数");
        }

        PromptCallback callback = registry.get(request.name());
        if (callback == null) {
            log.warn("Prompt 未找到: [{}]", request.name());
            throw new NoSuchElementException("找不到指定的 Prompt: " + request.name());
        }

        return callback.get(request);
    }

    /**
     * 清空已注册的 Prompt
     */
    public void clear() {
        registry.clear();
    }
}
