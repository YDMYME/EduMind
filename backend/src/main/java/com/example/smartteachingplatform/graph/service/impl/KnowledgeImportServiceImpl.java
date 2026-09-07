package com.example.smartteachingplatform.graph.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.mapper.CourseMapper;
import com.example.smartteachingplatform.graph.service.KnowledgeImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeImportServiceImpl implements KnowledgeImportService {
    private final CourseMapper courseMapper;

    @Override
    public byte[] buildTemplate(Long courseId, Long teacherId) {
        // 鉴权：复用 CourseMapper.findTeacherIdByCourseId（404/403）
        Long ownerId = courseMapper.findTeacherIdByCourseId(courseId);
        if (ownerId == null) throw new BusinessException(404, "课程不存在");
        if (!ownerId.equals(teacherId)) throw new BusinessException(403, "仅本课程教师可操作");

        // 两个 Sheet 的表头（仅表头，无数据行）
        List<List<String>> nodesHead = Arrays.asList(
                Collections.singletonList("nodeCode"),
                Collections.singletonList("name"),
                Collections.singletonList("description"),
                Collections.singletonList("parentCode"),
                Collections.singletonList("orderNo"));
        List<List<String>> edgesHead = Arrays.asList(
                Collections.singletonList("fromCode"),
                Collections.singletonList("toCode"),
                Collections.singletonList("relationType"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out).build();
        writer.write(Collections.emptyList(),
                EasyExcel.writerSheet("nodes").head(nodesHead).build());
        writer.write(Collections.emptyList(),
                EasyExcel.writerSheet("edges").head(edgesHead).build());
        writer.finish();
        return out.toByteArray();
    }
}
