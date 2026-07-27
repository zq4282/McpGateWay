package com.zmm.mcp.runtime;

import com.zmm.mcp.domain.entity.ToolDefinition;
import com.zmm.mcp.domain.mapper.ToolDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ToolInvoker 实现：查询 tool_definition 类型，路由到对应执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolInvokerImpl implements ToolInvoker {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final HttpExecutor httpExecutor;

    @Override
    public Object invoke(Long toolId, Map<String, Object> params) {
        ToolDefinition tool = toolDefinitionMapper.selectById(toolId);
        if (tool == null) {
            throw new RuntimeException("Tool 不存在: toolId=" + toolId);
        }

        if (!"HTTP".equals(tool.getType())) {
            throw new RuntimeException("Groovy 脚本只能调用 HTTP 类型 Tool，toolId=" + toolId);
        }

        log.debug("调用 HTTP Tool: {} (id={})", tool.getName(), toolId);
        return httpExecutor.execute(toolId, params);
    }
}
