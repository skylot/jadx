package jadx.core.dex.visitors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for describe dependencies of jadx visitors
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JadxVisitor {
	/**
	 * Visitor short name (identifier)
	 */
	String name();

	/**
	 * Detailed visitor description
	 */
	String desc() default "";

	/**
	 * This visitor must be run <b>after</b> listed visitors
	 */
	Class<? extends IDexTreeVisitor>[] runAfter() default {};

	/**
	 * This visitor must be run <b>before</b> listed visitors
	 */
	Class<? extends IDexTreeVisitor>[] runBefore() default {};
}
