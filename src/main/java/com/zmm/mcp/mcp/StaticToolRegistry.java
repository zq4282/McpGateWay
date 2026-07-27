package com.zmm.mcp.mcp;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 静态 Tool 自动注册与识别器：
 * 在启动时自动反射扫描所有带 @Tool 注解的方法，自动提取 Tool 名称。
 * 供 ApiKeyFilter 自动无感放行，无需任何硬编码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaticToolRegistry {

    private final StaticTools staticTools;

    @Getter
    private Set<String> staticToolNames = Collections.emptySet();

    @PostConstruct
    public void init() {
        Set<String> names = new HashSet<>();
        Method[] methods = staticTools.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool tool = method.getAnnotation(Tool.class);
                String name = (tool.name() != null && !tool.name().isBlank()) ? tool.name() : method.getName();
                names.add(name);
            }
        }
        this.staticToolNames = Collections.unmodifiableSet(names);
        log.info("StaticToolRegistry 自动扫描检测到的 @Tool 静态公共工具: {}", this.staticToolNames);
    }
}
