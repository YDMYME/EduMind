package com.example.smartteachingplatform.mastery.service;

import java.util.Map;

public interface MasteryService {

    Map<String, Object> getMyMastery(Long courseId, Long studentId);
}
