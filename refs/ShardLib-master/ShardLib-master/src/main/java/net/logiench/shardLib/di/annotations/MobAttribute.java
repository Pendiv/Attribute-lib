package net.logiench.shardLib.di.annotations;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@BindingAnnotation // これがGuiceのBinding Annotationであることを示す
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface MobAttribute {
}
