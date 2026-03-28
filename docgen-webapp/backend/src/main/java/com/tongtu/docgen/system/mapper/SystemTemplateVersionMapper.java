package com.tongtu.docgen.system.mapper;

import com.tongtu.docgen.system.model.entity.SysTemplateVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemTemplateVersionMapper {
    List<SysTemplateVersionEntity> listByTemplateId(@Param("templateId") Long templateId);

    SysTemplateVersionEntity findById(@Param("id") Long id);

    SysTemplateVersionEntity findByTemplateAndVersion(@Param("templateId") Long templateId, @Param("versionNo") Integer versionNo);

    SysTemplateVersionEntity findPublishedByTemplateId(@Param("templateId") Long templateId);

    int insertVersion(SysTemplateVersionEntity entity);

    int updateVersion(SysTemplateVersionEntity entity);

    int clearPublishedByTemplateId(@Param("templateId") Long templateId);
}
