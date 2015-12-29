package cli;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks methods to be treated as commands to be invoked by a {@link Shell}.
 *
 * @see Shell#register(Object)
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
	/**
	 * Returns the name of the command.<br/>
	 * If the value is not specified, the method name is used instead.
	 *
	 * @return the command name
	 */
	String value() default "";
}
