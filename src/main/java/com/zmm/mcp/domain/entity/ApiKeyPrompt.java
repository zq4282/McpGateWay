package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-Key 与 Prompt 关联关系实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("api_key_prompt")
public class ApiKeyPrompt {

    private Long apiKeyId;

    private Long promptId;

    private Integer enabled;
}
