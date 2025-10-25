package org.globsframework.json;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.IsJsonContent_;
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
            GlobTypeLoaderFactory.create(TypeWithJsonAttr.class).load();
        }
    }
}
