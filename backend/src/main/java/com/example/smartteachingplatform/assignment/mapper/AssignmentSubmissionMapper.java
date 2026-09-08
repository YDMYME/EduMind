package com.example.smartteachingplatform.assignment.mapper;

import com.example.smartteachingplatform.assignment.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AssignmentSubmissionMapper {

    @Select("SELECT * FROM assignment_submissions WHERE assignment_id = #{assignmentId} AND student_id =" +
            "#{studentId}")
    AssignmentSubmission findByAssignmentAndStudent(@Param("assignmentId") Long assignmentId,
                                                    @Param("studentId") Long studentId);
}
