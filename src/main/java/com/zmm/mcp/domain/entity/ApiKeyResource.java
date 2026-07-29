package com.zmm.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-Key 与 Resource 关联关系实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("api_key_resource")
public class ApiKeyResource {

    private Long apiKeyId;

    private Long resourceId;

    private Integer enabled;
}
