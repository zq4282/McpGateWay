package com.zmm.mcp.runtime;

import java.util.Map;

/**
 * Tool 调用接口，负责根据 tool_id 执行对应的 HTTP Tool
 */
public interface ToolInvoker {

    /**
     * @param toolId HTTP Tool 的 ID
     * @param params 调用参数（来自 Groovy 脚本）
     * @return 执行结果（通常为 Map 或基本类型）
     */
    Object invoke(Long toolId, Map<String, Object> params);
}
