package com.tayra.languages.backend.service;

import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.domain.Lesson;
import com.tayra.languages.backend.repository.CourseRepository;
import com.tayra.languages.backend.service.dto.PublishedCourseDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the published courses for the apps.
 */
@Service
@Transactional(readOnly = true)
public class PublishedCourseService {

    private static final Comparator<Lesson> READING_ORDER = Comparator.comparing(Lesson::getSortOrder).thenComparing(Lesson::getId);

    private final CourseRepository courseRepository;

    public PublishedCourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * @param languageCode the language to keep, or null for every language.
     */
    public List<PublishedCourseDTO> findAll(String languageCode) {
        return courseRepository.findPublishedWithLessons(languageCode).stream().map(PublishedCourseService::toDto).toList();
    }

    public Optional<PublishedCourseDTO> findOne(String slug) {
        return courseRepository.findPublishedWithLessonsBySlug(slug).map(PublishedCourseService::toDto);
    }

    private static PublishedCourseDTO toDto(Course course) {
        List<PublishedCourseDTO.Lesson> lessons = course
            .getLessons()
            .stream()
            .sorted(READING_ORDER)
            .map(lesson -> new PublishedCourseDTO.Lesson(lesson.getSlug(), lesson.getTitle(), lesson.getSummary(), lesson.getContent()))
            .toList();
        return new PublishedCourseDTO(
            course.getSlug(),
            course.getLanguageCode(),
            course.getTitle(),
            course.getDescription(),
            course.getLevel(),
            course.getTopic(),
            lessons
        );
    }
}
