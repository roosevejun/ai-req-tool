package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.project.model.entity.ProjectMemberEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {
    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description,
                   project_background AS projectBackground, similar_products AS similarProducts,
                   target_customer_groups AS targetCustomerGroups, commercial_value AS commercialValue,
                   core_product_value AS coreProductValue,
                   project_type AS projectType, priority, start_date AS startDate, target_date AS targetDate, tags,
                   visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            ORDER BY id DESC
            """)
    List<ProjectEntity> listProjects();

    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description,
                   project_background AS projectBackground, similar_products AS similarProducts,
                   target_customer_groups AS targetCustomerGroups, commercial_value AS commercialValue,
                   core_product_value AS coreProductValue,
                   project_type AS projectType, priority, start_date AS startDate, target_date AS targetDate, tags,
                   visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            WHERE id = #{id}
            LIMIT 1
            """)
    ProjectEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, project_key AS projectKey, project_name AS projectName, description,
                   project_background AS projectBackground, similar_products AS similarProducts,
                   target_customer_groups AS targetCustomerGroups, commercial_value AS commercialValue,
                   core_product_value AS coreProductValue,
                   project_type AS projectType, priority, start_date AS startDate, target_date AS targetDate, tags,
                   visibility, status,
                   owner_user_id AS ownerUserId, created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project
            WHERE project_key = #{projectKey}
            LIMIT 1
            """)
    ProjectEntity findByKey(@Param("projectKey") String projectKey);

    @Insert("""
            INSERT INTO pm_project(project_key, project_name, description, project_background, similar_products,
                                   target_customer_groups, commercial_value, core_product_value,
                                   project_type, priority, start_date, target_date, tags, visibility, status,
                                   owner_user_id, created_by, updated_by, created_at, updated_at)
            VALUES(#{projectKey}, #{projectName}, #{description}, #{projectBackground}, #{similarProducts},
                   #{targetCustomerGroups}, #{commercialValue}, #{coreProductValue},
                   #{projectType}, #{priority}, #{startDate}, #{targetDate}, #{tags}, #{visibility}, #{status},
                   #{ownerUserId}, #{createdBy}, #{updatedBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectEntity project);

    @Update("""
            UPDATE pm_project
            SET project_name = #{projectName},
                description = #{description},
                project_background = #{projectBackground},
                similar_products = #{similarProducts},
                target_customer_groups = #{targetCustomerGroups},
                commercial_value = #{commercialValue},
                core_product_value = #{coreProductValue},
                project_type = #{projectType},
                priority = #{priority},
                start_date = #{startDate},
                target_date = #{targetDate},
                tags = #{tags},
                visibility = #{visibility},
                status = #{status},
                owner_user_id = #{ownerUserId},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(ProjectEntity project);

    @Select("""
            SELECT pm.id, pm.project_id AS projectId, pm.user_id AS userId,
                   su.username, su.display_name AS displayName,
                   pm.project_role AS projectRole, pm.status,
                   pm.created_by AS createdBy, pm.updated_by AS updatedBy,
                   pm.created_at AS createdAt, pm.updated_at AS updatedAt
            FROM pm_project_member pm
            LEFT JOIN sys_user su ON su.id = pm.user_id
            WHERE pm.project_id = #{projectId}
            ORDER BY pm.id ASC
            """)
    List<ProjectMemberEntity> listMembersByProjectId(@Param("projectId") Long projectId);

    @Select("""
            SELECT pm.id, pm.project_id AS projectId, pm.user_id AS userId,
                   su.username, su.display_name AS displayName,
                   pm.project_role AS projectRole, pm.status,
                   pm.created_by AS createdBy, pm.updated_by AS updatedBy,
                   pm.created_at AS createdAt, pm.updated_at AS updatedAt
            FROM pm_project_member pm
            LEFT JOIN sys_user su ON su.id = pm.user_id
            WHERE pm.project_id = #{projectId} AND pm.user_id = #{userId}
            LIMIT 1
            """)
    ProjectMemberEntity findMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Insert("""
            INSERT INTO pm_project_member(project_id, user_id, project_role, status, created_by, updated_by, created_at, updated_at)
            VALUES(#{projectId}, #{userId}, #{projectRole}, #{status}, #{createdBy}, #{updatedBy}, NOW(), NOW())
            """)
    int insertMember(ProjectMemberEntity member);

    @Delete("""
            DELETE FROM pm_project_member
            WHERE project_id = #{projectId} AND user_id = #{userId}
            """)
    int deleteMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Update("""
            UPDATE pm_project
            SET tags = #{tags},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{projectId}
            """)
    int updateTags(@Param("projectId") Long projectId, @Param("tags") String tags, @Param("updatedBy") Long updatedBy);
}


