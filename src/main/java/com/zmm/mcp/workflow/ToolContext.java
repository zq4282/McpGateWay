package com.zmm.mcp.workflow;

import com.zmm.mcp.runtime.ToolInvoker;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Groovy 脚本内可使用的 tools 对象。
 * 脚本示例：
 * <pre>
 *   def customer = tools.call(10001L, [id: 100])
 *   return [name: customer.name]
 * </pre>
 */
@RequiredArgsConstructor
public class ToolContext {

    private final ToolInvoker toolInvoker;

    /**
     * 在 Groovy 脚本中调用 HTTP Tool
     *
     * @param toolId HTTP Tool 的 ID
     * @param params 调用参数
     * @return 执行结果
     */
    public Object call(Long toolId, Map<String, Object> params) {
        return toolInvoker.invoke(toolId, params);
    }
}
