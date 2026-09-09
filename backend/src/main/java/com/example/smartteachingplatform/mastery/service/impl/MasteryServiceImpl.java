package com.example.smartteachingplatform.mastery.service.impl;

import com.example.smartteachingplatform.common.exception.BusinessException;
import com.example.smartteachingplatform.course.entity.CourseMember;
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
}
