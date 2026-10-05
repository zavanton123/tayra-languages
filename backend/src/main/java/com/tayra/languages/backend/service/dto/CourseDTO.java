package com.tayra.languages.backend.service.dto;

import com.tayra.languages.backend.domain.enumeration.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.tayra.languages.backend.domain.Course} entity.
 */
@Schema(description = "A course: lessons to read in order, for one language.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CourseDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Schema(
        description = "The course's id in the app, stable once published, e.g. pt-primeiros-passos.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String slug;

    @NotNull
    @Size(min = 2, max = 8)
    @Schema(description = "ISO 639-1 code of the language taught.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String languageCode;

    @NotNull
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private CourseLevel level;

    @Size(max = 100)
    private String topic;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Where the course comes in the list of its language.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortOrder;

    @NotNull
    @Schema(description = "Only published courses are served to the app.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean published;

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

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public void setLevel(CourseLevel level) {
        this.level = level;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CourseDTO)) {
            return false;
        }

        CourseDTO courseDTO = (CourseDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, courseDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CourseDTO{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", languageCode='" + getLanguageCode() + "'" +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", level='" + getLevel() + "'" +
            ", topic='" + getTopic() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", published='" + getPublished() + "'" +
            "}";
    }
}
