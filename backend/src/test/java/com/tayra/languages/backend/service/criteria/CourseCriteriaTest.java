package com.tayra.languages.backend.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CourseCriteriaTest {

    @Test
    void newCourseCriteriaHasAllFiltersNullTest() {
        var courseCriteria = new CourseCriteria();
        assertThat(courseCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void courseCriteriaFluentMethodsCreatesFiltersTest() {
        var courseCriteria = new CourseCriteria();

        setAllFilters(courseCriteria);

        assertThat(courseCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void courseCriteriaCopyCreatesNullFilterTest() {
        var courseCriteria = new CourseCriteria();
        var copy = courseCriteria.copy();

        assertThat(courseCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(courseCriteria)
        );
    }

    @Test
    void courseCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var courseCriteria = new CourseCriteria();
        setAllFilters(courseCriteria);

        var copy = courseCriteria.copy();

        assertThat(courseCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(courseCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var courseCriteria = new CourseCriteria();

        assertThat(courseCriteria).hasToString("CourseCriteria{}");
    }

    private static void setAllFilters(CourseCriteria courseCriteria) {
        courseCriteria.id();
        courseCriteria.slug();
        courseCriteria.languageCode();
        courseCriteria.title();
        courseCriteria.description();
        courseCriteria.level();
        courseCriteria.topic();
        courseCriteria.sortOrder();
        courseCriteria.published();
        courseCriteria.lessonId();
        courseCriteria.distinct();
    }

    private static Condition<CourseCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getLanguageCode()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getDescription()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getTopic()) &&
                condition.apply(criteria.getSortOrder()) &&
                condition.apply(criteria.getPublished()) &&
                condition.apply(criteria.getLessonId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CourseCriteria> copyFiltersAre(CourseCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getLanguageCode(), copy.getLanguageCode()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getDescription(), copy.getDescription()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getTopic(), copy.getTopic()) &&
                condition.apply(criteria.getSortOrder(), copy.getSortOrder()) &&
                condition.apply(criteria.getPublished(), copy.getPublished()) &&
                condition.apply(criteria.getLessonId(), copy.getLessonId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
