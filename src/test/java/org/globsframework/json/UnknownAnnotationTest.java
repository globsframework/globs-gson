package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.Assert;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.KeyField_;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.KeyBuilder;
import org.junit.Test;

public class UnknownAnnotationTest {

    @Test
    public void testWriteReadWriteRead() {
        GsonBuilder builder = GlobsGson.createBuilder(name -> {
                    if (name.equals(TypeToTest.TYPE.getName())) {
                        return TypeToTest.TYPE;
                    }
                    return null;
                }
        );
        Gson gson = builder.create();
        String s = gson.toJson(TypeToTest.TYPE);
        GlobType type1 = gson.fromJson(s, GlobType.class);

        Assert.assertFalse(type1.getField(TypeToTest.F1.getName())
                .hasAnnotation(KeyBuilder.newKey(Ann1.TYPE, "XXX")));

        String s2 = gson.toJson(type1);

        {
            GsonBuilder builderWithAnnotation = GlobsGson.createBuilder(name -> {
                        if (name.equals(TypeToTest.TYPE.getName())) {
                            return TypeToTest.TYPE;
                        }
                        if (name.equals(Ann1.TYPE.getName())) {
                            return Ann1.TYPE;
                        }
                        return null;
                    }
            );
            Gson gsonWithann = builderWithAnnotation.create();
            GlobType globType = gsonWithann.fromJson(s2, GlobType.class);
            Assert.assertTrue(globType.getField(TypeToTest.F1.getName())
                    .hasAnnotation(KeyBuilder.newKey(Ann1.TYPE, "XXX")));
        }
    }

    public static class TypeToTest {
        public static GlobType TYPE;

        public static StringField F1;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("typeToTest");
            F1 = typeBuilder.declareStringField("f1", Ann1.TYPE.instantiate().set(Ann1.UUID, "XXX").set(Ann1.SOME_DATA, "aa"));
            TYPE = typeBuilder.get();
        }
    }

    public static class Ann1 {
        public static GlobType TYPE;

        @KeyField_
        public static StringField UUID;

        public static StringField SOME_DATA;

        static {
            GlobTypeLoaderFactory.create(Ann1.class).load();
        }
    }
}
