package com.example.smartteachingplatform.mastery.service.impl;

import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.entity.CourseMember;
import com.example.smartteachingplatform.course.mapper.CourseMapper;
import com.example.smartteachingplatform.course.mapper.CourseMemberMapper;
import com.example.smartteachingplatform.mastery.dto.MasteryItemResponse;
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
}
