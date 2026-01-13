package org.globsframework.json;

import com.google.gson.Gson;
import org.globsframework.core.metamodel.GlobModel;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.annotations.*;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.metamodel.impl.DefaultGlobModel;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.IsJsonContent;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;

import static org.globsframework.json.GlobsGsonAdapterTest.assertEquivalent;

public class GlobWithGlobFieldAndUnion {

//    static {
//        System.setProperty("org.globsframework.builder", "org.globsframework.model.generator.object.GeneratorGlobFactoryService");
//        GlobFactoryService.Builder.reset();
//    }

    public static final String A_GLOB = "{\n" +
                                        "  \"_kind\": \"test local type\",\n" +
                                        "  \"id\": 1,\n" +
                                        "  \"secondType\": {\n" +
                                        "    \"subFirst\": {\n" +
                                        "      \"data\": \"subFirst Data\",\n" +
                                        "      \"parent\": {\n" +
                                        "        \"name\": \"subFirst second level\"\n" +
                                        "      }\n" +
                                        "    }\n" +
                                        "  },\n" +
                                        "  \"arrayOfUnions\": [\n" +
                                        "    {\n" +
                                        "      \"subFirst\": {\n" +
                                        "        \"data\": \"subFirst array Data\"\n" +
                                        "      }\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "      \"subSecond\": {\n" +
                                        "        \"id\": 0,\n" +
                                        "        \"value\": 3.1415\n" +
                                        "      }\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"arrayOfType\": [{\n" +
                                        "    \"id\": 1,\n" +
                                        "    \"value\": 6.28\n" +
                                        "  }],\n" +
                                        "  \"simpleType\": {\n" +
                                        "    \"id\": 2,\n" +
                                        "    \"value\": 12.28\n" +
                                        "  }\n" +
                                        "}";

    public static class LocalType {
        @Required_
        public static final GlobType TYPE;

        @KeyField_
        public static final IntegerField ID;

        @Targets({SubSecondType.class, SubFirstType.class})
        public static final GlobUnionField SECOND_TYPE;

        @Targets({SubSecondType.class, SubFirstType.class})
        public static final GlobArrayUnionField ARRAY_OF_UNIONS;

        @Target(SubSecondType.class)
        public static final GlobArrayField ARRAY_OF_TYPE;

        @Target(SubSecondType.class)
        public static final GlobField SIMPLE_TYPE;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("test local type");
            globTypeBuilder.addAnnotation(Required.UNIQUE_GLOB);
            ID = globTypeBuilder.declareIntegerField("id", KeyField.ZERO);
            SECOND_TYPE = globTypeBuilder.declareGlobUnionField("secondType", new Supplier[]{() -> SubSecondType.TYPE, () -> SubFirstType.TYPE});
            ARRAY_OF_UNIONS = globTypeBuilder.declareGlobUnionArrayField("arrayOfUnions", new Supplier[]{() -> SubSecondType.TYPE, () -> SubFirstType.TYPE});
            ARRAY_OF_TYPE = globTypeBuilder.declareGlobArrayField("arrayOfType", () -> SubSecondType.TYPE);
            SIMPLE_TYPE = globTypeBuilder.declareGlobField("simpleType", () -> SubSecondType.TYPE);
            TYPE = globTypeBuilder.build();
        }
    }

    public static class SubFirstType {
        @Required_
        public static final GlobType TYPE;

        @KeyField_
        public static StringField NAME;

        public static StringField DATA;

        @Target(SubFirstType.class)
        public static GlobField PARENT;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("subFirst");
            globTypeBuilder.addAnnotation(Required.UNIQUE_GLOB);
            NAME = globTypeBuilder.declareStringField("name", KeyField.ZERO);
            DATA = globTypeBuilder.declareStringField("data");
            PARENT = globTypeBuilder.declareGlobField("parent", () -> SubFirstType.TYPE);
            TYPE = globTypeBuilder.build();
        }
    }

    public static class SubSecondType {
        @Required_
        public static final GlobType TYPE;

        public static IntegerField ID;

        public static DoubleField VALUE;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("subSecond");
            globTypeBuilder.addAnnotation(Required.UNIQUE_GLOB);
            ID = globTypeBuilder.declareIntegerField("id", KeyField.ZERO);
            VALUE = globTypeBuilder.declareDoubleField("value");
            TYPE = globTypeBuilder.build();
        }
    }


    @Test
    public void write() throws Exception {
        Gson gson = init();
        String json = gson.toJson(LocalType.TYPE);

        assertEquivalent("{\n" +
                         "  \"kind\": \"test local type\",\n" +
                         "  \"fields\": [\n" +
                         "    {\n" +
                         "      \"name\": \"id\",\n" +
                         "      \"type\": \"int\",\n" +
                         "      \"annotations\": [\n" +
                         "        {\n" +
                         "          \"_kind\": \"KeyField\",\n" +
                         "          \"index\": 0\n" +
                         "        }\n" +
                         "      ]\n" +
                         "    },\n" +
                         "    {\n" +
                         "      \"name\": \"secondType\",\n" +
                         "      \"type\": \"globUnion\",\n" +
                         "      \"kinds\": [\n" +
                         "        \"subFirst\",\n" +
                         "        \"subSecond\"\n" +
                         "      ]\n" +
                         "    },\n" +
                         "    {\n" +
                         "      \"name\": \"arrayOfUnions\",\n" +
                         "      \"type\": \"globUnionArray\",\n" +
                         "      \"kinds\": [\n" +
                         "        \"subFirst\",\n" +
                         "        \"subSecond\"\n" +
                         "      ]\n" +
                         "    },\n" +
                         "    {\n" +
                         "      \"name\": \"arrayOfType\",\n" +
                         "      \"type\": \"globArray\",\n" +
                         "      \"kind\": \"subSecond\"\n" +
                         "    },\n" +
                         "    {\n" +
                         "      \"name\": \"simpleType\",\n" +
                         "      \"type\": \"glob\",\n" +
                         "      \"kind\": \"subSecond\"\n" +
                         "    }\n" +
                         "  ],\n" +
                         "  \"annotations\": [\n" +
                         "    {\n" +
                         "      \"_kind\": \"Required\"\n" +
                         "    }\n" +
                         "  ]\n" +
                         "}", json);
    }

    private Gson init(GlobType... types) {
        GlobModel globTypes = new DefaultGlobModel(new DefaultGlobModel(AllCoreAnnotations.MODEL, types), LocalType.TYPE, SubFirstType.TYPE, SubSecondType.TYPE, IsJsonContent.TYPE);
        return GlobsGson.create(globTypes::getType);
    }

    @Test
    public void readWriteGlobType() throws Exception {
        Gson gson = init();
        String json = gson.toJson(LocalType.TYPE);
        GlobType type = gson.fromJson(json, GlobType.class);
        Assert.assertEquals(LocalType.TYPE.getName(), type.getName());
        Assert.assertEquals(LocalType.TYPE.getFieldCount(), type.getFieldCount());
        Assert.assertEquals(json, gson.toJson(type));
    }

    @Test
    public void readNewFormat() {
        Gson gson = init();
        String newFormat =
                "{\n" +
                "  \"kind\": \"test local type\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"id\",\n" +
                "      \"type\": \"int\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"KeyField\",\n" +
                "          \"index\": 0\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"secondType\",\n" +
                "      \"type\": \"globUnion\",\n" +
                "      \"kinds\": [\n" +
                "        \"subFirst\",\n" +
                "        \"subSecond\"\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"arrayOfUnions\",\n" +
                "      \"type\": \"globUnionArray\",\n" +
                "      \"kinds\": [\n" +
                "        \"subFirst\",\n" +
                "        \"subSecond\"\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"arrayOfType\",\n" +
                "      \"type\": \"globArray\",\n" +
                "      \"kind\": \"subSecond\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"simpleType\",\n" +
                "      \"type\": \"glob\",\n" +
                "      \"kind\": \"subSecond\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"annotations\": [\n" +
                "    {\n" +
                "      \"_kind\": \"Required\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        GlobType type = gson.fromJson(newFormat, GlobType.class);
        String json = gson.toJson(type);
        Assert.assertEquals(gson.toJson(LocalType.TYPE), json);
    }

    @Test
    public void readWriteGlob() throws Exception {
        MutableGlob instantiate = LocalType.TYPE.instantiate();
        MutableGlob glob = instantiate.set(LocalType.ID, 1)
                .set(LocalType.SECOND_TYPE, SubFirstType.TYPE.instantiate().set(SubFirstType.DATA, "subFirst Data")
                        .set(SubFirstType.PARENT, SubFirstType.TYPE.instantiate().set(SubFirstType.NAME, "subFirst second level")))
                .set(LocalType.ARRAY_OF_UNIONS, new Glob[]{SubFirstType.TYPE.instantiate()
                        .set(SubFirstType.DATA, "subFirst array Data"), SubSecondType.TYPE.instantiate()
                        .set(SubSecondType.ID, 0).set(SubSecondType.VALUE, 3.1415)})
                .set(LocalType.SIMPLE_TYPE, SubSecondType.TYPE.instantiate().set(SubSecondType.VALUE, 12.28).set(SubSecondType.ID, 2))
                .set(LocalType.ARRAY_OF_TYPE, new Glob[]{SubSecondType.TYPE.instantiate().set(SubSecondType.VALUE, 6.28).set(SubSecondType.ID, 1)});
        Gson gson = init();

        String toJson = gson.toJson(glob);
        GlobsGsonAdapterTest.assertEquivalent(A_GLOB, toJson);
        Glob glob1 = gson.fromJson(A_GLOB, Glob.class);
        Assert.assertEquals("subFirst Data", glob1.get(LocalType.SECOND_TYPE).get(SubFirstType.DATA));
        Glob glob2 = glob1.get(LocalType.SECOND_TYPE).get(SubFirstType.PARENT);
        Assert.assertNotNull(glob2);
        Assert.assertEquals("subFirst second level", glob2.get(SubFirstType.NAME));
        Assert.assertEquals("subFirst array Data", glob1.get(LocalType.ARRAY_OF_UNIONS)[0].get(SubFirstType.DATA));
        Assert.assertEquals(3.1415, glob1.get(LocalType.ARRAY_OF_UNIONS)[1].get(SubSecondType.VALUE), 0.001);

        Glob mutableGlob1 = gson.fromJson(A_GLOB, MutableGlob.class);
        Assert.assertEquals("subFirst Data", mutableGlob1.get(LocalType.SECOND_TYPE).get(SubFirstType.DATA));
    }
}
