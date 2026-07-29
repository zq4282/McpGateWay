package com.zmm.mcp.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmm.mcp.domain.entity.ResourceDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResourceDefinitionMapper extends BaseMapper<ResourceDefinition> {

    /**
     * 根据 API-Key 查询该 Key 授权可见且已启用的 Resource 列表
     */
    @Select("""
            SELECT rd.*
            FROM resource_definition rd
            INNER JOIN api_key_resource akr ON rd.id = akr.resource_id
            INNER JOIN api_key ak ON ak.id = akr.api_key_id
            WHERE ak.api_key = #{apiKey}
              AND ak.status = 1
              AND akr.enabled = 1
              AND rd.enabled = 1
            """)
    List<ResourceDefinition> findResourcesByApiKey(@Param("apiKey") String apiKey);
}
