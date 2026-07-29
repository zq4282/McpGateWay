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
 * Prompt 定义表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("prompt_definition")
public class PromptDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Prompt 唯一名称标识
     */
    private String name;

    /**
     * Prompt 描述信息
     */
    private String description;

    /**
     * 参数列表 JSON 结构（对应 List<McpSchema.PromptArgument>）
     */
    private String argumentsJson;

    /**
     * Prompt 模板文本（支持 {{varName}} 占位符）
     */
    private String template;

    /**
     * 是否启用（1：启用，0：禁用）
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
