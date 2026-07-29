package com.zmm.mcp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;

/**
 * 自定义 McpJsonMapperSupplier SPI 实现
 * 确保 MCP JSON 反序列化时忽略未知属性（如客户端传入的拓展 capabilities 字段），
 * 防止抛出 UnrecognizedPropertyException (Unrecognized field "form")。
 */
public class CustomMcpJsonMapperSupplier implements McpJsonMapperSupplier {

    @Override
    public McpJsonMapper get() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JacksonMcpJsonMapper(objectMapper);
    }
}
