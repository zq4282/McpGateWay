package com.zmm.mcp.resource;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 单个 Resource 的回调实现类：
 * 当 MCP 客户端发起 resources/read 请求时执行此 Callback。
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowResourceCallback implements ResourceCallback {

    private final Resource resourceDefinition;
    private final String content;

    @Override
    public Resource getResourceDefinition() {
        return resourceDefinition;
    }

    @Override
    public ReadResourceResult read(ReadResourceRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Resource [{}] 开始读取内容", resourceDefinition.uri());

            TextResourceContents resourceContents = new TextResourceContents(
                    resourceDefinition.uri(),
                    resourceDefinition.mimeType(),
                    content != null ? content : ""
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Resource [{}] 读取完成，耗时: {} ms", resourceDefinition.uri(), elapsed);

            return new ReadResourceResult(List.of(resourceContents));

        } catch (Exception e) {
            log.error("Resource [{}] 读取失败: {}", resourceDefinition.uri(), e.getMessage(), e);
            throw new RuntimeException("Resource 读取失败: " + e.getMessage(), e);
        }
    }
}
