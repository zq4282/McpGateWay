package com.zmm.mcp.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.auth.ApiKeyAuthService;
import com.zmm.mcp.domain.entity.ResourceDefinition;
import com.zmm.mcp.domain.mapper.ResourceDefinitionMapper;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 动态 Resource 数据库装载器：
 * 系统启动后自动从数据库 resource_definition 表加载已启用的 Resource 记录，
 * 并将其注册到 ResourceRegistry 注册中心。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicResourceLoader {

    private final ResourceDefinitionMapper resourceDefinitionMapper;
    private final ResourceRegistry resourceRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void loadResourcesFromDatabase() {
        log.info("开始从数据库动态加载已启用的 Resource 资源...");
        List<ResourceDefinition> list = resourceDefinitionMapper.selectList(
                new LambdaQueryWrapper<ResourceDefinition>()
                        .eq(ResourceDefinition::getEnabled, 1)
        );

        if (list == null || list.isEmpty()) {
            log.info("数据库中无已启用的 Resource 记录");
            return;
        }

        int count = 0;
        for (ResourceDefinition rd : list) {
            try {
                Resource resourceDef = new Resource(
                        rd.getUri(),
                        rd.getName(),
                        rd.getDescription(),
                        rd.getMimeType(),
                        null
                );
                WorkflowResourceCallback callback = new WorkflowResourceCallback(
                        resourceDef,
                        rd.getContent()
                );
                resourceRegistry.registerResource(callback);
                count++;
            } catch (Exception e) {
                log.error("加载数据库 Resource [{}] 失败: {}", rd.getUri(), e.getMessage(), e);
            }
        }
        log.info("成功从数据库装载动态 Resources 数量: {} 个", count);
    }
}
