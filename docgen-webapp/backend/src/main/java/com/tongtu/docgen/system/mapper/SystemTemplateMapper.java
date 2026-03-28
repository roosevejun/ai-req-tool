package com.tongtu.docgen.system.mapper;

import com.tongtu.docgen.system.model.entity.SysTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemTemplateMapper {
    List<SysTemplateEntity> listTemplates();

    SysTemplateEntity findById(@Param("id") Long id);

    SysTemplateEntity findByCode(@Param("templateCode") String templateCode);

    int insertTemplate(SysTemplateEntity entity);

    int updateTemplate(SysTemplateEntity entity);

    int updateVersionPointers(@Param("id") Long id,
                              @Param("latestVersionNo") Integer latestVersionNo,
                              @Param("publishedVersionNo") Integer publishedVersionNo,
                              @Param("updatedBy") Long updatedBy);
}
