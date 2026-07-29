package com.zmm.mcp.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.auth.ApiKeyContext;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * MCP 统一调用日志切面：
 * 通过 Spring AOP 对 Tool 执行、Prompt 渲染以及 Protocol Handler 处理过程中的入参、出参和耗时进行统拦截与日志输出。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class McpLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 切点：拦截 PromptRegistry.executePrompt(GetPromptRequest request)
     */
    @Pointcut("execution(* com.zmm.mcp.prompt.PromptRegistry.executePrompt(..))")
    public void promptExecutePointcut() {}

    /**
     * 切点：拦截 WorkflowEngineImpl.execute(Long, java.util.Map)
     */
    @Pointcut("execution(* com.zmm.mcp.workflow.WorkflowEngine.execute(..))")
    public void workflowExecutePointcut() {}

    /**
     * 切点：拦截 WorkflowToolCallback.call(String)
     */
    @Pointcut("execution(* com.zmm.mcp.mcp.WorkflowToolCallback.call(..))")
    public void toolCallPointcut() {}

    /**
     * 环绕通知：拦截 Prompt 执行出入参日志
     */
    @Around("promptExecutePointcut()")
    public Object logPromptExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String apiKey = ApiKeyContext.get();
        Object[] args = joinPoint.getArgs();
        String promptName = "UNKNOWN";
        String argumentsStr = "{}";

        if (args != null && args.length > 0 && args[0] instanceof GetPromptRequest req) {
            promptName = req.name();
            argumentsStr = toJsonQuietly(req.arguments());
        }

        log.info("========== [MCP-AOP-LOG] Prompt 渲染请求开始 ==========");
        log.info("[MCP-AOP-LOG] API-Key: [{}] | PromptName: [{}]", apiKey, promptName);
        log.info("[MCP-AOP-LOG] 入参 Arguments: {}", argumentsStr);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[MCP-AOP-LOG] Prompt [{}] 渲染成功 | 耗时: {} ms", promptName, elapsed);
            log.info("[MCP-AOP-LOG] 出参 Result: {}", toJsonQuietly(result));
            log.info("========== [MCP-AOP-LOG] Prompt 渲染请求结束 ==========");
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[MCP-AOP-LOG] Prompt [{}] 渲染失败 | 耗时: {} ms | 异常: {}", promptName, elapsed, t.getMessage());
            log.info("========== [MCP-AOP-LOG] Prompt 渲染请求异常 ==========");
            throw t;
        }
    }

    /**
     * 环绕通知：拦截 Workflow 工具实际执行出入参日志
     */
    @Around("workflowExecutePointcut()")
    public Object logWorkflowExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String apiKey = ApiKeyContext.get();
        Object[] args = joinPoint.getArgs();
        Object toolId = (args != null && args.length > 0) ? args[0] : "UNKNOWN";
        Object inputMap = (args != null && args.length > 1) ? args[1] : null;

        log.info("---------- [MCP-AOP-LOG] Workflow 工具底层执行开始 ----------");
        log.info("[MCP-AOP-LOG] API-Key: [{}] | ToolID: [{}]", apiKey, toolId);
        log.info("[MCP-AOP-LOG] 入参 Input: {}", toJsonQuietly(inputMap));

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[MCP-AOP-LOG] Workflow Tool [{}] 执行完成 | 耗时: {} ms", toolId, elapsed);
            log.info("[MCP-AOP-LOG] 出参 Output: {}", toJsonQuietly(result));
            log.info("---------- [MCP-AOP-LOG] Workflow 工具底层执行结束 ----------");
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[MCP-AOP-LOG] Workflow Tool [{}] 执行异常 | 耗时: {} ms | 错误: {}", toolId, elapsed, t.getMessage());
            log.info("---------- [MCP-AOP-LOG] Workflow 工具底层执行异常 ----------");
            throw t;
        }
    }

    private String toJsonQuietly(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
