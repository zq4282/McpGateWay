package com.zmm.mcp.resource;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource Registry 注册中心：
 * 负责注册、管理与检索系统中的所有 MCP Resource Callbacks，
 * 并对外统一提供 resources/list 和 resources/read 执行入口。
 */
@Slf4j
@Component
public class ResourceRegistry {

    private final Map<String, ResourceCallback> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个新的 Resource Callback
     */
    public void registerResource(ResourceCallback callback) {
        if (callback == null || callback.getResourceDefinition() == null) {
            log.warn("跳过注册空的 ResourceCallback");
            return;
        }
        String uri = callback.getResourceDefinition().uri();
        registry.put(uri, callback);
        log.info("ResourceRegistry 成功注册 Resource: [{}]", uri);
    }

    /**
     * 根据 URI 查找 Resource Callback
     */
    public Optional<ResourceCallback> getResourceCallback(String uri) {
        if (uri == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(uri));
    }

    /**
     * 获取所有已注册 Resource 的定义列表
     */
    public List<Resource> listResources() {
        return registry.values().stream()
                .map(ResourceCallback::getResourceDefinition)
                .toList();
    }

    /**
     * 执行指定 URI 的 Resource 读取，并返回 ReadResourceResult
     */
    public ReadResourceResult readResource(ReadResourceRequest request) {
        if (request == null || request.uri() == null || request.uri().isBlank()) {
            throw new IllegalArgumentException("resources/read 请求缺少必要的 uri 参数");
        }

        ResourceCallback callback = registry.get(request.uri());
        if (callback == null) {
            log.warn("Resource 未找到: [{}]", request.uri());
            throw new NoSuchElementException("找不到指定的 Resource: " + request.uri());
        }

        return callback.read(request);
    }

    /**
     * 清空已注册的 Resource
     */
    public void clear() {
        registry.clear();
    }
}
