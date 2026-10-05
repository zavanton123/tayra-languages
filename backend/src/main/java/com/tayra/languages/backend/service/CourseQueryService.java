package com.tayra.languages.backend.service;

import com.tayra.languages.backend.domain.*; // for static metamodels
import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.repository.CourseRepository;
import com.tayra.languages.backend.service.criteria.CourseCriteria;
import com.tayra.languages.backend.service.dto.CourseDTO;
import com.tayra.languages.backend.service.mapper.CourseMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Course} entities in the database.
 * The main input is a {@link CourseCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CourseDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CourseQueryService extends QueryService<Course> {

    private static final Logger LOG = LoggerFactory.getLogger(CourseQueryService.class);

    private final CourseRepository courseRepository;

    private final CourseMapper courseMapper;

    public CourseQueryService(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    /**
     * Return a {@link Page} of {@link CourseDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CourseDTO> findByCriteria(CourseCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Course> specification = createSpecification(criteria);
        return courseRepository.findAll(specification, page).map(courseMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CourseCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Course> specification = createSpecification(criteria);
        return courseRepository.count(specification);
    }

    /**
     * Function to convert {@link CourseCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Course> createSpecification(CourseCriteria criteria) {
        Specification<Course> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Course_.id),
                    buildStringSpecification(criteria.getSlug(), Course_.slug),
                    buildStringSpecification(criteria.getLanguageCode(), Course_.languageCode),
                    buildStringSpecification(criteria.getTitle(), Course_.title),
                    buildStringSpecification(criteria.getDescription(), Course_.description),
                    buildSpecification(criteria.getLevel(), Course_.level),
                    buildStringSpecification(criteria.getTopic(), Course_.topic),
                    buildRangeSpecification(criteria.getSortOrder(), Course_.sortOrder),
                    buildSpecification(criteria.getPublished(), Course_.published),
                    buildSpecification(criteria.getLessonId(), root -> root.join(Course_.lessons, JoinType.LEFT).get(Lesson_.id))
                )
            );
        }
        return specification;
    }
}
