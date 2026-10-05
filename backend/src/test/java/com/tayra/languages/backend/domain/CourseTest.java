package com.tayra.languages.backend.domain;

import static com.tayra.languages.backend.domain.CourseTestSamples.*;
import static com.tayra.languages.backend.domain.LessonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.tayra.languages.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourseTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Course.class);
        Course course1 = getCourseSample1();
        Course course2 = new Course();
        assertThat(course1).isNotEqualTo(course2);

        course2.setId(course1.getId());
        assertThat(course1).isEqualTo(course2);

        course2 = getCourseSample2();
        assertThat(course1).isNotEqualTo(course2);
    }

    @Test
    void lessonTest() {
        Course course = getCourseRandomSampleGenerator();
        Lesson lessonBack = getLessonRandomSampleGenerator();

        course.addLesson(lessonBack);
        assertThat(course.getLessons()).containsOnly(lessonBack);
        assertThat(lessonBack.getCourse()).isEqualTo(course);

        course.removeLesson(lessonBack);
        assertThat(course.getLessons()).doesNotContain(lessonBack);
        assertThat(lessonBack.getCourse()).isNull();

        course.lessons(new HashSet<>(Set.of(lessonBack)));
        assertThat(course.getLessons()).containsOnly(lessonBack);
        assertThat(lessonBack.getCourse()).isEqualTo(course);

        course.setLessons(new HashSet<>());
        assertThat(course.getLessons()).doesNotContain(lessonBack);
        assertThat(lessonBack.getCourse()).isNull();
    }
}
