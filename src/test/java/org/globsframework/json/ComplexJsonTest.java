package org.globsframework.json;

import junit.framework.Assert;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.*;
import org.globsframework.json.field.JsonSerializerServiceImpl;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ComplexJsonTest {

    @Test
    public void name() throws IOException {
        MutableGlob a = A.TYPE.instantiate()
                .set(A.a, "a")
                .set(A.b, new Glob[]{
                        B.TYPE.instantiate()
                                .set(B.name, "b1")
                                .set(B.otherField, "o1"),
                        B.TYPE.instantiate()
                                .set(B.name, "b2")
                                .set(B.otherField, "o2")
                });
        String encode;
        final JsonSerializerServiceImpl jsonSerializerService = new JsonSerializerServiceImpl();
        final GlobJson globJson = jsonSerializerService.get(A.TYPE);
        {
            encode = GSonUtils.encode(a, true);
            Assert.assertEquals("""
                    {"_kind":"A","a":"a","b":{"b1":{"otherField":"o1"},"b2":{"otherField":"o2"}}}""", encode);
        }
        {
            final StringWriter writer = new StringWriter();
            globJson.write(a, writer, true);
            encode = writer.toString();
            Assert.assertEquals("""
                    {"_kind":"A","a":"a","b":{"b1":{"otherField":"o1"},"b2":{"otherField":"o2"}}}""", encode);
        }
        {
            Glob newA = GSonUtils.decode(encode, A.TYPE);
            check(newA);
        }
        {
            Glob newA = globJson.read(new StringReader(encode));
            check(newA);
        }
        {
            final StringWriter writer = new StringWriter();
            globJson.write(new Glob[]{a, null, a}, writer, true);
            encode = writer.toString();
            Assert.assertEquals("""
                    [{"_kind":"A","a":"a","b":{"b1":{"otherField":"o1"},"b2":{"otherField":"o2"}}},null,{"_kind":"A","a":"a","b":{"b1":{"otherField":"o1"},"b2":{"otherField":"o2"}}}]""", encode);
            final StringWriter secondWrite = new StringWriter();
            globJson.write(globJson.readArray(new StringReader(encode)), secondWrite, true);
            Assert.assertEquals(encode, secondWrite.toString());
        }
    }

    private static void check(Glob newA) {
        Assert.assertEquals(2, newA.get(A.b).length);
        Assert.assertEquals("b1", newA.get(A.b)[0].get(B.name));
        Assert.assertEquals("o1", newA.get(A.b)[0].get(B.otherField));
        Assert.assertEquals("b2", newA.get(A.b)[1].get(B.name));
        Assert.assertEquals("o2", newA.get(A.b)[1].get(B.otherField));
    }


    public static class A {
        public static final GlobType TYPE;

        public static final StringField a;

        public static final GlobArrayField<B> b;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("A");
            a = globTypeBuilder.declareStringField("a");
            b = globTypeBuilder.declareGlobArrayField("b", () -> B.TYPE, JsonAsObject.UNIQUE_GLOB);
            TYPE =  globTypeBuilder.build();
        }
    }

    public static class B {
        public static GlobType TYPE;

        public static StringField name;

        public static StringField otherField;

        static {
            GlobTypeBuilder  globTypeBuilder = GlobTypeBuilderFactory.create("B");
            name = globTypeBuilder.declareStringField("name", JsonValueAsField.UNIQUE_GLOB);
            otherField = globTypeBuilder.declareStringField("otherField");
            TYPE = globTypeBuilder.build();
        }
    }
}
