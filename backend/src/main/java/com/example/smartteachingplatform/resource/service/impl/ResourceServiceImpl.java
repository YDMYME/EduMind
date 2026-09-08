package com.example.smartteachingplatform.resource.service.impl;

import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.graph.entity.KnowledgeNode;
import com.example.smartteachingplatform.graph.mapper.KnowledgeNodeMapper;
import com.example.smartteachingplatform.resource.entity.Resource;
import com.example.smartteachingplatform.resource.mapper.ResourceMapper;
import com.example.smartteachingplatform.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.smartteachingplatform.course.entity.CourseMember;
import com.example.smartteachingplatform.course.mapper.CourseMemberMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final CourseMemberMapper courseMemberMapper;

    @Override
    @Transactional
    public Map<String, Object> uploadResource(Long courseId, Long teacherId, Map<String, Object> body) {
        Resource resource = new Resource();
        resource.setCourseId(courseId);
        resource.setUploaderId(teacherId);
        resource.setResourceName((String) body.get("name"));
        resource.setResourceType(((String) body.get("type")).toLowerCase());
        resource.setFileUrl((String) body.get("url"));

        Object fileSize = body.get("fileSize");
        if (fileSize instanceof Number) {
            resource.setFileSize(((Number) fileSize).longValue());
        }
        Object duration = body.get("duration");
        if (duration instanceof Number) {
            resource.setDuration(((Number) duration).intValue());
        }
        resource.setDescription((String) body.get("description"));

        resourceMapper.insert(resource);

        Object nodeId = body.get("nodeId");
        if (nodeId != null) {
            long kid = nodeId instanceof Number ? ((Number) nodeId).longValue()
                                                 : Long.parseLong(nodeId.toString());
            resourceMapper.bindKnowledgeNode(resource.getId(), kid);
        }

        return Map.of("resourceId", resource.getId());
    }

    @Override
    public Map<String, Object> getLearning(Long courseId, Long nodeId) {
        KnowledgeNode node = knowledgeNodeMapper.findById(nodeId);
        if (node == null || !node.getCourseId().equals(courseId)) {
            throw new BusinessException(404, "知识点不存在");
        }

        List<Resource> resources = resourceMapper.findByKnowledgeNodeId(courseId, nodeId);
        List<Map<String, Object>> quizRows = resourceMapper.findQuizzesByKnowledgeNodeId(courseId, nodeId);

        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put("id", node.getId());
        nodeMap.put("name", node.getNodeName());
        nodeMap.put("description", node.getNodeDesc());

        List<Map<String, Object>> resourceList = resources.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getResourceName());
                    m.put("type", r.getResourceType().toUpperCase());
                    m.put("url", r.getFileUrl());
                    return m;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> quizList = quizRows.stream()
                .map(q -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("quizId", q.get("quiz_id"));
                    m.put("name", q.get("quiz_name"));
                    m.put("deadline", q.get("deadline"));
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("node", nodeMap);
        data.put("resources", resourceList);
        data.put("quizzes", quizList);

        return data;
    }

    @Override
    public Map<String, Object> listResources(Long courseId, Long userId, int page, int pageSize) {
        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该课程的成员");
        }
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        int total = resourceMapper.countByCourseId(courseId);
        int offset = (page - 1) * pageSize;
        List<Resource> resources = resourceMapper.findResourcesByCourseId(courseId, offset, pageSize);

        // 批量查每个资源关联的节点 id
        List<Long> ids = resources.stream().map(Resource::getId).collect(Collectors.toList());
        Map<Long, List<Long>> nodeIdsMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> row : resourceMapper.findNodeIdsByResourceIds(ids)) {
                Long rid = ((Number) row.get("resource_id")).longValue();
                Long nid = ((Number) row.get("knowledge_node_id")).longValue();
                nodeIdsMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(nid);
            }
        }

        List<Map<String, Object>> items = resources.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getResourceName());
            m.put("resourceType", r.getResourceType());
            m.put("type", r.getResourceType().toUpperCase());
            m.put("url", r.getFileUrl());
            m.put("description", r.getDescription());
            m.put("nodeIds", nodeIdsMap.getOrDefault(r.getId(), Collections.emptyList()));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> getNodeResources(Long nodeId, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.findById(nodeId);
        if (node == null) {
            throw new BusinessException(404, "知识点不存在");
        }
        Long courseId = node.getCourseId();

        CourseMember member = courseMemberMapper.findByCourseIdAndUserId(courseId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该课程的成员");
        }

        List<Resource> resources = resourceMapper.findByKnowledgeNodeId(courseId, nodeId);

        // 批量查每个资源关联的节点 id
        List<Long> ids = resources.stream().map(Resource::getId).collect(Collectors.toList());
        Map<Long, List<Long>> nodeIdsMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> row : resourceMapper.findNodeIdsByResourceIds(ids)) {
                Long rid = ((Number) row.get("resource_id")).longValue();
                Long nid = ((Number) row.get("knowledge_node_id")).longValue();
                nodeIdsMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(nid);
            }
        }

        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put("id", node.getId());
        nodeMap.put("name", node.getNodeName());
        nodeMap.put("description", node.getNodeDesc());

        List<Map<String, Object>> resourceList = resources.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getResourceName());
                    m.put("type", r.getResourceType().toUpperCase());
                    m.put("resourceType", r.getResourceType());
                    m.put("url", r.getFileUrl());
                    m.put("description", r.getDescription());
                    m.put("nodeIds", nodeIdsMap.getOrDefault(r.getId(), Collections.emptyList()));
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("node", nodeMap);
        data.put("resources", resourceList);
        return data;
    }
}
