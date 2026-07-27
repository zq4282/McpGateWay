package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workflow_tool_config")
public class WorkflowToolConfig {

    @TableId
    private Long toolId;

    /** Groovy 脚本内容 */
    private String groovyScript;
}
