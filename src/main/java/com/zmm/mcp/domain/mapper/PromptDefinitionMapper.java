package com.zmm.mcp.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmm.mcp.domain.entity.PromptDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptDefinitionMapper extends BaseMapper<PromptDefinition> {

    /**
     * 根据 API-Key 查询该 Key 授权可见且已启用的 Prompt 列表
     */
    @Select("""
            SELECT pd.*
            FROM prompt_definition pd
            INNER JOIN api_key_prompt akp ON pd.id = akp.prompt_id
            INNER JOIN api_key ak ON ak.id = akp.api_key_id
            WHERE ak.api_key = #{apiKey}
              AND ak.status = 1
              AND akp.enabled = 1
              AND pd.enabled = 1
            """)
    List<PromptDefinition> findPromptsByApiKey(@Param("apiKey") String apiKey);
}
