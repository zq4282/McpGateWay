package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("api_key_tool")
public class ApiKeyTool {

    @TableId(type = IdType.INPUT)
    private Long apiKeyId;

    private Long toolId;

    /** 1=启用，0=禁用 */
    private Integer enabled;
}
