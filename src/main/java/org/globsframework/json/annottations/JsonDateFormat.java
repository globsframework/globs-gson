package org.globsframework.json.annottations;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;
import org.globsframework.core.model.MutableGlob;

public class JsonDateFormat {
    public static final GlobType TYPE;

    public static final StringField FORMAT;

    @InitUniqueKey
    public static final Key UNIQUE_KEY;

    static {
        GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("JsonDateFormat");
        FORMAT = typeBuilder.declareStringField("format");
        typeBuilder.register(GlobCreateFromAnnotation.class, annotation -> get((JsonDateFormat_) annotation));
        TYPE = typeBuilder.build();
        UNIQUE_KEY = KeyBuilder.newEmptyKey(TYPE);
    }

    private static MutableGlob get(JsonDateFormat_ annotation) {
        return TYPE.instantiate().set(FORMAT, annotation.value());
    }

}
