package com.zmm.mcp.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.domain.entity.ApiKeyTool;
import com.zmm.mcp.domain.mapper.ApiKeyToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API-Key 与 Tool 绑定关系管理接口
 */
@CrossOrigin
@RestController
@RequestMapping("/admin/api-key-tools")
@RequiredArgsConstructor
public class AdminApiKeyToolController {

    private final ApiKeyToolMapper apiKeyToolMapper;

    /** 查询某 API-Key 绑定的所有 Tool */
    @GetMapping
    public List<ApiKeyTool> list(@RequestParam Long apiKeyId) {
        return apiKeyToolMapper.selectList(
                new LambdaQueryWrapper<ApiKeyTool>()
                        .eq(ApiKeyTool::getApiKeyId, apiKeyId)
        );
    }

    /** 绑定 API-Key 与 Tool */
    @PostMapping
    public ResponseEntity<?> bind(@RequestBody Map<String, Long> body) {
        Long apiKeyId = body.get("apiKeyId");
        Long toolId = body.get("toolId");
        if (apiKeyId == null || toolId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "apiKeyId 和 toolId 不能为空"));
        }
        ApiKeyTool entity = new ApiKeyTool();
        entity.setApiKeyId(apiKeyId);
        entity.setToolId(toolId);
        entity.setEnabled(1);
        apiKeyToolMapper.insert(entity);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 启用/禁用绑定关系 */
    @PutMapping("/enabled")
    public ResponseEntity<?> updateEnabled(@RequestBody Map<String, Object> body) {
        Long apiKeyId = Long.valueOf(body.get("apiKeyId").toString());
        Long toolId = Long.valueOf(body.get("toolId").toString());
        Integer enabled = Integer.valueOf(body.get("enabled").toString());

        ApiKeyTool entity = apiKeyToolMapper.selectOne(
                new LambdaQueryWrapper<ApiKeyTool>()
                        .eq(ApiKeyTool::getApiKeyId, apiKeyId)
                        .eq(ApiKeyTool::getToolId, toolId)
        );
        if (entity == null) return ResponseEntity.notFound().build();
        entity.setEnabled(enabled);
        apiKeyToolMapper.updateById(entity);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 解除绑定 */
    @DeleteMapping
    public ResponseEntity<?> unbind(@RequestParam Long apiKeyId, @RequestParam Long toolId) {
        apiKeyToolMapper.delete(
                new LambdaQueryWrapper<ApiKeyTool>()
                        .eq(ApiKeyTool::getApiKeyId, apiKeyId)
                        .eq(ApiKeyTool::getToolId, toolId)
        );
        return ResponseEntity.ok(Map.of("success", true));
    }
}
