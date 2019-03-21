package csw.time.core.tags;

import org.scalatest.TagAnnotation;

import java.lang.annotation.*;

@TagAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface TimeTests {}
