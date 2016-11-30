package me.ferrybig.javacoding.entitymanager;

import java.lang.annotation.Documented;

/**
 *
 * @author Fernando
 */
@Documented
public @interface JoinedColumn {
	String joinedField();
}
