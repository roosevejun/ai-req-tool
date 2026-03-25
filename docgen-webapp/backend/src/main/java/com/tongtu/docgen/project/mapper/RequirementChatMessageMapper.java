package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.RequirementChatMessageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RequirementChatMessageMapper {
    @Insert("""
            INSERT INTO rm_requirement_chat_message(session_id, role, content, seq_no, created_at)
            VALUES(#{sessionId}, #{role}, #{content}, #{seqNo}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RequirementChatMessageEntity entity);

    @Select("""
            SELECT id, session_id AS sessionId, role, content, seq_no AS seqNo, created_at AS createdAt
            FROM rm_requirement_chat_message
            WHERE session_id = #{sessionId}
            ORDER BY seq_no ASC, id ASC
            """)
    List<RequirementChatMessageEntity> listBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT COALESCE(MAX(seq_no), 0)
            FROM rm_requirement_chat_message
            WHERE session_id = #{sessionId}
            """)
    int findMaxSeqNo(@Param("sessionId") Long sessionId);
}
