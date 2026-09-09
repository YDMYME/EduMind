package com.example.smartteachingplatform.mastery.controller;

import com.example.smartteachingplatform.common.response.Result;
import com.example.smartteachingplatform.common.util.SecurityUtils;
import com.example.smartteachingplatform.mastery.service.MasteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MasteryController {

    private final MasteryService masteryService;

    @GetMapping("/api/courses/{courseId}/mastery/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getMyMastery(@PathVariable Long courseId) {
        return Result.success(masteryService.getMyMastery(courseId, SecurityUtils.getUserId()));
    }
}
