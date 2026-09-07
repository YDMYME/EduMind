package com.example.smartteachingplatform.graph.service;

public interface KnowledgeImportService {
    /**
     * 生成知识图谱导入模板（nodes + edges 两个 Sheet），供教师下载。
     *
     * @param courseId  课程 ID
     * @param teacherId 当前登录教师 ID
     * @return xlsx 文件字节
     */
    byte[] buildTemplate(Long courseId, Long teacherId);
}
