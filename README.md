# Dynamic MCP Gateway

基于 **Spring AI 1.1.0**、**SQLite** 与 **Groovy 动态脚本引擎** 的动态 MCP (Model Context Protocol) 网关。支持多租户 API-Key 凭证隔离、动态工具热插拔与可视化 Web 控制台。

---

## 🛠️ 工具展示（`tools/list`）与调用（`tools/call`）执行流程详解

在系统中，MCP 工具的**展示（列表裁剪）**与**调用（执行调度）**分别由框架的不同分层接管，整体流程如下：

```text
                                  ┌──────────────────────────┐
                                  │   POST /mcp (JSON-RPC)   │
                                  └────────────┬─────────────┘
                                               │
                                               ▼
                                 ┌───────────────────────────┐
                                 │   1. ApiKeyFilter         │ (身份认证 + ThreadLocal 注入)
                                 └─────────────┬─────────────┘
                                               │
               ┌───────────────────────────────┴───────────────────────────────┐
               │                                                               │
               ▼ (method: tools/list)                                          ▼ (method: tools/call)
┌──────────────────────────────────────────────┐                ┌──────────────────────────────────────────────┐
│  2. DynamicMcpHandlerListener (Handler装饰器)│                │  2. WorkflowToolCallback.call()              │
│     - 拦截 ListToolsResult Mono 对象        │                │     - 查库防止越权直调                        │
│     - 结合 ApiKeyContext 查库授权工具       │                └──────────────────────┬───────────────────────┘
│     - 强类型过滤 List<Tool> 对象列表         │                                       │
└──────────────────────┬───────────────────────┘                                       ▼
                       │                                                ┌──────────────────────────────────────────────┐
                       ▼                                                │  3. WorkflowEngineImpl (GroovyShell)         │
            返回专属工具列表 Response                                   │     - 独立 Binding 评估 Groovy 脚本         │
                                                                        └──────────────────────┬───────────────────────┘
                                                                                               │
                                                                                               ▼
                                                                        ┌──────────────────────────────────────────────┐
                                                                        │  4. HttpExecutor                             │
                                                                        │     - 模板渲染、占位符清理、URI 编码发包      │
                                                                        └──────────────────────┬───────────────────────┘
                                                                                               │
                                                                                               ▼
                                                                                   返回工具执行结果 Response
```

---

### 📋 一、 工具展示 (`tools/list`) 在哪里执行？

当客户端发送 JSON-RPC `method: "tools/list"` 请求时，工具展示过滤分为 **3 个关键步骤**：

#### 1. 身份验证与上下文注入
- **执行类**：[`com.zmm.mcp.auth.ApiKeyFilter`](src/main/java/com/zmm/mcp/auth/ApiKeyFilter.java)
- **流程说明**：
  1. 继承 `OncePerRequestFilter`，拦截 `/mcp` 路径请求。
  2. 提取 HTTP Header 中的 `Authorization: Bearer <API-Key>`。
  3. 查 SQLite 数据库校验 API-Key 的状态是否启用。
  4. 校验通过后，将 `apiKey` 写入 ThreadLocal 容器 `ApiKeyContext.set(apiKey)`。
  5. 调用 `filterChain.doFilter()` 放行请求。此过程零 Response 拦截、零字节流修改，极简高效。

#### 2. Spring AI MCP 框架默认生成全量响应
- **执行类**：Spring AI 内置的 `WebMvcStatelessServerTransport` 与 `McpStatelessAsyncServer`
- **流程说明**：
  - 应用启动时，`DynamicToolCallbackProvider` 会注册全量启用的 WORKFLOW 工具。
  - MCP 框架接收到 `tools/list` 请求后，生成包含系统中所有已注册工具规格的 `Mono<JSONRPCResponse>` 对象（其中 `result` 为 `ListToolsResult` 强类型 Java 对象）。

#### 3. MCP Handler 装饰器拦截与强类型对象裁剪
- **执行类**：[`com.zmm.mcp.mcp.DynamicMcpHandlerListener`](src/main/java/com/zmm/mcp/mcp/DynamicMcpHandlerListener.java)
- **流程说明**：
  1. 在 Spring Boot 应用就绪事件（`ApplicationReadyEvent`）时，监听器会将 `WebMvcStatelessServerTransport` 的底层 `mcpHandler` 包装为 `DynamicHandlerDecorator` 装饰器。
  2. 当拦截到 `tools/list` 请求的响应 Mono 管道时：
     - 从 `ApiKeyContext.get()` 读取当前请求的 API-Key。
     - 查 SQLite 数据库 `api_key_tool` 关联表，获取该 API-Key 允许调用的 Workflow 工具名称集合。
     - 读取 [`StaticToolRegistry`](src/main/java/com/zmm/mcp/mcp/StaticToolRegistry.java) 在启动时自动反射扫描得到的 `@Tool` 静态公共工具（如 `get_current_time`, `get_random_number`）。
     - 在内存中对 Java 对象 `ListToolsResult.tools()` 进行强类型 List 过滤，仅保留当前 Key 有权限的工具。
     - 重新构造过滤后的 `JSONRPCResponse` 输出给客户端。

---

### 🚀 二、 工具调用 (`tools/call`) 在哪里执行？

当客户端发送 JSON-RPC `method: "tools/call"` 请求（例如调用天气预报工具）时，工具调用分为 **5 个步骤**：

#### 1. 框架路由与回调入口
- **执行类**：[`com.zmm.mcp.mcp.WorkflowToolCallback`](src/main/java/com/zmm/mcp/mcp/WorkflowToolCallback.java)
- **流程说明**：
  - Spring AI MCP 框架接收到 `tools/call` 请求后，匹配请求中的 `"name": "get_weather"`，定位到相对应的 `WorkflowToolCallback` 适配器实例。
  - 自动触发其核心入口方法 **`call(String toolInput)`**。

#### 2. 双重越权校验 (Defense in Depth)
- **执行类**：[`com.zmm.mcp.auth.ApiKeyAuthService`](src/main/java/com/zmm/mcp/auth/ApiKeyAuthService.java)
- **流程说明**：
  - 在 `call()` 方法内部，首先从 `ApiKeyContext.get()` 获取当前 API-Key。
  - 调用 `apiKeyAuthService.isAllowed(apiKey, toolId)` 再次校验该 Key 是否绑有了该工具的执行权限。
  - 若未授权，直接拒绝并返回 403/FORBIDDEN 错误 JSON；若授权通过，进入后续执行引擎。

#### 3. Groovy 工作流引擎评估
- **执行类**：[`com.zmm.mcp.workflow.WorkflowEngineImpl`](src/main/java/com/zmm/mcp/workflow/WorkflowEngineImpl.java)
- **流程说明**：
  1. 为单次调用创建全新的 `GroovyShell` 和 `Binding` 对象（绝不共享，防止变量污染）。
  2. 将客户端传入的参数集合写入 `Binding`。
  3. 将上下文代理对象 `new ToolContext(toolInvoker)` 注入为 Groovy 脚本中的 `tools` 变量。
  4. 执行数据库中配置的 Groovy 脚本（如：`tools.call(10L, [city: city])`）。

#### 4. 工具调度与 HTTP 引擎发包
- **执行类**：[`com.zmm.mcp.runtime.ToolInvokerImpl`](src/main/java/com/zmm/mcp/runtime/ToolInvokerImpl.java) & [`com.zmm.mcp.runtime.HttpExecutor`](src/main/java/com/zmm/mcp/runtime/HttpExecutor.java)
- **流程说明**：
  1. Groovy 脚本中调用 `tools.call(httpToolId, params)` 触发 `ToolContext` -> `ToolInvokerImpl`。
  2. `HttpExecutor` 根据底层 HTTP 工具的配置渲染模板（替换 `{{city}}` 占位符）。
  3. 正则识别并剔除未传参的空占位符，防止拼接出无效 Query 参数。
  4. 格式化编码参数，并构造为 `java.net.URI` 对象（防止 Spring `RestTemplate` 二次编码）。
  5. 自动注入 Chrome User-Agent 发起真实的网络 HTTP GET/POST 请求。

#### 5. 结果组装与响应
- **流程说明**：
  - 目标三方 API 返回 HTTP 响应数据。
  - Groovy 脚本接收到结果，可进行加工、过滤或拼接。
  - 最终结果返回给 `WorkflowToolCallback`，包装为标准 MCP JSON-RPC 格式响应给 Agent/客户端。

---

## 🏃 快速开始

```bash
# 启动服务
mvn spring-boot:run

# 访问嵌入式 Web 控制台与 MCP Playground
http://localhost:8080/
```
