package com.zmm.mcp;

import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.prompt.PromptCallback;
import com.zmm.mcp.prompt.PromptRegistry;
import com.zmm.mcp.prompt.WorkflowPromptCallback;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PromptRegistryTest {

    private PromptRegistry promptRegistry;
    private ApiKeyAuthService apiKeyAuthService;

    @BeforeEach
    public void setUp() {
        promptRegistry = new PromptRegistry();
        apiKeyAuthService = Mockito.mock(ApiKeyAuthService.class);
    }

    @Test
    public void testRegisterAndListPrompts() {
        Prompt promptDef = new Prompt("test_prompt", "测试 Prompt", List.of(new PromptArgument("name", "姓名", true)));
        PromptCallback callback = new WorkflowPromptCallback(promptDef, "Hello {{name}}!", null, apiKeyAuthService);

        promptRegistry.registerPrompt(callback);

        List<Prompt> list = promptRegistry.listPrompts();
        assertEquals(1, list.size());
        assertEquals("test_prompt", list.get(0).name());
        assertEquals("测试 Prompt", list.get(0).description());
    }

    @Test
    public void testExecutePromptGetWithTemplateRendering() {
        Prompt promptDef = new Prompt("code_review", "Code Review Prompt", List.of(new PromptArgument("code", "代码", true)));
        PromptCallback callback = new WorkflowPromptCallback(
                promptDef,
                "请审阅以下代码：\n```java\n{{code}}\n```",
                null,
                apiKeyAuthService
        );

        promptRegistry.registerPrompt(callback);

        GetPromptRequest request = new GetPromptRequest("code_review", Map.of("code", "System.out.println(\"Hello\");"));
        GetPromptResult result = promptRegistry.executePrompt(request);

        assertNotNull(result);
        assertEquals("Code Review Prompt", result.description());
        assertEquals(1, result.messages().size());
        PromptMessage msg = result.messages().get(0);
        assertEquals(Role.USER, msg.role());
        assertTrue(msg.content() instanceof TextContent);
        TextContent textContent = (TextContent) msg.content();
        assertTrue(textContent.text().contains("System.out.println(\"Hello\");"));
    }

    @Test
    public void testPromptGetWithMissingPromptThrowsException() {
        GetPromptRequest request = new GetPromptRequest("non_existent_prompt", Map.of());
        assertThrows(RuntimeException.class, () -> promptRegistry.executePrompt(request));
    }
}
