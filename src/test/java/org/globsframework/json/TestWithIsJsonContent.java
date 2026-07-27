package org.globsframework.json;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.StringArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.IsJsonContent;
import org.globsframework.json.annottations.IsJsonContent_;
import org.globsframework.json.field.JsonSerializerServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class TestWithIsJsonContent {

    @Test
    public void withComplexType() throws IOException {
        check("false", "{\"value\":false}");
        check("12", "{\"value\":12}");
        check("\"false\"", "{\"value\":\"false\"}");
        check("{\"a\":true}", "{\"value\":{\"a\":true}}");
        check("[{\"a\":true}]", "{\"value\":[{\"a\":true}]}");
    }

    private void check(String value, String expected) throws IOException {
        final JsonSerializerServiceImpl jsonSerializerService = new JsonSerializerServiceImpl();

        MutableGlob v = TypeWithJsonAttr.TYPE.instantiate().set(TypeWithJsonAttr.value, value);
        Assert.assertEquals(expected, GSonUtils.encode(v, false));
        final StringWriter writer = new StringWriter();
        final GlobJson globJson = jsonSerializerService.get(TypeWithJsonAttr.TYPE);
        globJson.write(v, writer);
        Assert.assertEquals(expected, writer.toString());
        {
            Glob r = GSonUtils.decode(GSonUtils.encode(v, false), TypeWithJsonAttr.TYPE);
            Assert.assertTrue(TypeWithJsonAttr.value.valueEqual(v.get(TypeWithJsonAttr.value), r.get(TypeWithJsonAttr.value)));
        }
        {
            Glob r = globJson.read(new StringReader(expected));
            Assert.assertTrue(TypeWithJsonAttr.value.valueEqual(v.get(TypeWithJsonAttr.value), r.get(TypeWithJsonAttr.value)));
        }
    }


    public static class TypeWithJsonAttr {
        public static GlobType TYPE;
        @IsJsonContent_
        public static StringField value;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("typeWithJsonAttr");
            value = globTypeBuilder.declareStringField("value", IsJsonContent.UNIQUE_GLOB);
            TYPE = globTypeBuilder.build();
        }
    }

    // "_kind" placed after "values" below forces GlobGSonDeserializer.readFieldByField, which
    // deserializes each field from a parsed JsonElement tree via GSonVisitor rather than the
    // JsonReader-based visitor. GSonVisitor.visitStringArray used to silently turn primitive
    // (non object/array) elements of an IsJsonContent array into null instead of converting them.
    @Test
    public void arrayOfJsonContentWithPrimitiveElements() {
        Glob decoded = GSonUtils.decode(
                "{\"values\":[12,true,\"str\",{\"a\":1},[1,2]],\"_kind\":\"typeWithJsonArrayAttr\"}",
                name -> TypeWithJsonArrayAttr.TYPE.getName().equals(name) ? TypeWithJsonArrayAttr.TYPE : null);
        Assert.assertArrayEquals(new String[]{"12", "true", "\"str\"", "{\"a\":1}", "[1,2]"},
                decoded.get(TypeWithJsonArrayAttr.values));
    }

    public static class TypeWithJsonArrayAttr {
        public static GlobType TYPE;
        @IsJsonContent_
        public static StringArrayField values;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("typeWithJsonArrayAttr");
            values = globTypeBuilder.declareStringArrayField("values", IsJsonContent.UNIQUE_GLOB);
            TYPE = globTypeBuilder.build();
        }
    }
}
