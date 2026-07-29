package com.zmm.mcp;

import com.zmm.mcp.resource.ResourceRegistry;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResourceDbAndAopTest {

    @Autowired
    private ResourceRegistry resourceRegistry;

    @Test
    public void testDatabaseResourcesLoaded() {
        List<Resource> resources = resourceRegistry.listResources();
        assertNotNull(resources);
        assertTrue(resources.size() >= 3, "从数据库装载的 Resource 数量应当至少有 3 个");

        boolean hasManual = resources.stream()
                .anyMatch(r -> "file:///docs/gateway_manual.md".equals(r.uri()));
        assertTrue(hasManual, "应当包含从数据库装载的 file:///docs/gateway_manual.md 资源");
    }

    @Test
    public void testReadResourceWithAopLog() {
        String uri = "file:///docs/gateway_manual.md";
        ReadResourceRequest request = new ReadResourceRequest(uri);

        // 执行读取 Resource（会触发 McpLogAspect 切面的 resourceReadPointcut 日志拦截）
        ReadResourceResult result = resourceRegistry.readResource(request);

        assertNotNull(result);
        assertEquals(1, result.contents().size());
        assertTrue(result.contents().get(0) instanceof TextResourceContents);

        TextResourceContents textContent = (TextResourceContents) result.contents().get(0);
        assertEquals(uri, textContent.uri());
        assertEquals("text/markdown", textContent.mimeType());
        assertTrue(textContent.text().contains("MCP Gateway 网关使用手册"));
    }
}
