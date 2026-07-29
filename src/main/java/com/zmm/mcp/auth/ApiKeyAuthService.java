package com.zmm.mcp.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.domain.entity.ApiKeyTool;
import com.zmm.mcp.domain.entity.ApiKey;
import com.zmm.mcp.domain.mapper.ApiKeyMapper;
import com.zmm.mcp.domain.mapper.ApiKeyToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * API-Key 工具授权校验服务
 * 用于在 tools/call 阶段验证"当前 API-Key 是否有权调用指定工具"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyToolMapper apiKeyToolMapper;
    private final com.zmm.mcp.domain.mapper.ApiKeyPromptMapper apiKeyPromptMapper;

    /**
     * 校验指定 API-Key 是否有权调用某个工具
     *
     * @param apiKey 当前请求的 API-Key 字符串
     * @param toolId 被调用的工具 ID（tool_definition.id）
     * @return true=有权限，false=无权限
     */
    public boolean isAllowed(String apiKey, Long toolId) {
        if (apiKey == null || toolId == null) {
            return false;
        }

        // 1. 查询 api_key 表，获取 id
        ApiKey keyEntity = apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getApiKey, apiKey)
                        .eq(ApiKey::getStatus, 1)
        );
        if (keyEntity == null) {
            log.warn("工具调用授权失败：API-Key 不存在或已禁用 [{}]", apiKey);
            return false;
        }

        // 2. 查询 api_key_tool 表，检查是否绑定且启用
        ApiKeyTool binding = apiKeyToolMapper.selectOne(
                new LambdaQueryWrapper<ApiKeyTool>()
                        .eq(ApiKeyTool::getApiKeyId, keyEntity.getId())
                        .eq(ApiKeyTool::getToolId, toolId)
                        .eq(ApiKeyTool::getEnabled, 1)
        );

        if (binding == null) {
            log.warn("工具调用授权失败：API-Key [{}] 无权调用 toolId={}", apiKey, toolId);
            return false;
        }

        return true;
    }

    /**
     * 校验指定 API-Key 是否有权调用某个 Prompt
     *
     * @param apiKey 当前请求的 API-Key 字符串
     * @param promptId 被调用的 Prompt ID（prompt_definition.id）
     * @return true=有权限，false=无权限
     */
    public boolean isPromptAllowed(String apiKey, Long promptId) {
        if (apiKey == null || promptId == null) {
            return false;
        }

        ApiKey keyEntity = apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getApiKey, apiKey)
                        .eq(ApiKey::getStatus, 1)
        );
        if (keyEntity == null) {
            log.warn("Prompt 调用授权失败：API-Key 不存在或已禁用 [{}]", apiKey);
            return false;
        }

        com.zmm.mcp.domain.entity.ApiKeyPrompt binding = apiKeyPromptMapper.selectOne(
                new LambdaQueryWrapper<com.zmm.mcp.domain.entity.ApiKeyPrompt>()
                        .eq(com.zmm.mcp.domain.entity.ApiKeyPrompt::getApiKeyId, keyEntity.getId())
                        .eq(com.zmm.mcp.domain.entity.ApiKeyPrompt::getPromptId, promptId)
                        .eq(com.zmm.mcp.domain.entity.ApiKeyPrompt::getEnabled, 1)
        );

        if (binding == null) {
            log.warn("Prompt 调用授权失败：API-Key [{}] 无权调用 promptId={}", apiKey, promptId);
            return false;
        }

        return true;
    }
}
