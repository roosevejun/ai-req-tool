package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.ProjectEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {
    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description, visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            ORDER BY id DESC
            """)
    List<ProjectEntity> listProjects();

    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description, visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            WHERE id = #{id}
            LIMIT 1
            """)
    ProjectEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description, visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            WHERE project_key = #{projectKey}
            LIMIT 1
            """)
    ProjectEntity findByKey(@Param("projectKey") String projectKey);

    @Insert("""
            INSERT INTO pm_project(project_key, project_name, description, visibility, status, owner_user_id, created_by, updated_by, created_at, updated_at)
            VALUES(#{projectKey}, #{projectName}, #{description}, #{visibility}, #{status}, #{ownerUserId}, #{createdBy}, #{updatedBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectEntity project);

    @Update("""
            UPDATE pm_project
            SET project_name = #{projectName},
                description = #{description},
                visibility = #{visibility},
                status = #{status},
                owner_user_id = #{ownerUserId},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(ProjectEntity project);
}


