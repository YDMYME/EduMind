package com.example.smartteachingplatform.graph.service.impl;

import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.entity.CourseMember;
import com.example.smartteachingplatform.course.mapper.CourseMemberMapper;
import com.example.smartteachingplatform.graph.dto.NodeQueryResponse;
import com.example.smartteachingplatform.graph.entity.KnowledgeEdge;
import com.example.smartteachingplatform.graph.entity.KnowledgeNode;
import com.example.smartteachingplatform.graph.mapper.KnowledgeEdgeMapper;
import com.example.smartteachingplatform.graph.mapper.KnowledgeNodeMapper;
import com.example.smartteachingplatform.graph.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.smartteachingplatform.graph.dto.NodeRequest;
import com.example.smartteachingplatform.course.mapper.CourseMapper;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final KnowledgeEdgeMapper knowledgeEdgeMapper;
    private final CourseMemberMapper courseMemberMapper;
    private final CourseMapper courseMapper;

    @Override
    public Map<String, Object> getGraph(Long courseId, Long userId, String userRole, String viewRole) {
        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该课程的成员");
        }

        // 防伪造：TEACHER 可传 ?role=student 降级为学生视图；STUDENT 传 ?role=teacher 无效
        boolean isTeacherView;
        if ("TEACHER".equals(userRole)) {
            isTeacherView = !"student".equalsIgnoreCase(viewRole);
        } else {
            if ("teacher".equalsIgnoreCase(viewRole)) {
                throw new BusinessException(403, "无权查看教师视图");
            }
            isTeacherView = false;
        }

        List<KnowledgeNode> nodes;
        if (isTeacherView) {
            nodes = knowledgeNodeMapper.findNodesWithAvgMastery(courseId);
        } else {
            nodes = knowledgeNodeMapper.findNodesWithStudentMastery(courseId, userId);
        }
        List<KnowledgeEdge> edges = knowledgeEdgeMapper.findByCourseId(courseId);

        return buildGraphResponse(courseId, nodes, edges, isTeacherView);
    }

    @Override
    public Map<String, Object> getStudentGraph(Long courseId, Long studentId, Long userId, String userRole) {
        if ("STUDENT".equals(userRole) && !userId.equals(studentId)) {
            throw new BusinessException(403, "只能查看自己的学习情况");
        }

        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该课程的成员");
        }

        List<KnowledgeNode> nodes = knowledgeNodeMapper.findNodesWithStudentMastery(courseId, studentId);
        List<KnowledgeEdge> edges = knowledgeEdgeMapper.findByCourseId(courseId);

        return buildGraphResponse(courseId, nodes, edges, false);
    }

    @Override
    public Map<String, Object> createNode(Long courseId, Long teacherId, NodeRequest request) {
        assertCourseTeacher(courseId, teacherId);

        if (request.getNodeCode() == null || request.getNodeCode().isBlank()) {
            throw new BusinessException(400, "nodeCode 不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(400, "name 不能为空");
        }
        if (knowledgeNodeMapper.findByCode(courseId, request.getNodeCode()) != null) {
            throw new BusinessException(409, "节点编码已存在");
        }
        Long parentId = resolveParentId(courseId, request.getParentCode());

        KnowledgeNode node = new KnowledgeNode();
        node.setCourseId(courseId);
        node.setNodeCode(request.getNodeCode());
        node.setNodeName(request.getName());
        node.setNodeDesc(request.getDescription());
        node.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : 1);
        node.setParentId(parentId);
        node.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        node.setStatus("active");
        knowledgeNodeMapper.insert(node);

        return Map.of("nodeId", node.getId());
    }

    @Override
    public Map<String, Object> updateNode(Long nodeId, Long teacherId, NodeRequest request) {
        KnowledgeNode node = knowledgeNodeMapper.findById(nodeId);
        if (node == null) {
            throw new BusinessException(404, "节点不存在");
        }
        assertCourseTeacher(node.getCourseId(), teacherId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(400, "name 不能为空");
        }
        if (request.getNodeCode() != null && !request.getNodeCode().isBlank()) {
            KnowledgeNode exist = knowledgeNodeMapper.findByCode(node.getCourseId(), request.getNodeCode());
            if (exist != null && !exist.getId().equals(nodeId)) {
                throw new BusinessException(409, "节点编码已存在");
            }
        }
        Long parentId = resolveParentId(node.getCourseId(), request.getParentCode());

        node.setNodeCode(request.getNodeCode() != null ? request.getNodeCode() : node.getNodeCode());
        node.setNodeName(request.getName());
        node.setNodeDesc(request.getDescription());
        if (request.getDifficulty() != null) node.setDifficulty(request.getDifficulty());
        node.setParentId(parentId);
        if (request.getSortOrder() != null) node.setSortOrder(request.getSortOrder());
        knowledgeNodeMapper.updateNode(node);

        return Map.of("nodeId", nodeId);
    }

    private Long resolveParentId(Long courseId, String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        KnowledgeNode parent = knowledgeNodeMapper.findByCode(courseId, parentCode);
        if (parent == null) {
            throw new BusinessException(400, "父节点不存在");
        }
        return parent.getId();
    }

    private void assertCourseTeacher(Long courseId, Long teacherId) {
        Long ownerId = courseMapper.findTeacherIdByCourseId(courseId);
        if (ownerId == null) throw new BusinessException(404, "课程不存在");
        if (!ownerId.equals(teacherId)) throw new BusinessException(403, "仅本课程教师可操作");
    }

    private Map<String, Object> buildGraphResponse(Long courseId, List<KnowledgeNode> nodes,
                                                    List<KnowledgeEdge> edges, boolean isTeacherView) {
        Long recommendedNodeId = null;
        List<Long> weakNodeIds = new ArrayList<>();

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (KnowledgeNode node : nodes) {
            double score = node.getMasteryScore() != null ? node.getMasteryScore() : 0.0;
            int roundedScore = (int) Math.round(score);
            String level = computeLevel(roundedScore);

            Map<String, Object> nodeMap = new LinkedHashMap<>();
            nodeMap.put("id", node.getId());
            nodeMap.put("name", node.getNodeName());
            nodeMap.put("description", node.getNodeDesc());
            nodeMap.put("order", node.getSortOrder());
            nodeMap.put("x", node.getXPosition());
            nodeMap.put("y", node.getYPosition());
            nodeMap.put("masteryScore", roundedScore);
            nodeMap.put("masteryLevel", level);
            nodeMap.put("isRecommended", false);
            nodeMap.put("isWeakTop", false);

            nodeList.add(nodeMap);
        }

        if (isTeacherView) {
            List<KnowledgeNode> sorted = nodes.stream()
                    .sorted(Comparator.comparingDouble(
                            n -> n.getMasteryScore() != null ? n.getMasteryScore() : 0.0))
                    .collect(Collectors.toList());

            Set<Long> weakSet = new HashSet<>();
            int count = 0;
            for (KnowledgeNode n : sorted) {
                if (count >= 5) break;
                weakSet.add(n.getId());
                count++;
            }
            weakNodeIds.addAll(weakSet);

            for (Map<String, Object> nm : nodeList) {
                nm.put("isWeakTop", weakSet.contains((Long) nm.get("id")));
            }
        } else {
            for (Map<String, Object> nm : nodeList) {
                int score = (int) nm.get("masteryScore");
                if (score < 80 && recommendedNodeId == null) {
                    recommendedNodeId = (Long) nm.get("id");
                    nm.put("isRecommended", true);
                }
            }
        }

        List<Map<String, Object>> edgeList = edges.stream()
                .map(e -> {
                    Map<String, Object> em = new LinkedHashMap<>();
                    em.put("from", e.getSourceNodeId());
                    em.put("to", e.getTargetNodeId());
                    em.put("type", e.getRelationType().toUpperCase());
                    return em;
                })
                .collect(Collectors.toList());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("viewType", isTeacherView ? "TEACHER" : "STUDENT");
        meta.put("recommendedNodeId", recommendedNodeId);
        meta.put("weakNodeIds", weakNodeIds);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", courseId);
        data.put("nodes", nodeList);
        data.put("edges", edgeList);
        data.put("meta", meta);

        return data;
    }

    @Override
    public Map<String, Object> getNodeStudents(Long courseId, Long nodeId) {
        KnowledgeNode node = knowledgeNodeMapper.findById(nodeId);
        List<Map<String, Object>> rows = knowledgeNodeMapper.findStudentsByNodeId(courseId, nodeId);

        double totalScore = 0;
        List<Map<String, Object>> students = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Number scoreNum = (Number) row.get("mastery_score");
            double score = scoreNum != null ? scoreNum.doubleValue() : 0.0;
            totalScore += score;

            Map<String, Object> s = new LinkedHashMap<>();
            s.put("studentId", row.get("student_id"));
            s.put("studentName", row.get("student_name"));
            s.put("masteryScore", Math.round(score));
            s.put("masteryLevel", computeLevel((int) Math.round(score)));
            students.add(s);
        }

        int count = students.size();
        int avgScore = count > 0 ? (int) Math.round(totalScore / count) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", nodeId);
        result.put("nodeName", node != null ? node.getNodeName() : "");
        result.put("classAvgScore", avgScore);
        result.put("classAvgLevel", computeLevel(avgScore));
        result.put("totalStudents", count);
        result.put("students", students);
        return result;
    }

    @Override
    public Map<String, Object> queryNodes(Long courseId, Long userId, int page, int pageSize) {
        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该课程的成员");
        }
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 100;

        int total = knowledgeNodeMapper.countByCourseId(courseId);
        int offset = (page - 1) * pageSize;
        List<NodeQueryResponse> items = knowledgeNodeMapper.findNodesByCourseId(courseId, offset, pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    private String computeLevel(int score) {
        if (score == 0) return "GRAY";
        if (score < 60) return "RED";
        if (score < 80) return "YELLOW";
        return "GREEN";
    }
}
