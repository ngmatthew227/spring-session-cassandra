package io.github.ngmatthew227.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(CassandraHttpSessionConfiguration.class)
@Configuration
public @interface EnableCassandraHttpSession {

	String tableName() default "spring_session";

	int maxInactiveIntervalInSeconds() default 1800;

	DefaultConsistencyLevel consistencyLevel() default DefaultConsistencyLevel.ONE;

}
