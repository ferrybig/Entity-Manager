package me.ferrybig.javacoding.entitymanager;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author Fernando
 */
@Documented
@Target(value=TYPE)
@Retention(value=RUNTIME)
public @interface Entity {
	String table();
	boolean cache() default true;
	long cacheTimeOut() default 1000;
}
