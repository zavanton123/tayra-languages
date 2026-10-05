package com.tayra.languages.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tayra.languages.backend.domain.enumeration.CourseLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A course: lessons to read in order, for one language.
 */
@Entity
@Table(name = "course")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Course implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * The course's id in the app, stable once published, e.g. pt-primeiros-passos.
     */
    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Column(name = "slug", length = 80, nullable = false, unique = true)
    private String slug;

    /**
     * ISO 639-1 code of the language taught.
     */
    @NotNull
    @Size(min = 2, max = 8)
    @Column(name = "language_code", length = 8, nullable = false)
    private String languageCode;

    @NotNull
    @Size(max = 200)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CourseLevel level;

    @Size(max = 100)
    @Column(name = "topic", length = 100)
    private String topic;

    /**
     * Where the course comes in the list of its language.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /**
     * Only published courses are served to the app.
     */
    @NotNull
    @Column(name = "published", nullable = false)
    private Boolean published;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "course")
    @JsonIgnoreProperties(value = { "course" }, allowSetters = true)
    private Set<Lesson> lessons = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Course id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return this.slug;
    }

    public Course slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLanguageCode() {
        return this.languageCode;
    }

    public Course languageCode(String languageCode) {
        this.setLanguageCode(languageCode);
        return this;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTitle() {
        return this.title;
    }

    public Course title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Course description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CourseLevel getLevel() {
        return this.level;
    }

    public Course level(CourseLevel level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(CourseLevel level) {
        this.level = level;
    }

    public String getTopic() {
        return this.topic;
    }

    public Course topic(String topic) {
        this.setTopic(topic);
        return this;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Course sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getPublished() {
        return this.published;
    }

    public Course published(Boolean published) {
        this.setPublished(published);
        return this;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Set<Lesson> getLessons() {
        return this.lessons;
    }

    public void setLessons(Set<Lesson> lessons) {
        if (this.lessons != null) {
            this.lessons.forEach(i -> i.setCourse(null));
        }
        if (lessons != null) {
            lessons.forEach(i -> i.setCourse(this));
        }
        this.lessons = lessons;
    }

    public Course lessons(Set<Lesson> lessons) {
        this.setLessons(lessons);
        return this;
    }

    public Course addLesson(Lesson lesson) {
        this.lessons.add(lesson);
        lesson.setCourse(this);
        return this;
    }

    public Course removeLesson(Lesson lesson) {
        this.lessons.remove(lesson);
        lesson.setCourse(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Course)) {
            return false;
        }
        return getId() != null && getId().equals(((Course) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Course{" +
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
