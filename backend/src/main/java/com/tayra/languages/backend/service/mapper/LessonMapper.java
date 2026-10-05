package com.tayra.languages.backend.service.mapper;

import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.domain.Lesson;
import com.tayra.languages.backend.service.dto.CourseDTO;
import com.tayra.languages.backend.service.dto.LessonDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Lesson} and its DTO {@link LessonDTO}.
 */
@Mapper(componentModel = "spring")
public interface LessonMapper extends EntityMapper<LessonDTO, Lesson> {
    @Mapping(target = "course", source = "course", qualifiedByName = "courseTitle")
    LessonDTO toDto(Lesson s);

    @Named("courseTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    CourseDTO toDtoCourseTitle(Course course);
}
