package com.tayra.languages.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CourseTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Course getCourseSample1() {
        return new Course()
            .id(1L)
            .slug("slug1")
            .languageCode("languageCode1")
            .title("title1")
            .description("description1")
            .topic("topic1")
            .sortOrder(1);
    }

    public static Course getCourseSample2() {
        return new Course()
            .id(2L)
            .slug("slug2")
            .languageCode("languageCode2")
            .title("title2")
            .description("description2")
            .topic("topic2")
            .sortOrder(2);
    }

    public static Course getCourseRandomSampleGenerator() {
        return new Course()
            .id(longCount.incrementAndGet())
            .slug(UUID.randomUUID().toString())
            .languageCode(UUID.randomUUID().toString())
            .title(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .topic(UUID.randomUUID().toString())
            .sortOrder(intCount.incrementAndGet());
    }
}
