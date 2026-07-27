package com.zmm.mcp.admin;

import com.zmm.mcp.domain.entity.HttpToolConfig;
import com.zmm.mcp.domain.entity.ToolDefinition;
import com.zmm.mcp.domain.entity.WorkflowToolConfig;
import com.zmm.mcp.domain.mapper.HttpToolConfigMapper;
import com.zmm.mcp.domain.mapper.ToolDefinitionMapper;
import com.zmm.mcp.domain.mapper.WorkflowToolConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 管理接口（支持 WORKFLOW 和 HTTP 两种类型）
 */
@CrossOrigin
@RestController
@RequestMapping("/admin/tools")
@RequiredArgsConstructor
public class AdminToolController {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final WorkflowToolConfigMapper workflowToolConfigMapper;
    private final HttpToolConfigMapper httpToolConfigMapper;

    /** 查询所有 Tool */
    @GetMapping
    public List<ToolDefinition> list() {
        return toolDefinitionMapper.selectList(null);
    }

    /** 查询单个 Tool 的完整配置信息 */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        ToolDefinition tool = toolDefinitionMapper.selectById(id);
        if (tool == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new HashMap<>();
        result.put("id", tool.getId());
        result.put("name", tool.getName());
        result.put("type", tool.getType());
        result.put("description", tool.getDescription());
        result.put("inputSchema", tool.getInputSchema());
        result.put("outputSchema", tool.getOutputSchema());
        result.put("version", tool.getVersion());
        result.put("enabled", tool.getEnabled());
        result.put("createdTime", tool.getCreatedTime());

        if ("WORKFLOW".equals(tool.getType())) {
            WorkflowToolConfig config = workflowToolConfigMapper.selectById(id);
            if (config != null) {
                result.put("groovyScript", config.getGroovyScript());
            }
        } else {
            HttpToolConfig config = httpToolConfigMapper.selectById(id);
            if (config != null) {
                result.put("method", config.getMethod());
                result.put("url", config.getUrl());
                result.put("headersTemplate", config.getHeadersTemplate());
                result.put("queryTemplate", config.getQueryTemplate());
                result.put("bodyTemplate", config.getBodyTemplate());
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 新增 Tool（WORKFLOW 或 HTTP）
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        if (!"WORKFLOW".equals(type) && !"HTTP".equals(type)) {
            return ResponseEntity.badRequest().body(Map.of("error", "type 必须为 WORKFLOW 或 HTTP"));
        }

        ToolDefinition tool = new ToolDefinition();
        tool.setName((String) body.get("name"));
        tool.setType(type);
        tool.setDescription((String) body.get("description"));
        tool.setInputSchema((String) body.get("inputSchema"));
        tool.setOutputSchema((String) body.get("outputSchema"));
        tool.setVersion(1);
        tool.setEnabled(1);
        tool.setCreatedTime(LocalDateTime.now());
        toolDefinitionMapper.insert(tool);

        if ("WORKFLOW".equals(type)) {
            WorkflowToolConfig config = new WorkflowToolConfig();
            config.setToolId(tool.getId());
            config.setGroovyScript((String) body.get("groovyScript"));
            workflowToolConfigMapper.insert(config);
        } else {
            HttpToolConfig config = new HttpToolConfig();
            config.setToolId(tool.getId());
            config.setMethod((String) body.get("method"));
            config.setUrl((String) body.get("url"));
            config.setHeadersTemplate((String) body.get("headersTemplate"));
            config.setQueryTemplate((String) body.get("queryTemplate"));
            config.setBodyTemplate((String) body.get("bodyTemplate"));
            httpToolConfigMapper.insert(config);
        }

        return ResponseEntity.ok(Map.of("id", tool.getId(), "name", tool.getName(), "type", tool.getType()));
    }

    /** 修改 Tool */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ToolDefinition tool = toolDefinitionMapper.selectById(id);
        if (tool == null) return ResponseEntity.notFound().build();

        if (body.containsKey("name")) tool.setName((String) body.get("name"));
        if (body.containsKey("description")) tool.setDescription((String) body.get("description"));
        if (body.containsKey("inputSchema")) tool.setInputSchema((String) body.get("inputSchema"));
        tool.setVersion(tool.getVersion() + 1);
        toolDefinitionMapper.updateById(tool);

        if ("WORKFLOW".equals(tool.getType())) {
            if (body.containsKey("groovyScript")) {
                WorkflowToolConfig config = workflowToolConfigMapper.selectById(id);
                if (config == null) {
                    config = new WorkflowToolConfig();
                    config.setToolId(id);
                    config.setGroovyScript((String) body.get("groovyScript"));
                    workflowToolConfigMapper.insert(config);
                } else {
                    config.setGroovyScript((String) body.get("groovyScript"));
                    workflowToolConfigMapper.updateById(config);
                }
            }
        } else {
            HttpToolConfig config = httpToolConfigMapper.selectById(id);
            if (config == null) {
                config = new HttpToolConfig();
                config.setToolId(id);
                if (body.containsKey("method")) config.setMethod((String) body.get("method"));
                if (body.containsKey("url")) config.setUrl((String) body.get("url"));
                if (body.containsKey("headersTemplate")) config.setHeadersTemplate((String) body.get("headersTemplate"));
                if (body.containsKey("queryTemplate")) config.setQueryTemplate((String) body.get("queryTemplate"));
                if (body.containsKey("bodyTemplate")) config.setBodyTemplate((String) body.get("bodyTemplate"));
                httpToolConfigMapper.insert(config);
            } else {
                if (body.containsKey("method")) config.setMethod((String) body.get("method"));
                if (body.containsKey("url")) config.setUrl((String) body.get("url"));
                if (body.containsKey("headersTemplate")) config.setHeadersTemplate((String) body.get("headersTemplate"));
                if (body.containsKey("queryTemplate")) config.setQueryTemplate((String) body.get("queryTemplate"));
                if (body.containsKey("bodyTemplate")) config.setBodyTemplate((String) body.get("bodyTemplate"));
                httpToolConfigMapper.updateById(config);
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 更新 Workflow Tool 的 Groovy 脚本（同时版本号+1） */
    @PutMapping("/{id}/groovy-script")
    public ResponseEntity<?> updateGroovyScript(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        ToolDefinition tool = toolDefinitionMapper.selectById(id);
        if (tool == null) return ResponseEntity.notFound().build();
        if (!"WORKFLOW".equals(tool.getType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有 WORKFLOW 类型才有脚本"));
        }

        WorkflowToolConfig config = workflowToolConfigMapper.selectById(id);
        if (config == null) {
            config = new WorkflowToolConfig();
            config.setToolId(id);
            config.setGroovyScript(body.get("groovyScript"));
            workflowToolConfigMapper.insert(config);
        } else {
            config.setGroovyScript(body.get("groovyScript"));
            workflowToolConfigMapper.updateById(config);
        }

        tool.setVersion(tool.getVersion() + 1);
        toolDefinitionMapper.updateById(tool);

        return ResponseEntity.ok(Map.of("success", true, "version", tool.getVersion()));
    }

    /** 启用/禁用 Tool */
    @PutMapping("/{id}/enabled")
    public ResponseEntity<?> updateEnabled(@PathVariable Long id,
                                           @RequestBody Map<String, Integer> body) {
        ToolDefinition tool = toolDefinitionMapper.selectById(id);
        if (tool == null) return ResponseEntity.notFound().build();
        tool.setEnabled(body.getOrDefault("enabled", 1));
        toolDefinitionMapper.updateById(tool);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 删除 Tool */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        toolDefinitionMapper.deleteById(id);
        workflowToolConfigMapper.deleteById(id);
        httpToolConfigMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
