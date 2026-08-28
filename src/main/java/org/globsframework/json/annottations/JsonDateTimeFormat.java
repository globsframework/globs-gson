package org.globsframework.json.annottations;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.BooleanField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class JsonDateTimeFormat {
    public static final GlobType TYPE;

    public static final StringField format;

    public static final BooleanField strictIso8601;

    public static final BooleanField useFastIso8601;

    public static final BooleanField useLocalZone;

    public static final StringField nullValue;

    public static final Key UNIQUE_KEY;

    static {
        GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("JsonDateTimeFormat");
        format = typeBuilder.declareStringField("format");
        strictIso8601 = typeBuilder.declareBooleanField("strictIso8601");
        useFastIso8601 = typeBuilder.declareBooleanField("useFastIso8601");
        useLocalZone = typeBuilder.declareBooleanField("useLocalZone");
        nullValue = typeBuilder.declareStringField("nullValue");
        TYPE = typeBuilder.build();
        UNIQUE_KEY = KeyBuilder.newEmptyKey(TYPE);
//        GlobTypeLoaderFactory.create(JsonDateTimeFormat.class, "JsonDateTimeFormat")
//                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
//                        .set(format, ((JsonDateTimeFormat_) annotation).pattern())
//                        .set(useLocalZone, ((JsonDateTimeFormat_) annotation).asLocal())
//                        .set(nullValue, ((JsonDateTimeFormat_) annotation).nullValue())
//                        .set(strictIso8601, ((JsonDateTimeFormat_) annotation).strictIso8601())
//                )
//                .load();
    }

    public static Glob create(String pattern, boolean useLocalZone, String nullValue) {
        return TYPE.instantiate()
                .set(format, pattern)
                .set(JsonDateTimeFormat.useLocalZone, useLocalZone)
                .set(JsonDateTimeFormat.nullValue, nullValue);
    }
}
