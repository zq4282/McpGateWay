package com.zmm.mcp.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 静态 @Tool 方法回调 Provider 配置
 */
@Configuration
public class StaticToolConfig {

    @Bean
    public ToolCallbackProvider staticToolCallbackProvider(StaticTools staticTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(staticTools)
                .build();
    }
}
