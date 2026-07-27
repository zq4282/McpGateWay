package com.zmm.mcp.mcp;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 静态 Tool 注册组件：
 * 使用 Spring AI 的 @Tool 注解实现。
 * 包含：
 * 1. get_current_time: 获取当前系统格式化时间
 * 2. get_random_number: 生成指定范围内的动态随机数
 */
@Component
public class StaticTools {

    /**
     * 获取当前系统时间
     */
    @Tool(name = "get_current_time", description = "获取当前系统标准格式化时间 (yyyy-MM-dd HH:mm:ss)")
    public Map<String, Object> getCurrentTime() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Map.of(
                "currentTime", now,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取指定范围内的动态随机数
     */
    @Tool(name = "get_random_number", description = "生成指定范围 [min, max] 内的动态随机整数")
    public Map<String, Object> getRandomNumber(int min, int max) {
        int effectiveMin = Math.min(min, max);
        int effectiveMax = Math.max(min, max);
        int randomVal = ThreadLocalRandom.current().nextInt(effectiveMin, effectiveMax + 1);

        return Map.of(
                "min", effectiveMin,
                "max", effectiveMax,
                "randomNumber", randomVal
        );
    }
}
