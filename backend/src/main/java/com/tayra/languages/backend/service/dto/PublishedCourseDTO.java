package com.tayra.languages.backend.service.dto;

import com.tayra.languages.backend.domain.enumeration.CourseLevel;
import java.util.List;

/**
 * A published course as the apps read it, with its lessons in reading order. Courses and lessons are known by their
 * slugs, which stay the same when the content is edited.
 */
public record PublishedCourseDTO(
    String id,
    String languageCode,
    String title,
    String description,
    CourseLevel level,
    String topic,
    List<Lesson> lessons
) {
    public record Lesson(String id, String title, String summary, String text) {}
}
