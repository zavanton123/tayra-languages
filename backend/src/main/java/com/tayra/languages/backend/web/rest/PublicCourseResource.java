package com.tayra.languages.backend.web.rest;

import com.tayra.languages.backend.service.PublishedCourseService;
import com.tayra.languages.backend.service.dto.PublishedCourseDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

/**
 * The published courses, open to the apps without signing in.
 */
@RestController
@RequestMapping("/api/public/courses")
public class PublicCourseResource {

    private final PublishedCourseService publishedCourseService;

    public PublicCourseResource(PublishedCourseService publishedCourseService) {
        this.publishedCourseService = publishedCourseService;
    }

    /**
     * {@code GET /api/public/courses} : the published courses with their lessons.
     *
     * @param language keeps only the courses in this language, for example {@code pt}.
     */
    @GetMapping("")
    public List<PublishedCourseDTO> getCourses(@RequestParam(name = "language", required = false) String language) {
        return publishedCourseService.findAll(language);
    }

    /**
     * {@code GET /api/public/courses/:slug} : one published course with its lessons.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PublishedCourseDTO> getCourse(@PathVariable("slug") String slug) {
        return ResponseUtil.wrapOrNotFound(publishedCourseService.findOne(slug));
    }
}
