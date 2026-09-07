package com.example.smartteachingplatform.course.service;

public interface StudentImportService {

    /**
     * 生成学生名单导入模板（仅表头），供教师下载。
     *
     * @param courseId  课程 ID
     * @param teacherId 当前登录教师 ID
     * @return xlsx 文件字节
     */
    byte[] buildTemplate(Long courseId, Long teacherId);
}
