package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.ProjectChatMessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectChatMessageMapper {
    @Insert("""
            INSERT INTO pm_project_chat_message(session_id, seq_no, role, message_type, content, created_at)
            VALUES(#{sessionId}, #{seqNo}, #{role}, #{messageType}, #{content}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectChatMessageEntity entity);

    @Select("""
            SELECT id, session_id AS sessionId, seq_no AS seqNo,
                   role, message_type AS messageType, content, created_at AS createdAt
            FROM pm_project_chat_message
            WHERE session_id = #{sessionId}
            ORDER BY seq_no ASC
            """)
    List<ProjectChatMessageEntity> listBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT COALESCE(MAX(seq_no), 0)
            FROM pm_project_chat_message
            WHERE session_id = #{sessionId}
            """)
    int findMaxSeqNo(@Param("sessionId") Long sessionId);
}
