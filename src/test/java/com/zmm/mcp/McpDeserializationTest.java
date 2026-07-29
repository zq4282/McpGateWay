package com.zmm.mcp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class McpDeserializationTest {

    @Test
    public void testDeserializeInitializeRequestWithUnknownFormCapability() throws Exception {
        String jsonPayload = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {
                      "elicitation": {
                        "form": {}
                      }
                    },
                    "clientInfo": {
                      "name": "test-client",
                      "version": "1.0.0"
                    }
                  }
                }
                """;

        // 1. 测试从 SPI 加载自定义的 McpJsonMapperSupplier
        ServiceLoader<McpJsonMapperSupplier> loader = ServiceLoader.load(McpJsonMapperSupplier.class);
        McpJsonMapperSupplier supplier = loader.findFirst().orElseThrow();
        McpJsonMapper mapper = supplier.get();

        McpSchema.JSONRPCRequest request = mapper.readValue(jsonPayload, McpSchema.JSONRPCRequest.class);
        assertNotNull(request);

        // 2. 测试配置了 FAIL_ON_UNKNOWN_PROPERTIES = false 的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        McpSchema.JSONRPCRequest request2 = objectMapper.readValue(jsonPayload, McpSchema.JSONRPCRequest.class);
        assertNotNull(request2);
    }
}
