package com.zmm.mcp.mcp;

import com.zmm.mcp.auth.ApiKeyContext;
import com.zmm.mcp.domain.entity.ToolDefinition;
import com.zmm.mcp.domain.mapper.ToolDefinitionMapper;
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
 * 实现强类型 Reactor 管道层 JSONRPCResponse 动态隔离。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpHandlerListener implements ApplicationListener<ApplicationReadyEvent> {

    private final WebMvcStatelessServerTransport transport;
    private final ToolDefinitionMapper toolDefinitionMapper;
    private final StaticToolRegistry staticToolRegistry;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Field handlerField = WebMvcStatelessServerTransport.class.getDeclaredField("mcpHandler");
            handlerField.setAccessible(true);
            McpStatelessServerHandler originalHandler = (McpStatelessServerHandler) handlerField.get(transport);

            if (originalHandler != null && !(originalHandler instanceof DynamicHandlerDecorator)) {
                DynamicHandlerDecorator decorator = new DynamicHandlerDecorator(originalHandler);
                transport.setMcpHandler(decorator);
                log.info("成功在 ApplicationReadyEvent 时向 MCP WebMvc ServerTransport 安装动态授权 Decorator 拦截器");
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
            return delegate.handleRequest(context, request).map(response -> {
                if ("tools/list".equals(request.method())) {
                    return filterToolsListResponse(response);
                }
                return response;
            });
        }

        @Override
        public Mono<Void> handleNotification(McpTransportContext context, JSONRPCNotification notification) {
            return delegate.handleNotification(context, notification);
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
