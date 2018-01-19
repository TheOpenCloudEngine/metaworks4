package org.metaworks.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by uengine on 2017. 7. 12..
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestAssociation {
    String path();
    String serviceId() default "self";
    boolean validateLink() default false;
    String joinColumn() default "";
}
