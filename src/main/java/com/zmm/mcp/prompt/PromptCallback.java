package com.zmm.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;

/**
 * 单个 Prompt 的回调接口：
 * 遵循与 ToolCallback / WorkflowToolCallback 类似的结构设计，
 * 负责提供 Prompt 定义以及处理 prompts/get 获取和渲染请求。
 */
public interface PromptCallback {

    /**
     * 获取 Prompt 元数据定义（名称、描述、参数列表等）
     */
    Prompt getPromptDefinition();

    /**
     * 执行 Prompt 渲染逻辑并返回结果
     *
     * @param request 客户端传入的 GetPromptRequest
     * @return 包含描述与 PromptMessage 消息列表的 GetPromptResult
     */
    GetPromptResult get(GetPromptRequest request);
}
