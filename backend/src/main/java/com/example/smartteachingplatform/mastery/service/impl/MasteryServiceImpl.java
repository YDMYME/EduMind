package com.example.smartteachingplatform.mastery.service.impl;

import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.entity.CourseMember;
import com.example.smartteachingplatform.course.mapper.CourseMapper;
import com.example.smartteachingplatform.course.mapper.CourseMemberMapper;
import com.example.smartteachingplatform.graph.entity.KnowledgeNode;
import com.example.smartteachingplatform.graph.mapper.KnowledgeNodeMapper;
import com.example.smartteachingplatform.mastery.dto.MasteryHistoryItemResponse;
import com.example.smartteachingplatform.mastery.dto.MasteryItemResponse;
import com.example.smartteachingplatform.mastery.dto.NodeSummaryResponse;
import com.example.smartteachingplatform.mastery.mapper.MasteryMapper;
import com.example.smartteachingplatform.mastery.service.MasteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MasteryServiceImpl implements MasteryService {

    private final MasteryMapper masteryMapper;
    private final CourseMemberMapper courseMemberMapper;
    private final CourseMapper courseMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    @Override
    public Map<String, Object> getMyMastery(Long courseId, Long studentId) {
        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, studentId);
        if (member == null) {
            throw new BusinessException(403, "未加入该课程");
        }

        List<MasteryItemResponse> items = masteryMapper.findStudentMastery(courseId, studentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        return data;
    }

    @Override
    public Map<String, Object> getStudentMastery(Long courseId, Long studentId, Long teacherId) {
        Long ownerId = courseMapper.findTeacherIdByCourseId(courseId);
        if (ownerId == null || !ownerId.equals(teacherId)) {
            throw new BusinessException(403, "无权限操作该课程");
        }
        CourseMember student = courseMemberMapper.findByCourseIdAndUserId(courseId, studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不在该课程中");
        }

        List<MasteryItemResponse> items = masteryMapper.findStudentMastery(courseId, studentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        return data;
    }

    @Override
    public NodeSummaryResponse getNodeSummary(Long courseId, Long nodeId, Long teacherId) {
        Long ownerId = courseMapper.findTeacherIdByCourseId(courseId);
        if (ownerId == null || !ownerId.equals(teacherId)) {
            throw new BusinessException(403, "无权限操作该课程");
        }
        KnowledgeNode node = knowledgeNodeMapper.findById(nodeId);
        if (node == null || !courseId.equals(node.getCourseId())) {
            throw new BusinessException(404, "知识点不存在");
        }

        NodeSummaryResponse resp = new NodeSummaryResponse();
        resp.setNodeId(node.getId());
        resp.setNodeName(node.getNodeName());
        resp.setStudentCount(masteryMapper.countStudentsByCourse(courseId));
        resp.setAverageScore(masteryMapper.avgMasteryScore(courseId, nodeId));
        resp.setAtRiskStudents(masteryMapper.atRiskStudents(courseId, nodeId));

        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("GRAY", 0);
        dist.put("RED", 0);
        dist.put("YELLOW", 0);
        dist.put("GREEN", 0);
        for (Map<String, Object> row : masteryMapper.distribution(courseId, nodeId)) {
            String level = ((String) row.get("level")).toUpperCase();
            int cnt = ((Number) row.get("cnt")).intValue();
            dist.put(level, cnt);
        }
        resp.setDistribution(dist);
        return resp;
    }

    @Override
    public Map<String, Object> getHistory(Long courseId, Long studentId, Long nodeId,
                                          int page, int pageSize, Long userId) {
        Long teacherId = courseMapper.findTeacherIdByCourseId(courseId);
        boolean isTeacher = userId.equals(teacherId);
        if (!isTeacher) {
            if (!userId.equals(studentId)) {
                throw new BusinessException(403, "只能查询本人的掌握度历史");
            }
            CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
            if (member == null) {
                throw new BusinessException(403, "未加入该课程");
            }
        } else {
            CourseMember student = courseMemberMapper.findByCourseIdAndUserId(courseId, studentId);
            if (student == null) {
                throw new BusinessException(404, "学生不在该课程中");
            }
        }

        int size = Math.max(1, pageSize);
        int offset = (Math.max(1, page) - 1) * size;
        List<MasteryHistoryItemResponse> items =
                masteryMapper.findHistoryPage(courseId, studentId, nodeId, size, offset);
        long total = masteryMapper.countHistory(courseId, studentId, nodeId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }
}
