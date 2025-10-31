package org.globsframework.json.annottations;

import org.globsframework.core.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})
public @interface JsonFlattenTargetArray_ {

    GlobType TYPE = JsonFlattenTargetArray.TYPE;
}
