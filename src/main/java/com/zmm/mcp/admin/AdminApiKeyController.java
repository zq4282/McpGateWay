package com.zmm.mcp.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.domain.entity.ApiKey;
import com.zmm.mcp.domain.mapper.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API-Key 管理接口
 */
@CrossOrigin
@RestController
@RequestMapping("/admin/api-keys")
@RequiredArgsConstructor
public class AdminApiKeyController {

    private final ApiKeyMapper apiKeyMapper;

    /** 查询所有 API-Key */
    @GetMapping
    public List<ApiKey> list() {
        return apiKeyMapper.selectList(null);
    }

    /** 新增 API-Key */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String key = body.get("apiKey");
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "apiKey 不能为空"));
        }
        ApiKey entity = new ApiKey();
        entity.setApiKey(key);
        entity.setStatus(1);
        entity.setCreatedTime(LocalDateTime.now());
        apiKeyMapper.insert(entity);
        return ResponseEntity.ok(entity);
    }

    /** 启用/禁用 API-Key */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, Integer> body) {
        ApiKey entity = apiKeyMapper.selectById(id);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.setStatus(body.getOrDefault("status", 1));
        apiKeyMapper.updateById(entity);
        return ResponseEntity.ok(entity);
    }

    /** 删除 API-Key */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        apiKeyMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
