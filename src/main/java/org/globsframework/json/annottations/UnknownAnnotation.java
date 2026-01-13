package org.globsframework.json.annottations;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.annotations.KeyField;
import org.globsframework.core.metamodel.annotations.KeyField_;
import org.globsframework.core.metamodel.fields.StringField;

public class UnknownAnnotation {
    public static final GlobType TYPE;

    @KeyField_
    public static final StringField uuid;

    @IsJsonContent_
    public static final StringField CONTENT;

    static {
        GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("UnknownAnnotation");
        uuid = typeBuilder.declareStringField("uuid", KeyField.ZERO);
        CONTENT = typeBuilder.declareStringField("content", IsJsonContent.UNIQUE_GLOB);
        TYPE = typeBuilder.build();
    }
}
