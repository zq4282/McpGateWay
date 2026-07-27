package com.zmm.mcp.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmm.mcp.domain.entity.HttpToolConfig;
import com.zmm.mcp.domain.mapper.HttpToolConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HTTP Tool 执行器：
 * 1. 根据 tool_id 查询 http_tool_config
 * 2. 渲染模板变量（{{key}} → 实际值）
 * 3. 发起 HTTP 请求并返回 JSON 结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpExecutor {

    private final HttpToolConfigMapper httpToolConfigMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\{\\{.*?\\}\\}");

    public Object execute(Long toolId, Map<String, Object> params) {
        HttpToolConfig config = httpToolConfigMapper.selectById(toolId);
        if (config == null) {
            throw new RuntimeException("HTTP Tool 配置不存在: toolId=" + toolId);
        }

        try {
            String url = renderTemplate(config.getUrl(), params);
            HttpHeaders headers = buildHeaders(config.getHeadersTemplate(), params);
            String method = config.getMethod().toUpperCase();

            ResponseEntity<String> response;

            if ("GET".equals(method)) {
                // GET：将参数追加为 Query String 并进行 URL 编码
                String queryTemplate = config.getQueryTemplate();
                String renderedQuery = renderTemplate(queryTemplate, params);
                String finalUrl = buildUrlWithQuery(url, renderedQuery);

                log.info("\n======================= [HTTP Tool GET 请求] =======================\nTool ID: {}\n请求 URL: {}\n入参 Map: {}\n==================================================================",
                        toolId, finalUrl, params);

                // 使用 java.net.URI 避免 RestTemplate 对已编码的 URL 进行二次编码(%E5 -> %25E5)
                URI targetUri = URI.create(finalUrl);
                response = restTemplate.exchange(targetUri, HttpMethod.GET,
                        new HttpEntity<>(headers), String.class);
            } else {
                // POST：渲染 body 模板，清理未替换的占位符
                String body = renderTemplate(config.getBodyTemplate(), params);
                body = cleanUnresolvedPlaceholders(body);

                log.info("\n======================= [HTTP Tool POST 请求] =======================\nTool ID: {}\n请求 URL: {}\n请求 Body: {}\n==================================================================",
                        toolId, url, body);

                URI targetUri = URI.create(url);
                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                response = restTemplate.exchange(targetUri, HttpMethod.POST, entity, String.class);
            }

            String responseBody = response.getBody();
            log.info("HTTP Tool [{}] 响应结果: {}", toolId, responseBody);

            if (!StringUtils.hasText(responseBody)) {
                return Map.of();
            }

            try {
                return objectMapper.readValue(responseBody,
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return responseBody;
            }

        } catch (Exception e) {
            log.error("HTTP Tool 执行失败: toolId={}, error={}", toolId, e.getMessage(), e);
            throw new RuntimeException("HTTP Tool 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 渲染模板：将 {{key}} 替换为 params 中对应的值
     */
    private String renderTemplate(String template, Map<String, Object> params) {
        if (!StringUtils.hasText(template) || params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                String placeholder = "{{" + entry.getKey() + "}}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 清理未被替换的 {{placeholder}} 字符串，防止传给服务端引发解析错误
     */
    private String cleanUnresolvedPlaceholders(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return UNRESOLVED_PLACEHOLDER.matcher(text).replaceAll("");
    }

    /**
     * 构建 HTTP Headers
     */
    private HttpHeaders buildHeaders(String headersTemplate, Map<String, Object> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.USER_AGENT, DEFAULT_USER_AGENT);

        if (!StringUtils.hasText(headersTemplate)) {
            return headers;
        }

        try {
            String rendered = cleanUnresolvedPlaceholders(renderTemplate(headersTemplate, params));
            Map<String, String> headerMap = objectMapper.readValue(rendered,
                    new TypeReference<Map<String, String>>() {});
            headerMap.forEach((k, v) -> {
                if (StringUtils.hasText(v)) {
                    headers.set(k, v);
                }
            });
        } catch (Exception e) {
            log.warn("解析 headers_template 失败: {}", e.getMessage());
        }

        return headers;
    }

    /**
     * 将 query_template 中的参数拼接到 URL（智能忽略未传参的未替换占位符，值进行 URL 编码）
     */
    private String buildUrlWithQuery(String url, String queryTemplate) {
        if (!StringUtils.hasText(queryTemplate)) {
            return url;
        }
        try {
            Map<String, String> queryMap = objectMapper.readValue(queryTemplate,
                    new TypeReference<Map<String, String>>() {});
            StringBuilder sb = new StringBuilder(url);
            sb.append(url.contains("?") ? "&" : "?");

            boolean hasParam = false;
            for (Map.Entry<String, String> entry : queryMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 过滤 null、空字符串以及未替换的占位符（如 {{lang}}）
                if (StringUtils.hasText(value) && !value.contains("{{")) {
                    try {
                        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                        sb.append(key).append("=").append(encodedValue).append("&");
                        hasParam = true;
                    } catch (Exception e) {
                        sb.append(key).append("=").append(value).append("&");
                        hasParam = true;
                    }
                }
            }

            if (hasParam && sb.charAt(sb.length() - 1) == '&') {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        } catch (Exception e) {
            log.warn("解析 query_template 失败: {}", e.getMessage());
            return url;
        }
    }
}
