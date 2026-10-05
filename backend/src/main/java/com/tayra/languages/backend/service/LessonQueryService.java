package com.tayra.languages.backend.service;

import com.tayra.languages.backend.domain.*; // for static metamodels
import com.tayra.languages.backend.domain.Lesson;
import com.tayra.languages.backend.repository.LessonRepository;
import com.tayra.languages.backend.service.criteria.LessonCriteria;
import com.tayra.languages.backend.service.dto.LessonDTO;
import com.tayra.languages.backend.service.mapper.LessonMapper;
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
 * Service for executing complex queries for {@link Lesson} entities in the database.
 * The main input is a {@link LessonCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link LessonDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class LessonQueryService extends QueryService<Lesson> {

    private static final Logger LOG = LoggerFactory.getLogger(LessonQueryService.class);

    private final LessonRepository lessonRepository;

    private final LessonMapper lessonMapper;

    public LessonQueryService(LessonRepository lessonRepository, LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
    }

    /**
     * Return a {@link Page} of {@link LessonDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<LessonDTO> findByCriteria(LessonCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Lesson> specification = createSpecification(criteria);
        return lessonRepository.findAll(specification, page).map(lessonMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(LessonCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Lesson> specification = createSpecification(criteria);
        return lessonRepository.count(specification);
    }

    /**
     * Function to convert {@link LessonCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Lesson> createSpecification(LessonCriteria criteria) {
        Specification<Lesson> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Lesson_.course, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Lesson_.id),
                    buildStringSpecification(criteria.getSlug(), Lesson_.slug),
                    buildStringSpecification(criteria.getTitle(), Lesson_.title),
                    buildStringSpecification(criteria.getSummary(), Lesson_.summary),
                    buildRangeSpecification(criteria.getSortOrder(), Lesson_.sortOrder),
                    buildSpecification(criteria.getCourseId(), root -> root.join(Lesson_.course, JoinType.LEFT).get(Course_.id))
                )
            );
        }
        return specification;
    }
}
