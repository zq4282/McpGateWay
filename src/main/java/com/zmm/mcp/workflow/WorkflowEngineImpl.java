package com.zmm.mcp.workflow;

import com.zmm.mcp.domain.entity.WorkflowToolConfig;
import com.zmm.mcp.domain.mapper.WorkflowToolConfigMapper;
import com.zmm.mcp.runtime.ToolInvoker;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Workflow 执行引擎实现：
 * 1. 加载 Groovy 脚本
 * 2. 创建独立 Binding（包含 input 参数和 tools 对象）
 * 3. 每次执行创建新的 GroovyShell，不共享实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngineImpl implements WorkflowEngine {

    private final WorkflowToolConfigMapper workflowToolConfigMapper;
    private final ToolInvoker toolInvoker;

    @Override
    public Object execute(Long workflowToolId, Map<String, Object> input) {
        WorkflowToolConfig config = workflowToolConfigMapper.selectById(workflowToolId);
        if (config == null || config.getGroovyScript() == null) {
            throw new RuntimeException("Workflow 脚本不存在: toolId=" + workflowToolId);
        }

        log.info("开始执行 Workflow Tool [{}], 脚本接收到的入参: {}", workflowToolId, input);

        // 每次执行创建新的独立 Binding，禁止共享
        Binding binding = new Binding();
        // 注入入参变量
        if (input != null) {
            input.forEach(binding::setVariable);
        }
        // 注入 tools 对象，供脚本调用 tools.call(toolId, params)
        binding.setVariable("tools", new ToolContext(toolInvoker));

        GroovyShell shell = new GroovyShell(binding);

        try {
            Object result = shell.evaluate(config.getGroovyScript());
            log.info("Workflow Tool [{}] 脚本执行完成，返回结果: {}", workflowToolId, result);
            return result;
        } catch (Exception e) {
            log.error("Workflow Tool [{}] 执行失败: {}", workflowToolId, e.getMessage(), e);
            throw new RuntimeException("Groovy 脚本执行失败: " + e.getMessage(), e);
        }
    }
}
