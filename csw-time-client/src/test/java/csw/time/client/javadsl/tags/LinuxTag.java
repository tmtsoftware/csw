package csw.time.client.javadsl.tags;


import org.scalatest.TagAnnotation;

import java.lang.annotation.*;

@TagAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface LinuxTag {}