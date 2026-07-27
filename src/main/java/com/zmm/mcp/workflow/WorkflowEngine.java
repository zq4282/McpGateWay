package com.zmm.mcp.workflow;

import java.util.Map;

/**
 * Workflow 执行引擎接口
 */
public interface WorkflowEngine {

    /**
     * 执行 Workflow Tool
     *
     * @param workflowToolId tool_definition 表中 WORKFLOW 类型 Tool 的 ID
     * @param input          Agent 传入的参数
     * @return 执行结果
     */
    Object execute(Long workflowToolId, Map<String, Object> input);
}
