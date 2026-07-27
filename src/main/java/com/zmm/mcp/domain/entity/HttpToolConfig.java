package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("http_tool_config")
public class HttpToolConfig {

    @TableId
    private Long toolId;

    /** GET 或 POST */
    private String method;

    /** 目标 URL，可含 {{变量}} */
    private String url;

    /** JSON 字符串，Header 模板，可含 {{变量}} */
    private String headersTemplate;

    /** JSON 字符串，Query 参数模板，可含 {{变量}} */
    private String queryTemplate;

    /** JSON 字符串，请求体模板，可含 {{变量}} */
    private String bodyTemplate;
}
