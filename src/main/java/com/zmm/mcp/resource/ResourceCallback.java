package com.zmm.mcp.resource;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;

/**
 * 单个 Resource 的回调接口：
 * 遵循与 PromptCallback / ToolCallback 类似的架构设计，
 * 负责提供 Resource 定义以及处理 resources/read 读取和渲染请求。
 */
public interface ResourceCallback {

    /**
     * 获取 Resource 元数据定义（URI、名称、描述、MIME类型等）
     */
    Resource getResourceDefinition();

    /**
     * 读取 Resource 内容逻辑并返回结果
     *
     * @param request 客户端传入的 ReadResourceRequest
     * @return 包含 ResourceContents 的 ReadResourceResult
     */
    ReadResourceResult read(ReadResourceRequest request);
}
