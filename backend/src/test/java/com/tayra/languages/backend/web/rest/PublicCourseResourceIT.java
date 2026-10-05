package com.tayra.languages.backend.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tayra.languages.backend.IntegrationTest;
import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.domain.Lesson;
import com.tayra.languages.backend.domain.enumeration.CourseLevel;
import com.tayra.languages.backend.repository.CourseRepository;
import com.tayra.languages.backend.repository.LessonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link PublicCourseResource} REST controller.
 */
@AutoConfigureMockMvc
@WithUnauthenticatedMockUser
@IntegrationTest
class PublicCourseResourceIT {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theApiIsReadWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/api/public/courses?language=de")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/public/courses/no-such-course")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void draftsStayHiddenAndLessonsFollowTheirOrder() throws Exception {
        Course draft = courseRepository.saveAndFlush(course("it-draft", false));
        Course live = courseRepository.saveAndFlush(course("it-live", true));
        lessonRepository.saveAndFlush(lesson("it-live-b", 2, live));
        lessonRepository.saveAndFlush(lesson("it-live-a", 1, live));
        lessonRepository.saveAndFlush(lesson("it-draft-a", 1, draft));
        // The courses were saved without lessons; they are read again from the tables.
        entityManager.clear();

        mockMvc
            .perform(get("/api/public/courses?language=it"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value("it-live"))
            .andExpect(jsonPath("$[0].level").value("A1"))
            .andExpect(jsonPath("$[0].lessons", hasSize(2)))
            .andExpect(jsonPath("$[0].lessons[0].text").value("Ciao."));
        mockMvc
            .perform(get("/api/public/courses/it-live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lessons[0].id").value("it-live-a"))
            .andExpect(jsonPath("$.lessons[1].id").value("it-live-b"));
        mockMvc.perform(get("/api/public/courses/it-draft")).andExpect(status().isNotFound());
    }

    @Test
    void theAdminApiStaysClosed() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    private static Course course(String slug, boolean published) {
        return new Course().slug(slug).languageCode("it").title(slug).level(CourseLevel.A1).sortOrder(0).published(published);
    }

    private static Lesson lesson(String slug, int order, Course course) {
        return new Lesson().slug(slug).title(slug).content("Ciao.").sortOrder(order).course(course);
    }
}
