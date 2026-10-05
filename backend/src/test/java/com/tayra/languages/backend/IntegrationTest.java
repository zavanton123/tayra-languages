package com.tayra.languages.backend;

import com.tayra.languages.backend.config.AsyncSyncConfiguration;
import com.tayra.languages.backend.config.EmbeddedSQL;
import com.tayra.languages.backend.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        TayraApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.tayra.languages.backend.config.JacksonHibernateConfiguration.class,
    }
)
@EmbeddedSQL
public @interface IntegrationTest {}
