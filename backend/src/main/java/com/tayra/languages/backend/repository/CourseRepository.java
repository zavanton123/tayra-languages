package com.tayra.languages.backend.repository;

import com.tayra.languages.backend.domain.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Course entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    @Query(
        "select distinct course from Course course left join fetch course.lessons " +
            "where course.published = true and (:languageCode is null or course.languageCode = :languageCode) " +
            "order by course.sortOrder, course.id"
    )
    List<Course> findPublishedWithLessons(@Param("languageCode") String languageCode);

    @Query("select course from Course course left join fetch course.lessons where course.published = true and course.slug = :slug")
    Optional<Course> findPublishedWithLessonsBySlug(@Param("slug") String slug);
}
