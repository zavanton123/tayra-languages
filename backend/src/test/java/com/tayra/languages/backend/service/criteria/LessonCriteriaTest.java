package com.tayra.languages.backend.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class LessonCriteriaTest {

    @Test
    void newLessonCriteriaHasAllFiltersNullTest() {
        var lessonCriteria = new LessonCriteria();
        assertThat(lessonCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void lessonCriteriaFluentMethodsCreatesFiltersTest() {
        var lessonCriteria = new LessonCriteria();

        setAllFilters(lessonCriteria);

        assertThat(lessonCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void lessonCriteriaCopyCreatesNullFilterTest() {
        var lessonCriteria = new LessonCriteria();
        var copy = lessonCriteria.copy();

        assertThat(lessonCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(lessonCriteria)
        );
    }

    @Test
    void lessonCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var lessonCriteria = new LessonCriteria();
        setAllFilters(lessonCriteria);

        var copy = lessonCriteria.copy();

        assertThat(lessonCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(lessonCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var lessonCriteria = new LessonCriteria();

        assertThat(lessonCriteria).hasToString("LessonCriteria{}");
    }

    private static void setAllFilters(LessonCriteria lessonCriteria) {
        lessonCriteria.id();
        lessonCriteria.slug();
        lessonCriteria.title();
        lessonCriteria.summary();
        lessonCriteria.sortOrder();
        lessonCriteria.courseId();
        lessonCriteria.distinct();
    }

    private static Condition<LessonCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getSummary()) &&
                condition.apply(criteria.getSortOrder()) &&
                condition.apply(criteria.getCourseId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<LessonCriteria> copyFiltersAre(LessonCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getSummary(), copy.getSummary()) &&
                condition.apply(criteria.getSortOrder(), copy.getSortOrder()) &&
                condition.apply(criteria.getCourseId(), copy.getCourseId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
