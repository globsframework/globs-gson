package org.globsframework.json;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.JsonHideValue;
import org.globsframework.json.annottations.JsonHideValue_;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class CustomTest {

    @Test
    public void name() throws IOException {
        JsonSerializerServiceImpl jsonSerializerService = new JsonSerializerServiceImpl();
        final GlobJson globJson = jsonSerializerService.get(Custom.TYPE);
        Glob glob = Custom.TYPE.instantiate().set(Custom.custom, "AAA").set(Custom.SECRET, "secret");
        final StringWriter writer = new StringWriter();
        globJson.write(glob, writer, true);
        Assert.assertEquals("{\"_kind\":\"Custom\",\"custom\":\"TR_AAA\",\"secret\":\"secret\"}", writer.toString());
        writer.getBuffer().setLength(0);
        globJson.write(glob, writer);
        Assert.assertEquals("{\"custom\":\"TR_AAA\",\"secret\":\"secret\"}", writer.toString());
        // No custom serialisation here
        Assert.assertEquals("{\"_kind\":\"Custom\",\"custom\":\"AAA\",\"secret\":\"****\"}", GSonUtils.encodeHidSensitiveData(glob));
    }

    static class Custom {
        public static final GlobType TYPE;

        public static final StringField custom;

        public static final StringField SECRET;

        static {
            final GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("Custom");
            TYPE = globTypeBuilder.unCompleteType();
            custom = globTypeBuilder.declareStringField("custom");
            SECRET = globTypeBuilder.declareStringField("secret", JsonHideValue.UNIQUE_GLOB);
            globTypeBuilder.complete();

            globTypeBuilder.register(custom, ToStringFieldJsonSerializer.class, new ToStringFieldJsonSerializer() {

                @Override
                public String serialize(Glob value) {
                    return "TR_" + value.get(custom);
                }

                @Override
                public void deserialize(String value, MutableGlob glob) {
                    glob.set(custom, value.substring(3));
                }
            });
        }
    }
}
