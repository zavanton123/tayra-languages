package com.tayra.languages.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.tayra.languages.backend.domain.Lesson} entity.
 */
@Schema(description = "A text to read, part of a course.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LessonDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Schema(
        description = "The lesson's id in the app, stable once published, e.g. pt-primeiros-passos-1.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String slug;

    @NotNull
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String summary;

    @Schema(description = "The text of the lesson.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String content;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Where the lesson comes in its course.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortOrder;

    @NotNull
    private CourseDTO course;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public CourseDTO getCourse() {
        return course;
    }

    public void setCourse(CourseDTO course) {
        this.course = course;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LessonDTO)) {
            return false;
        }

        LessonDTO lessonDTO = (LessonDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, lessonDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LessonDTO{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", title='" + getTitle() + "'" +
            ", summary='" + getSummary() + "'" +
            ", content='" + getContent() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", course=" + getCourse() +
            "}";
    }
}
