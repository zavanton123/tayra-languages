package com.tayra.languages.backend.web.rest;

import static com.tayra.languages.backend.domain.LessonAsserts.*;
import static com.tayra.languages.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tayra.languages.backend.IntegrationTest;
import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.domain.Lesson;
import com.tayra.languages.backend.repository.LessonRepository;
import com.tayra.languages.backend.service.LessonService;
import com.tayra.languages.backend.service.dto.LessonDTO;
import com.tayra.languages.backend.service.mapper.LessonMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link LessonResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class LessonResourceIT {

    private static final String DEFAULT_SLUG = "t";
    private static final String UPDATED_SLUG = "w27pjh";

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SUMMARY = "AAAAAAAAAA";
    private static final String UPDATED_SUMMARY = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final Integer DEFAULT_SORT_ORDER = 0;
    private static final Integer UPDATED_SORT_ORDER = 1;
    private static final Integer SMALLER_SORT_ORDER = 0 - 1;

    private static final String ENTITY_API_URL = "/api/lessons";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LessonRepository lessonRepository;

    @Mock
    private LessonRepository lessonRepositoryMock;

    @Autowired
    private LessonMapper lessonMapper;

    @Mock
    private LessonService lessonServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLessonMockMvc;

    private Lesson lesson;

    private Lesson insertedLesson;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Lesson createEntity(EntityManager em) {
        Lesson lesson = new Lesson()
            .slug(DEFAULT_SLUG)
            .title(DEFAULT_TITLE)
            .summary(DEFAULT_SUMMARY)
            .content(DEFAULT_CONTENT)
            .sortOrder(DEFAULT_SORT_ORDER);
        // Add required entity
        Course course;
        if (TestUtil.findAll(em, Course.class).isEmpty()) {
            course = CourseResourceIT.createEntity();
            em.persist(course);
            em.flush();
        } else {
            course = TestUtil.findAll(em, Course.class).get(0);
        }
        lesson.setCourse(course);
        return lesson;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Lesson createUpdatedEntity(EntityManager em) {
        Lesson updatedLesson = new Lesson()
            .slug(UPDATED_SLUG)
            .title(UPDATED_TITLE)
            .summary(UPDATED_SUMMARY)
            .content(UPDATED_CONTENT)
            .sortOrder(UPDATED_SORT_ORDER);
        // Add required entity
        Course course;
        if (TestUtil.findAll(em, Course.class).isEmpty()) {
            course = CourseResourceIT.createUpdatedEntity();
            em.persist(course);
            em.flush();
        } else {
            course = TestUtil.findAll(em, Course.class).get(0);
        }
        updatedLesson.setCourse(course);
        return updatedLesson;
    }

    @BeforeEach
    void initTest() {
        lesson = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedLesson != null) {
            lessonRepository.delete(insertedLesson);
            insertedLesson = null;
        }
    }

    @Test
    @Transactional
    void createLesson() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);
        var returnedLessonDTO = om.readValue(
            restLessonMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LessonDTO.class
        );

        // Validate the Lesson in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLesson = lessonMapper.toEntity(returnedLessonDTO);
        assertLessonUpdatableFieldsEquals(returnedLesson, getPersistedLesson(returnedLesson));

        insertedLesson = returnedLesson;
    }

    @Test
    @Transactional
    void createLessonWithExistingId() throws Exception {
        // Create the Lesson with an existing ID
        lesson.setId(1L);
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLessonMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lesson.setSlug(null);

        // Create the Lesson, which fails.
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        restLessonMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lesson.setTitle(null);

        // Create the Lesson, which fails.
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        restLessonMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lesson.setSortOrder(null);

        // Create the Lesson, which fails.
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        restLessonMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLessons() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList
        restLessonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(lesson.getId().intValue())))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLessonsWithEagerRelationshipsIsEnabled() throws Exception {
        when(lessonServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLessonMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(lessonServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLessonsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(lessonServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLessonMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(lessonRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getLesson() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get the lesson
        restLessonMockMvc
            .perform(get(ENTITY_API_URL_ID, lesson.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(lesson.getId().intValue()))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.summary").value(DEFAULT_SUMMARY))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER));
    }

    @Test
    @Transactional
    void getLessonsByIdFiltering() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        Long id = lesson.getId();

        defaultLessonFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultLessonFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultLessonFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllLessonsBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where slug equals to
        defaultLessonFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLessonsBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where slug in
        defaultLessonFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLessonsBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where slug is not null
        defaultLessonFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllLessonsBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where slug contains
        defaultLessonFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLessonsBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where slug does not contain
        defaultLessonFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllLessonsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where title equals to
        defaultLessonFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllLessonsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where title in
        defaultLessonFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllLessonsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where title is not null
        defaultLessonFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllLessonsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where title contains
        defaultLessonFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllLessonsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where title does not contain
        defaultLessonFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllLessonsBySummaryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where summary equals to
        defaultLessonFiltering("summary.equals=" + DEFAULT_SUMMARY, "summary.equals=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllLessonsBySummaryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where summary in
        defaultLessonFiltering("summary.in=" + DEFAULT_SUMMARY + "," + UPDATED_SUMMARY, "summary.in=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllLessonsBySummaryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where summary is not null
        defaultLessonFiltering("summary.specified=true", "summary.specified=false");
    }

    @Test
    @Transactional
    void getAllLessonsBySummaryContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where summary contains
        defaultLessonFiltering("summary.contains=" + DEFAULT_SUMMARY, "summary.contains=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllLessonsBySummaryNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where summary does not contain
        defaultLessonFiltering("summary.doesNotContain=" + UPDATED_SUMMARY, "summary.doesNotContain=" + DEFAULT_SUMMARY);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder equals to
        defaultLessonFiltering("sortOrder.equals=" + DEFAULT_SORT_ORDER, "sortOrder.equals=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder in
        defaultLessonFiltering("sortOrder.in=" + DEFAULT_SORT_ORDER + "," + UPDATED_SORT_ORDER, "sortOrder.in=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder is not null
        defaultLessonFiltering("sortOrder.specified=true", "sortOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder is greater than or equal to
        defaultLessonFiltering("sortOrder.greaterThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.greaterThanOrEqual=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder is less than or equal to
        defaultLessonFiltering("sortOrder.lessThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.lessThanOrEqual=" + SMALLER_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder is less than
        defaultLessonFiltering("sortOrder.lessThan=" + UPDATED_SORT_ORDER, "sortOrder.lessThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsBySortOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        // Get all the lessonList where sortOrder is greater than
        defaultLessonFiltering("sortOrder.greaterThan=" + SMALLER_SORT_ORDER, "sortOrder.greaterThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllLessonsByCourseIsEqualToSomething() throws Exception {
        Course course;
        if (TestUtil.findAll(em, Course.class).isEmpty()) {
            lessonRepository.saveAndFlush(lesson);
            course = CourseResourceIT.createEntity();
        } else {
            course = TestUtil.findAll(em, Course.class).get(0);
        }
        em.persist(course);
        em.flush();
        lesson.setCourse(course);
        lessonRepository.saveAndFlush(lesson);
        Long courseId = course.getId();
        // Get all the lessonList where course equals to courseId
        defaultLessonShouldBeFound("courseId.equals=" + courseId);

        // Get all the lessonList where course equals to (courseId + 1)
        defaultLessonShouldNotBeFound("courseId.equals=" + (courseId + 1));
    }

    private void defaultLessonFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultLessonShouldBeFound(shouldBeFound);
        defaultLessonShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultLessonShouldBeFound(String filter) throws Exception {
        restLessonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(lesson.getId().intValue())))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)));

        // Check, that the count call also returns 1
        restLessonMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultLessonShouldNotBeFound(String filter) throws Exception {
        restLessonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restLessonMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingLesson() throws Exception {
        // Get the lesson
        restLessonMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLesson() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lesson
        Lesson updatedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLesson are not directly saved in db
        em.detach(updatedLesson);
        updatedLesson
            .slug(UPDATED_SLUG)
            .title(UPDATED_TITLE)
            .summary(UPDATED_SUMMARY)
            .content(UPDATED_CONTENT)
            .sortOrder(UPDATED_SORT_ORDER);
        LessonDTO lessonDTO = lessonMapper.toDto(updatedLesson);

        restLessonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, lessonDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO))
            )
            .andExpect(status().isOk());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLessonToMatchAllProperties(updatedLesson);
    }

    @Test
    @Transactional
    void putNonExistingLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, lessonDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(lessonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLessonWithPatch() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lesson using partial update
        Lesson partialUpdatedLesson = new Lesson();
        partialUpdatedLesson.setId(lesson.getId());

        restLessonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLesson.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLesson))
            )
            .andExpect(status().isOk());

        // Validate the Lesson in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLessonUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLesson, lesson), getPersistedLesson(lesson));
    }

    @Test
    @Transactional
    void fullUpdateLessonWithPatch() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lesson using partial update
        Lesson partialUpdatedLesson = new Lesson();
        partialUpdatedLesson.setId(lesson.getId());

        partialUpdatedLesson
            .slug(UPDATED_SLUG)
            .title(UPDATED_TITLE)
            .summary(UPDATED_SUMMARY)
            .content(UPDATED_CONTENT)
            .sortOrder(UPDATED_SORT_ORDER);

        restLessonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLesson.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLesson))
            )
            .andExpect(status().isOk());

        // Validate the Lesson in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLessonUpdatableFieldsEquals(partialUpdatedLesson, getPersistedLesson(partialUpdatedLesson));
    }

    @Test
    @Transactional
    void patchNonExistingLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, lessonDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(lessonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(lessonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLesson() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lesson.setId(longCount.incrementAndGet());

        // Create the Lesson
        LessonDTO lessonDTO = lessonMapper.toDto(lesson);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLessonMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(lessonDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Lesson in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLesson() throws Exception {
        // Initialize the database
        insertedLesson = lessonRepository.saveAndFlush(lesson);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the lesson
        restLessonMockMvc
            .perform(delete(ENTITY_API_URL_ID, lesson.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return lessonRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Lesson getPersistedLesson(Lesson lesson) {
        return lessonRepository.findById(lesson.getId()).orElseThrow();
    }

    protected void assertPersistedLessonToMatchAllProperties(Lesson expectedLesson) {
        assertLessonAllPropertiesEquals(expectedLesson, getPersistedLesson(expectedLesson));
    }

    protected void assertPersistedLessonToMatchUpdatableProperties(Lesson expectedLesson) {
        assertLessonAllUpdatablePropertiesEquals(expectedLesson, getPersistedLesson(expectedLesson));
    }
}
