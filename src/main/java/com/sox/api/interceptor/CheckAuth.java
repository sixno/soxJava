package com.sox.api.interceptor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CheckAuth {
    String index() default "";
    String value() default "1";
    String except() default "";
}
