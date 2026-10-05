package com.tayra.languages.backend.service.mapper;

import com.tayra.languages.backend.domain.Course;
import com.tayra.languages.backend.service.dto.CourseDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Course} and its DTO {@link CourseDTO}.
 */
@Mapper(componentModel = "spring")
public interface CourseMapper extends EntityMapper<CourseDTO, Course> {}
