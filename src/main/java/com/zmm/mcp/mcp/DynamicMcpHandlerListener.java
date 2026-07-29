package com.zmm.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.auth.ApiKeyContext;
import com.zmm.mcp.domain.entity.ToolDefinition;
import com.zmm.mcp.domain.mapper.ToolDefinitionMapper;
import com.zmm.mcp.prompt.PromptRegistry;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态 MCP Handler 事件监听器：
 * 在 Spring Boot 完全启动后 (ApplicationReadyEvent)，装饰 WebMvcStatelessServerTransport 的 mcpHandler。
 * 1. 实现强类型 Reactor 管道层 JSONRPCResponse 动态工具隔离。
 * 2. 提供 Prompt Registry 协议层代理：拦截 prompts/list 与 prompts/get 请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpHandlerListener implements ApplicationListener<ApplicationReadyEvent> {

    private final WebMvcStatelessServerTransport transport;
    private final ToolDefinitionMapper toolDefinitionMapper;
    private final com.zmm.mcp.domain.mapper.PromptDefinitionMapper promptDefinitionMapper;
    private final StaticToolRegistry staticToolRegistry;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Field handlerField = WebMvcStatelessServerTransport.class.getDeclaredField("mcpHandler");
            handlerField.setAccessible(true);
            McpStatelessServerHandler originalHandler = (McpStatelessServerHandler) handlerField.get(transport);

            if (originalHandler != null && !(originalHandler instanceof DynamicHandlerDecorator)) {
                DynamicHandlerDecorator decorator = new DynamicHandlerDecorator(originalHandler);
                transport.setMcpHandler(decorator);
                log.info("成功向 MCP WebMvc ServerTransport 安装动态 Tools/Prompts Decorator 拦截器");
            }
        } catch (Exception e) {
            log.error("安装 DynamicMcpHandler 拦截器失败: {}", e.getMessage(), e);
        }
    }

    private class DynamicHandlerDecorator implements McpStatelessServerHandler {

        private final McpStatelessServerHandler delegate;

        public DynamicHandlerDecorator(McpStatelessServerHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<JSONRPCResponse> handleRequest(McpTransportContext context, JSONRPCRequest request) {
            if ("prompts/list".equals(request.method())) {
                return handlePromptsList(request);
            }
            if ("prompts/get".equals(request.method())) {
                return handlePromptsGet(request);
            }
            if ("server/discover".equals(request.method())) {
                log.info("收到客户端发起的 server/discover 请求，返回 Method Not Found 规范响应");
                JSONRPCResponse.JSONRPCError error = new JSONRPCResponse.JSONRPCError(-32601, "Method not found: server/discover", null);
                return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), null, error));
            }

            return delegate.handleRequest(context, request)
                    .map(response -> {
                        if ("tools/list".equals(request.method())) {
                            return filterToolsListResponse(response);
                        }
                        return response;
                    })
                    .onErrorResume(e -> {
                        log.warn("MCP 处理请求方法 [{}] 异常/缺失 Handler: {}", request.method(), e.getMessage());
                        JSONRPCResponse.JSONRPCError error = new JSONRPCResponse.JSONRPCError(-32601, "Method not found or unsupported: " + request.method(), null);
                        return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), null, error));
                    });
        }

        @Override
        public Mono<Void> handleNotification(McpTransportContext context, JSONRPCNotification notification) {
            return delegate.handleNotification(context, notification);
        }

        private Mono<JSONRPCResponse> handlePromptsList(JSONRPCRequest request) {
            try {
                List<Prompt> prompts = promptRegistry.listPrompts();
                String apiKey = ApiKeyContext.get();
                if (apiKey != null) {
                    List<com.zmm.mcp.domain.entity.PromptDefinition> allowedPrompts = promptDefinitionMapper.findPromptsByApiKey(apiKey);
                    Set<String> allowedNames = allowedPrompts.stream()
                            .map(com.zmm.mcp.domain.entity.PromptDefinition::getName)
                            .collect(Collectors.toSet());
                    // 补充允许静态全局预置 Prompt
                    allowedNames.add("code_review_prompt");
                    allowedNames.add("sql_generator_prompt");
                    allowedNames.add("text_summary_prompt");

                    prompts = prompts.stream()
                            .filter(p -> allowedNames.contains(p.name()))
                            .collect(Collectors.toList());
                }

                ListPromptsResult result = new ListPromptsResult(prompts, null, null);
                log.info("MCP Handler 成功处理 prompts/list，返回 Prompt {} 个", prompts.size());
                return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), result, null));
            } catch (Exception e) {
                log.error("处理 prompts/list 失败: {}", e.getMessage(), e);
                JSONRPCResponse.JSONRPCError error = new JSONRPCResponse.JSONRPCError(-32603, e.getMessage(), null);
                return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), null, error));
            }
        }

        private Mono<JSONRPCResponse> handlePromptsGet(JSONRPCRequest request) {
            try {
                GetPromptRequest getPromptReq;
                if (request.params() instanceof GetPromptRequest gpr) {
                    getPromptReq = gpr;
                } else {
                    getPromptReq = objectMapper.convertValue(request.params(), GetPromptRequest.class);
                }

                GetPromptResult result = promptRegistry.executePrompt(getPromptReq);
                return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), result, null));
            } catch (Exception e) {
                log.error("处理 prompts/get 失败: {}", e.getMessage(), e);
                JSONRPCResponse.JSONRPCError error = new JSONRPCResponse.JSONRPCError(-32603, e.getMessage(), null);
                return Mono.just(new JSONRPCResponse(request.jsonrpc(), request.id(), null, error));
            }
        }

        private JSONRPCResponse filterToolsListResponse(JSONRPCResponse response) {
            if (response == null || response.result() == null) {
                return response;
            }

            String apiKey = ApiKeyContext.get();
            if (apiKey == null) {
                return response;
            }

            if (response.result() instanceof ListToolsResult listResult) {
                // 1. 查库获取当前 API-Key 许可的 Workflow 工具名称
                List<ToolDefinition> allowedTools = toolDefinitionMapper.findWorkflowToolsByApiKey(apiKey);
                Set<String> allowedNames = allowedTools.stream()
                        .map(ToolDefinition::getName)
                        .collect(Collectors.toSet());

                // 2. 合并静态 @Tool 工具白名单
                Set<String> staticTools = staticToolRegistry.getStaticToolNames();
                if (staticTools != null) {
                    allowedNames.addAll(staticTools);
                }

                // 3. 内存级强类型过滤 List<Tool>
                List<Tool> filteredTools = listResult.tools().stream()
                        .filter(t -> allowedNames.contains(t.name()))
                        .collect(Collectors.toList());

                log.info("MCP Handler 成功为 API-Key [{}] 强类型裁剪工具列表 {} -> {} 个",
                        apiKey, listResult.tools().size(), filteredTools.size());

                ListToolsResult filteredResult = new ListToolsResult(
                        filteredTools,
                        listResult.nextCursor(),
                        listResult.meta()
                );

                return new JSONRPCResponse(
                        response.jsonrpc(),
                        response.id(),
                        filteredResult,
                        response.error()
                );
            }

            return response;
        }
    }
}

