package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tool_definition")
public class ToolDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** WORKFLOW 或 HTTP */
    private String type;

    private String description;

    /** JSON Schema 字符串，描述入参 */
    private String inputSchema;

    /** JSON Schema 字符串，描述出参 */
    private String outputSchema;

    private Integer version;

    /** 1=启用，0=禁用 */
    private Integer enabled;

    private LocalDateTime createdTime;
}
