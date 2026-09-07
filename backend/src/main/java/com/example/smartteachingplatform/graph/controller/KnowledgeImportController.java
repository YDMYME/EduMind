package com.example.smartteachingplatform.graph.controller;

import com.example.smartteachingplatform.common.util.SecurityUtils;
import com.example.smartteachingplatform.graph.service.KnowledgeImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseId}/knowledge-imports")
@RequiredArgsConstructor
public class KnowledgeImportController {

    private final KnowledgeImportService knowledgeImportService;

    @GetMapping("/template")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long courseId) {
        byte[] bytes = knowledgeImportService.buildTemplate(courseId, SecurityUtils.getUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=knowledge_import_template.xlsx")
                .body(bytes);
    }
}
