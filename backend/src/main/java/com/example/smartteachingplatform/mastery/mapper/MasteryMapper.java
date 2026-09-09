package com.example.smartteachingplatform.mastery.mapper;

import com.example.smartteachingplatform.mastery.dto.MasteryItemResponse;
import com.example.smartteachingplatform.mastery.dto.NodeSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    @Select("SELECT COUNT(*) FROM course_members WHERE course_id = #{courseId} AND member_role = 'student' AND status = 'active'")
    int countStudentsByCourse(Long courseId);

    @Select("SELECT COALESCE(AVG(COALESCE(km.mastery_score, 0)), 0) " +
            "FROM course_members cm " +
            "LEFT JOIN knowledge_mastery km ON km.student_id = cm.user_id " +
            "  AND km.course_id = cm.course_id AND km.knowledge_node_id = #{nodeId} " +
            "WHERE cm.course_id = #{courseId} AND cm.member_role = 'student' AND cm.status = 'active'")
    BigDecimal avgMasteryScore(@Param("courseId") Long courseId, @Param("nodeId") Long nodeId);

    @Select("SELECT COALESCE(km.mastery_level, 'gray') AS level, COUNT(*) AS cnt " +
            "FROM course_members cm " +
            "LEFT JOIN knowledge_mastery km ON km.student_id = cm.user_id " +
            "  AND km.course_id = cm.course_id AND km.knowledge_node_id = #{nodeId} " +
            "WHERE cm.course_id = #{courseId} AND cm.member_role = 'student' AND cm.status = 'active' " +
            "GROUP BY COALESCE(km.mastery_level, 'gray')")
    List<Map<String, Object>> distribution(@Param("courseId") Long courseId, @Param("nodeId") Long nodeId);

    @Select("SELECT cm.user_id AS student_id, u.user_no AS student_no, u.real_name AS real_name, " +
            "COALESCE(km.mastery_score, 0) AS score " +
            "FROM course_members cm " +
            "JOIN users u ON u.id = cm.user_id " +
            "LEFT JOIN knowledge_mastery km ON km.student_id = cm.user_id " +
            "  AND km.course_id = cm.course_id AND km.knowledge_node_id = #{nodeId} " +
            "WHERE cm.course_id = #{courseId} AND cm.member_role = 'student' AND cm.status = 'active' " +
            "  AND COALESCE(km.mastery_score, 0) < 60 " +
            "ORDER BY COALESCE(km.mastery_score, 0) ASC")
    List<NodeSummaryResponse.AtRiskStudent> atRiskStudents(@Param("courseId") Long courseId,
                                                           @Param("nodeId") Long nodeId);
}
