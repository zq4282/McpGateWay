package com.zmm.mcp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

/**
 * 应用级 Bean 配置
 * 优化 RestTemplate，支持忽略 SSL 证书校验，配置 10 秒超时
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection httpsConnection) {
                    try {
                        TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public X509Certificate[] getAcceptedIssuers() { return null; }
                                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                            }
                        };
                        SSLContext sc = SSLContext.getInstance("TLS");
                        sc.init(null, trustAllCerts, new java.security.SecureRandom());
                        httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    } catch (Exception ignored) {
                    }
                }
                super.prepareConnection(connection, httpMethod);
            }
        };

        factory.setConnectTimeout(10000); // 10 秒连接超时
        factory.setReadTimeout(10000);    // 10 秒读取超时

        return new RestTemplate(factory);
    }

    /**
     * 配置 Jackson 全局反序列化特性：忽略未知属性
     * 防止 MCP 客户端发送新协议特性或拓展字段时报错 UnrecognizedPropertyException
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 配置 Spring 环境下的 McpJsonMapper Bean
     */
    @Bean
    public McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
        return new JacksonMcpJsonMapper(objectMapper);
    }
}

