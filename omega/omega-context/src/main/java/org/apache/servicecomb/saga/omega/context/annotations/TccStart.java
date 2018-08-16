package org.apache.servicecomb.saga.omega.context.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the annotated method will start a TCC .
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface TccStart {
  /**
   * TCC timeout, in seconds. <br>
   * Default value is 0, which means never timeout.
   *
   * @return
   */
  int timeout() default 0;
}
