package com.zmm.mcp.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmm.mcp.domain.entity.ToolDefinition;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ToolDefinitionMapper extends BaseMapper<ToolDefinition> {

    /**
     * 根据 API-Key 查询该 Key 绑定的、启用的 WORKFLOW 类型 Tool
     */
    @Select("""
            SELECT td.*
            FROM tool_definition td
            INNER JOIN api_key_tool akt ON td.id = akt.tool_id
            INNER JOIN api_key ak ON ak.id = akt.api_key_id
            WHERE ak.api_key = #{apiKey}
              AND ak.status = 1
              AND akt.enabled = 1
              AND td.enabled = 1
              AND td.type = 'WORKFLOW'
            """)
    List<ToolDefinition> findWorkflowToolsByApiKey(@Param("apiKey") String apiKey);
}
