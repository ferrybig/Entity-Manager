package me.ferrybig.javacoding.entitymanager;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author Fernando
 */
@Documented
@Target(value=FIELD)
@Retention(value=RUNTIME)
public @interface JoinedColumn {
	String joinedField();
}
