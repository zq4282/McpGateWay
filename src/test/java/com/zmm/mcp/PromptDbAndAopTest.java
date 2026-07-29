package com.zmm.mcp;

import com.zmm.mcp.prompt.PromptRegistry;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PromptDbAndAopTest {

    @Autowired
    private PromptRegistry promptRegistry;

    @Test
    public void testDatabasePromptsLoaded() {
        List<Prompt> prompts = promptRegistry.listPrompts();
        assertNotNull(prompts);
        assertTrue(prompts.size() >= 3, "从数据库及静态加载的 Prompt 数量应该至少有 3 个");

        // 验证数据库预置的 Weather Advisor Prompt 存在
        boolean hasWeatherAdvisor = prompts.stream()
                .anyMatch(p -> "weather_advisor_prompt".equals(p.name()));
        assertTrue(hasWeatherAdvisor, "应当包含从数据库装载的 weather_advisor_prompt");
    }

    @Test
    public void testExecuteToolCallingPromptWithAopLog() {
        GetPromptRequest request = new GetPromptRequest(
                "weather_advisor_prompt",
                Map.of("city", "上海", "activity", "户外徒步")
        );

        // 执行 Prompt（会触发 McpLogAspect 切面日志拦截）
        GetPromptResult result = promptRegistry.executePrompt(request);

        assertNotNull(result);
        assertEquals(1, result.messages().size());
        TextContent textContent = (TextContent) result.messages().get(0).content();

        // 验证 Prompt 渲染后的模版中包含输入的城市与调用的工具说明
        assertTrue(textContent.text().contains("上海"));
        assertTrue(textContent.text().contains("户外徒步"));
        assertTrue(textContent.text().contains("get_weather"));
    }
}
