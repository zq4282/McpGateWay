package com.zmm.mcp.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmm.mcp.domain.entity.ApiKey;
import com.zmm.mcp.domain.mapper.ApiKeyMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API-Key 认证过滤器（单一职责设计）：
 * 1. 验证 Authorization: Bearer <key> 凭证有效性。
 * 2. 验证通过后写入 ApiKeyContext (ThreadLocal 上下文)。
 * 3. 不参与任何 Response 修改，纯粹为后续 ToolCallback 与 Provider 提供身份上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只拦截 /mcp 路径，其他非 /mcp 请求不执行此 Filter
        String path = request.getRequestURI();
        return !path.startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        try {
            String apiKey = extractApiKey(request);

            if (!StringUtils.hasText(apiKey)) {
                log.warn("请求 {} 缺少 Authorization Header", path);
                sendUnauthorized(response, "Missing Authorization header");
                return;
            }

            // 验证 API-Key 是否有效且启用
            ApiKey entity = apiKeyMapper.selectOne(
                    new LambdaQueryWrapper<ApiKey>()
                            .eq(ApiKey::getApiKey, apiKey)
                            .eq(ApiKey::getStatus, 1)
            );

            if (entity == null) {
                log.warn("无效或禁用的 API-Key: {}", apiKey);
                sendUnauthorized(response, "Invalid or disabled API-Key");
                return;
            }

            // 存入 ThreadLocal 上下文，供后续 MCP Tool 组件优雅使用
            ApiKeyContext.set(apiKey);

            filterChain.doFilter(request, response);

        } finally {
            ApiKeyContext.clear();
        }
    }

    private String extractApiKey(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
