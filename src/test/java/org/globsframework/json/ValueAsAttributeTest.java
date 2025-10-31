package org.globsframework.json;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.json.annottations.JsonFlatten;
import org.globsframework.json.annottations.JsonFlattenAttribute;
import org.globsframework.json.annottations.JsonFlattenTargetArray;
import org.globsframework.json.annottations.JsonFlattenTargetAttribute;
import org.globsframework.json.field.JsonSerializerServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ValueAsAttributeTest {

    @Test
    public void name() throws IOException {
        String jsonData = """
                {
                  "1": {
                    "k1": "v1",
                    "k2": "v2"
                    },
                  "2": {
                    "k1": "v3",
                    "k2": "v4"
                    }
                }
                """;

        GlobJsonService globJsonService = new JsonSerializerServiceImpl();
        final GlobJson globJson = globJsonService.get(Root.TYPE);
        final Glob read = globJson.read(new StringReader(jsonData));
        final StringWriter writer = new StringWriter();
        globJson.write(read, writer);
        Assert.assertEquals(GSonUtils.normalize(jsonData), GSonUtils.normalize(writer.toString()));
    }

    public static class Root {
        public static final GlobType TYPE;

        public static final GlobArrayField l1s;

        static {
            GlobTypeBuilder builder = GlobTypeBuilderFactory.create("Root");
            builder.addAnnotation(JsonFlatten.UNIQUE_GLOB);
            TYPE = builder.unCompleteType();
            l1s = builder.declareGlobArrayField("l1s", L1.TYPE, JsonFlattenTargetArray.UNIQUE_GLOB);
            builder.complete();
        }
    }

    public static class L1 {
        public static final GlobType TYPE;

        public static final StringField name;

        public static final GlobArrayField l2s;

        static {
            GlobTypeBuilder builder = GlobTypeBuilderFactory.create("L1");
            builder.addAnnotation(JsonFlatten.UNIQUE_GLOB);
            TYPE = builder.unCompleteType();
            name = builder.declareStringField("name", JsonFlattenAttribute.UNIQUE_GLOB);
            l2s = builder.declareGlobArrayField("l2s", L2.TYPE, JsonFlattenTargetArray.UNIQUE_GLOB);
            builder.complete();
        }
    }
    public static class L2 {
        public static final GlobType TYPE;

        public static final StringField v1;

        public static final StringField v2;

        static {
            GlobTypeBuilder builder = GlobTypeBuilderFactory.create("L2");
            TYPE = builder.unCompleteType();
            v1 = builder.declareStringField("v1", JsonFlattenAttribute.UNIQUE_GLOB);
            v2 = builder.declareStringField("v2", JsonFlattenTargetAttribute.UNIQUE_GLOB);
            builder.complete();
        }
    }

}
