package com.example.smartteachingplatform.mastery.mapper;

import com.example.smartteachingplatform.mastery.dto.MasteryItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MasteryMapper {

    @Select("SELECT kn.id AS node_id, kn.node_name AS node_name, " +
            "COALESCE(km.mastery_score, 0) AS mastery_score, " +
            "COALESCE(UPPER(km.mastery_level), 'GRAY') AS mastery_level, " +
            "km.last_learned_at AS last_learned_at " +
            "FROM knowledge_nodes kn " +
            "LEFT JOIN knowledge_mastery km ON km.knowledge_node_id = kn.id " +
            "  AND km.course_id = #{courseId} AND km.student_id = #{studentId} " +
            "WHERE kn.course_id = #{courseId} AND kn.status = 'active' " +
            "ORDER BY kn.sort_order, kn.id")
    List<MasteryItemResponse> findStudentMastery(@Param("courseId") Long courseId,
                                                 @Param("studentId") Long studentId);
}
