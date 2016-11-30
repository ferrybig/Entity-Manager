package me.ferrybig.javacoding.entitymanager;

import java.lang.annotation.Documented;

/**
 *
 * @author Fernando
 */
@Documented
public @interface Entity {
	String table();
}
