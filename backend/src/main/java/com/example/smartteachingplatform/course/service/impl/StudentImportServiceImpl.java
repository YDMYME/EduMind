package com.example.smartteachingplatform.course.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.mapper.CourseMapper;
import com.example.smartteachingplatform.course.service.StudentImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentImportServiceImpl implements StudentImportService {
    private static final List<String> HEADERS =
            Arrays.asList("studentNo", "realName", "email", "phone", "className");

    private static final String SHEET_NAME = "学生名单";

    private final CourseMapper courseMapper;

    @Override
    public byte[] buildTemplate(Long courseId, Long teacherId) {
        Long ownerId = courseMapper.findTeacherIdByCourseId(courseId);
        if (ownerId == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!ownerId.equals(teacherId)){
            throw new BusinessException(403, "仅本课程教师可操作");
        }

        List<List<String>> head = HEADERS.stream()
                .map(Collections::singletonList)
                .collect(Collectors.toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out)
                .head(head)
                .sheet(SHEET_NAME)
                .doWrite(Collections.emptyList());

        return out.toByteArray();
    }
}
