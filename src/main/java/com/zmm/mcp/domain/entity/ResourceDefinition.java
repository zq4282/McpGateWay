package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resource 资源定义表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resource_definition")
public class ResourceDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资源唯一 URI 标识（例如：file:///docs/manual.md）
     */
    private String uri;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源描述
     */
    private String description;

    /**
     * 媒体类型 (MIME Type)，如 text/markdown, application/json, text/plain
     */
    private String mimeType;

    /**
     * 资源文本内容
     */
    private String content;

    /**
     * 是否启用（1：启用，0：禁用）
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
