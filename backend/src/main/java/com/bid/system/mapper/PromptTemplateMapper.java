package com.bid.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bid.system.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    @Select("SELECT * FROM prompt_template WHERE is_active = 1 LIMIT 1")
    PromptTemplate findActive();

    @Update("UPDATE prompt_template SET content = #{content}, field_def = #{fieldDef}, updated_at = NOW() WHERE id = #{id}")
    void updateContent(java.lang.Long id, java.lang.String content, java.lang.String fieldDef);
}
