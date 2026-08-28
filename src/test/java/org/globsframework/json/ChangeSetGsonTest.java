package org.globsframework.json;

import com.google.gson.Gson;
import org.globsframework.core.metamodel.GlobModel;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.annotations.KeyField;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobArrayUnionField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.GlobUnionField;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobModel;
import org.globsframework.core.model.*;
import org.globsframework.core.model.delta.DefaultChangeSet;
import org.globsframework.core.model.delta.MutableChangeSet;
import org.globsframework.core.model.format.GlobPrinter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.function.Supplier;

public class ChangeSetGsonTest {

    @Test
    public void readWrite() {
        MutableChangeSet changeSet = DefaultChangeSet.createOrdered();
        Glob sub1 = SubType.TYPE.instantiate().set(SubType.SUB_NAME, "nSub1").set(SubType.UUID, "AAAA");
        changeSet.processCreation(sub1.getKey(), sub1);

        Glob subWOK2 = SubTypeWWithoutKey.TYPE.instantiate().set(SubTypeWWithoutKey.COUNT, 3).set(SubTypeWWithoutKey.UUID, "KKKK");
        changeSet.processCreation(subWOK2.getKey(), subWOK2);

        Glob d1 = DummyType.TYPE.instantiate().set(DummyType.NAME, "d1").set(DummyType.UUID, "XXXX")
                .set(DummyType.SUB_ELEMENT, sub1)
                .set(DummyType.COUNTS, new Glob[]{subWOK2});

        changeSet.processCreation(d1.getKey(), d1);

        Glob d2 = DummyType.TYPE.instantiate().set(DummyType.NAME, "d1").set(DummyType.UUID, "YYYYY");
        changeSet.processUpdate(d2.getKey(), DummyType.NAME, "d1", "d2");

        Glob d3_1 = DummyType.TYPE.instantiate().set(DummyType.UUID, "EERS");
        changeSet.processUpdate(d3_1.getKey(), DummyType.NAME, null, "a bad name");

        Glob d3 = DummyType.TYPE.instantiate().set(DummyType.UUID, "ZZZZ").set(DummyType.NAME, "d3");
        changeSet.processDeletion(d3.getKey(), d3);

        GlobModel globModel = new DefaultGlobModel(DummyType.TYPE, SubType.TYPE, SubTypeWWithoutKey.TYPE);

        Gson gson = GlobsGson.create(globModel::getType);
        String jsonChangeSet = gson.toJson(changeSet);
        GlobsGsonAdapterTest.assertEquivalent("[\n" +
                "  {\n" +
                "    \"state\": \"delete\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"ZZZZ\"\n" +
                "    },\n" +
                "    \"oldValue\": {\n" +
                "      \"name\": \"d3\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"update\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"YYYYY\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"name\": \"d1\"\n" +
                "    },\n" +
                "    \"oldValue\": {\n" +
                "      \"name\": \"d2\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"update\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"EERS\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"name\": null\n" +
                "    },\n" +
                "    \"oldValue\": {\n" +
                "      \"name\": \"a bad name\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"subType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"AAAA\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"subName\": \"nSub1\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"subTypeWWithoutKey\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"KKKK\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"count\": 3\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"XXXX\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"name\": \"d1\",\n" +
                "      \"subElement\": {\n" +
                "        \"uuid\": \"AAAA\"\n" +
                "      },\n" +
                "      \"counts\": [\n" +
                "        {\n" +
                "          \"uuid\": \"KKKK\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "]", jsonChangeSet);

        PreChangeSet preChangeSet = gson.fromJson(jsonChangeSet, PreChangeSet.class);

        ChangeSet actualChangeSet = preChangeSet.resolve(key -> {
//            if (key.equals(d2.getKey())) {
//                return d2;
//            }
            throw new RuntimeException("Unexpected key " + GlobPrinter.toString(key.asFieldValues()));
        });

        {
            Set<Key> created = actualChangeSet.getCreated(SubType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(sub1.getKey()));
        }
        {
            Set<Key> created = actualChangeSet.getCreated(DummyType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(d1.getKey()));
        }
        {
            Set<Key> created = actualChangeSet.getCreated(SubTypeWWithoutKey.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(subWOK2.getKey()));
        }
        {
            Set<Key> deleted = actualChangeSet.getDeleted(DummyType.TYPE);
            Assert.assertEquals(deleted.size(), 1);
            Assert.assertTrue(deleted.contains(d3.getKey()));
        }
        {
            Set<Key> updated = actualChangeSet.getUpdated(DummyType.TYPE);
            Assert.assertEquals(updated.size(), 2);
            Assert.assertTrue(updated.contains(d2.getKey()));
            Assert.assertTrue(updated.contains(d3_1.getKey()));
        }
    }

    @Test
    public void createAndCreateSub() {
        MutableChangeSet changeSet = DefaultChangeSet.createOrdered();
        Glob sub1 = SubType.TYPE.instantiate().set(SubType.SUB_NAME, "nSub1").set(SubType.UUID, "AAAA");
        changeSet.processCreation(sub1.getKey(), sub1);

        Glob master = DummyType.TYPE.instantiate().set(DummyType.UUID, "UUID_1").set(DummyType.SUB_ELEMENT, sub1);
        changeSet.processCreation(master.getKey(), master);
        GlobModel globModel = new DefaultGlobModel(DummyType.TYPE, SubType.TYPE, SubTypeWWithoutKey.TYPE);

        Gson gson = GlobsGson.create(globModel::getType);
        String jsonChangeSet = gson.toJson(changeSet);
        GlobsGsonAdapterTest.assertEquivalent("[\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"subType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"AAAA\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"subName\": \"nSub1\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"UUID_1\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"subElement\": {\n" +
                "        \"uuid\": \"AAAA\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "]", jsonChangeSet);

        PreChangeSet preChangeSet = gson.fromJson(jsonChangeSet, PreChangeSet.class);

        ChangeSet actualChangeSet = preChangeSet.resolve(key -> {
            throw new RuntimeException("Unexpected key " + GlobPrinter.toString(key.asFieldValues()));
        });

        {
            Set<Key> created = actualChangeSet.getCreated(DummyType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(master.getKey()));
            FieldValues newValues = actualChangeSet.getNewValues(created.iterator().next());
            Glob glob = newValues.get(DummyType.SUB_ELEMENT);
            Assert.assertNotNull(glob);
            Assert.assertEquals("AAAA", glob.get(SubType.UUID));
            Assert.assertEquals("nSub1", glob.get(SubType.SUB_NAME));
        }
        {
            Set<Key> created = actualChangeSet.getCreated(SubType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(sub1.getKey()));
        }
    }

    @Test
    public void createAndUpdate() {
        MutableChangeSet changeSet = DefaultChangeSet.createOrdered();
        Glob sub1 = SubType.TYPE.instantiate().set(SubType.SUB_NAME, "nSub1").set(SubType.UUID, "AAAA");
        changeSet.processCreation(sub1.getKey(), sub1);

        Glob master = DummyType.TYPE.instantiate().set(DummyType.UUID, "UUID_1");
        changeSet.processUpdate(master.getKey(), DummyType.SUB_ELEMENT, sub1, null);

        GlobModel globModel = new DefaultGlobModel(DummyType.TYPE, SubType.TYPE, SubTypeWWithoutKey.TYPE);

        Gson gson = GlobsGson.create(globModel::getType);
        String jsonChangeSet = gson.toJson(changeSet);
        GlobsGsonAdapterTest.assertEquivalent("[\n" +
                "  {\n" +
                "    \"state\": \"update\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"UUID_1\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"subElement\": {\n" +
                "        \"uuid\": \"AAAA\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"oldValue\": {}\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"create\",\n" +
                "    \"_kind\": \"subType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"AAAA\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"subName\": \"nSub1\"\n" +
                "    }\n" +
                "  }\n" +
                "]", jsonChangeSet);

        PreChangeSet preChangeSet = gson.fromJson(jsonChangeSet, PreChangeSet.class);
        ((MutableGlob) master).set(DummyType.SUB_ELEMENT, null);
        ChangeSet actualChangeSet = preChangeSet.resolve(key -> {
//            if (key.equals(master.getKey())) {
//                return master;
//            }
            throw new RuntimeException("Unexpected key " + GlobPrinter.toString(key.asFieldValues()));
        });

        {
            Set<Key> updated = actualChangeSet.getUpdated(DummyType.TYPE);
            Assert.assertEquals(updated.size(), 1);
            Assert.assertTrue(updated.contains(master.getKey()));
        }
        {
            Set<Key> created = actualChangeSet.getCreated(SubType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(sub1.getKey()));
        }
    }

    @Test
    public void testWithNull() {
        //language=JSON
        String json = "[\n" +
                "  {\n" +
                "    \"state\": \"update\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"UUID_1\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"name\": \"AAAA\"\n" +
                "    },\n" +
                "    \"oldValue\": {\n" +
                "      \"name\": null\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"state\": \"update\",\n" +
                "    \"_kind\": \"dummyType\",\n" +
                "    \"key\": {\n" +
                "      \"uuid\": \"UUID_2\"\n" +
                "    },\n" +
                "    \"newValue\": {\n" +
                "      \"name\": null\n" +
                "    },\n" +
                "    \"oldValue\": {\n" +
                "      \"name\": \"AAAA\"\n" +
                "    }\n" +
                "  }\n" +
                "]";

        Glob uuid1 = DummyType.TYPE.instantiate().set(DummyType.UUID, "UUID_1");
        Glob uuid2 = DummyType.TYPE.instantiate().set(DummyType.UUID, "UUID_2");
        GlobModel globModel = new DefaultGlobModel(DummyType.TYPE);
        Gson gson = GlobsGson.create(globModel::getType);
        PreChangeSet preChangeSet = gson.fromJson(json, PreChangeSet.class);
        ChangeSet changeSet = preChangeSet.resolve(key -> {
            Assert.fail();
            return null;
//            switch (key.get(DummyType.UUID)) {
//                case "UUID_1":
//                    return uuid1;
//                case "UUID_2":
//                    return uuid2;
//                default:
//                    return null;
//            }
        });
        Set<Key> updated = changeSet.getUpdated(DummyType.TYPE);
        Assert.assertTrue(updated.contains(uuid1.getKey()));
        Assert.assertTrue(updated.contains(uuid2.getKey()));
    }


    @Test
    public void Update() {

    }

    @Test
    public void createWithUnionAndUnionArrayFields() {
        MutableChangeSet changeSet = DefaultChangeSet.createOrdered();

        Glob subForUnion = SubType.TYPE.instantiate().set(SubType.SUB_NAME, "nUnion").set(SubType.UUID, "UNION_1");
        changeSet.processCreation(subForUnion.getKey(), subForUnion);

        Glob subForArrayUnion = SubType.TYPE.instantiate().set(SubType.SUB_NAME, "nArrayUnion").set(SubType.UUID, "ARRAY_UNION_1");
        changeSet.processCreation(subForArrayUnion.getKey(), subForArrayUnion);

        Glob master = DummyType.TYPE.instantiate().set(DummyType.UUID, "UUID_1")
                .set(DummyType.UNION_ELEMENT, subForUnion)
                .set(DummyType.ARRAY_UNION_ELEMENT, new Glob[]{subForArrayUnion});
        changeSet.processCreation(master.getKey(), master);

        GlobModel globModel = new DefaultGlobModel(DummyType.TYPE, SubType.TYPE, SubTypeWWithoutKey.TYPE);

        Gson gson = GlobsGson.create(globModel::getType);
        String jsonChangeSet = gson.toJson(changeSet);

        PreChangeSet preChangeSet = gson.fromJson(jsonChangeSet, PreChangeSet.class);

        ChangeSet actualChangeSet = preChangeSet.resolve(key -> {
            throw new RuntimeException("Unexpected key " + GlobPrinter.toString(key.asFieldValues()));
        });

        {
            Set<Key> created = actualChangeSet.getCreated(DummyType.TYPE);
            Assert.assertEquals(created.size(), 1);
            Assert.assertTrue(created.contains(master.getKey()));
            FieldValues newValues = actualChangeSet.getNewValues(created.iterator().next());

            Glob unionValue = newValues.get(DummyType.UNION_ELEMENT);
            Assert.assertNotNull(unionValue);
            Assert.assertEquals(subForUnion.getKey(), unionValue.getKey());

            Glob[] arrayUnionValue = newValues.get(DummyType.ARRAY_UNION_ELEMENT);
            Assert.assertNotNull(arrayUnionValue);
            Assert.assertEquals(1, arrayUnionValue.length);
            Assert.assertEquals(subForArrayUnion.getKey(), arrayUnionValue[0].getKey());
        }
    }

    public static class DummyType {
        public static GlobType TYPE;

        public static StringField UUID;

        public static StringField NAME;

        public static GlobField<SubType> SUB_ELEMENT;

        public static GlobArrayField<SubTypeWWithoutKey> COUNTS;

        public static GlobUnionField UNION_ELEMENT;

        public static GlobArrayUnionField ARRAY_UNION_ELEMENT;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("dummyType");
            UUID = globTypeBuilder.declareStringField("uuid", KeyField.ZERO);
            NAME = globTypeBuilder.declareStringField("name");
            SUB_ELEMENT = globTypeBuilder.declareGlobField("subElement", () -> SubType.TYPE);
            COUNTS = globTypeBuilder.declareGlobArrayField("counts", () -> SubTypeWWithoutKey.TYPE);
            UNION_ELEMENT = globTypeBuilder.declareGlobUnionField("unionElement", new Supplier[]{() -> SubType.TYPE});
            ARRAY_UNION_ELEMENT = globTypeBuilder.declareGlobUnionArrayField("arrayUnionElement", new Supplier[]{() -> SubType.TYPE});
            TYPE = globTypeBuilder.build();
        }
    }

    public static class SubType {
        public static final GlobType TYPE;

        public static final StringField UUID;

        public static final StringField SUB_NAME;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("subType");
            UUID = globTypeBuilder.declareStringField("uuid", KeyField.ZERO);
            SUB_NAME = globTypeBuilder.declareStringField("subName");
            TYPE = globTypeBuilder.build();
        }
    }

    //was without key
    public static class SubTypeWWithoutKey {
        public static GlobType TYPE;

        public static StringField UUID;

        public static IntegerField COUNT;

        static {
            GlobTypeBuilder globTypeBuilder = GlobTypeBuilderFactory.create("subTypeWWithoutKey");
            UUID = globTypeBuilder.declareStringField("uuid", KeyField.ZERO);
            COUNT = globTypeBuilder.declareIntegerField("count");
            TYPE = globTypeBuilder.build();
        }
    }
}
