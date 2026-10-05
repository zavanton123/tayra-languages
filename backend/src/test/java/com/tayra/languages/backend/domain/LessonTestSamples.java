package com.tayra.languages.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LessonTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Lesson getLessonSample1() {
        return new Lesson().id(1L).slug("slug1").title("title1").summary("summary1").sortOrder(1);
    }

    public static Lesson getLessonSample2() {
        return new Lesson().id(2L).slug("slug2").title("title2").summary("summary2").sortOrder(2);
    }

    public static Lesson getLessonRandomSampleGenerator() {
        return new Lesson()
            .id(longCount.incrementAndGet())
            .slug(UUID.randomUUID().toString())
            .title(UUID.randomUUID().toString())
            .summary(UUID.randomUUID().toString())
            .sortOrder(intCount.incrementAndGet());
    }
}
