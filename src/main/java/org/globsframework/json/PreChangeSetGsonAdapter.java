package org.globsframework.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeResolver;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.*;
import org.globsframework.core.model.delta.DefaultFixStateChangeSet;
import org.globsframework.core.model.delta.DeltaGlob;
import org.globsframework.core.model.delta.FixStateChangeSet;
import org.globsframework.core.utils.exceptions.ItemNotFound;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PreChangeSetGsonAdapter extends TypeAdapter<PreChangeSet> {
    private final GlobTypeResolver resolver;

    public PreChangeSetGsonAdapter(GlobTypeResolver resolver) {
        this.resolver = resolver;
    }

    public void write(JsonWriter out, PreChangeSet changeSet) throws IOException {
        throw new RuntimeException("A preChangedSet is not exportable , use changeSet");
    }

    public PreChangeSet read(JsonReader in) throws IOException {
        FixStateChangeSet changeSet = new DefaultFixStateChangeSet();
        JsonReaderVisitor jsonReaderVisitor = new JsonReaderVisitor();
        in.beginArray();

        while (in.peek() == JsonToken.BEGIN_OBJECT) {
            JsonElement elt = JsonParser.parseReader(in);
            JsonObject jsonObject = elt.getAsJsonObject();
            String state = jsonObject.get("state").getAsString();
            String kind = jsonObject.get("_kind").getAsString();
            GlobType globType = resolver.getType(kind);
            JsonObject key = jsonObject.get("key").getAsJsonObject();
            Key readKey = readKey(key, globType, globType.getKeyFields(), jsonReaderVisitor);
            switch (state) {
                case "create":
                    DeltaGlob valuesForCreate = changeSet.getForCreate(readKey);
                    readValues(jsonObject.getAsJsonObject("newValue"), globType, valuesForCreate::setValue, jsonReaderVisitor);
                    break;
                case "update":
                    DeltaGlob values = changeSet.getForUpdate(readKey);
                    readValues(jsonObject.getAsJsonObject("newValue"), globType, values::setValue, jsonReaderVisitor);
                    readValues(jsonObject.getAsJsonObject("oldValue"), globType, values::setPreviousValue, jsonReaderVisitor);
                    break;
                case "delete":
                    DeltaGlob newValues = changeSet.getForDelete(readKey);
                    readValues(jsonObject.getAsJsonObject("oldValue"), globType, newValues::setValue, jsonReaderVisitor);
                    break;
                default:
                    throw new RuntimeException("'" + state + "' not expected (create/delete/update)");
            }
        }
        in.endArray();
        return new PreChangeSet() {
            final Map<Key, Glob> local = new HashMap<>();

            public ChangeSet resolve(GlobAccessor globAccessor) {
                jsonReaderVisitor.functions.forEach(g -> g.apply(key -> {
                    Glob glob = local.get(key);
                    if (glob != null) {
                        return glob;
                    }
                    if (changeSet.isCreated(key)) {
                        MutableGlob instantiate = key.getGlobType().instantiate();
                        changeSet.getNewValues(key).safeApply(instantiate::setValue);
                        local.put(key, instantiate);
                        return instantiate;
                    } else {
                        return globAccessor.get(key);
                    }
                }));

                return changeSet;
            }
        };
    }

    Key readKey(JsonObject jsonObject, GlobType globType, Field[] fields, JsonReaderVisitor jsonReaderVisitor) {
        KeyBuilder keyBuilder = KeyBuilder.create(globType);
        for (Field field : fields) {
            JsonElement jsonElement = jsonObject.get(field.getName());
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                field.safeAccept(jsonReaderVisitor, jsonElement, keyBuilder);
            }
        }
        return keyBuilder.get();
    }

    void readValues(JsonObject jsonObject, GlobType globType, FieldValueSetter values, JsonReaderVisitor jsonReaderVisitor) {
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            Field field = globType.findField(entry.getKey());
            if (field == null) {
                String message = entry.getKey() + " not found in " + globType.getName() + " got " + Arrays.toString(globType.getFields());
                message += " from " + jsonObject;
                throw new RuntimeException(message);
            }
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                values.setValue(field, null);
            } else {
                field.safeAccept(jsonReaderVisitor, value, new AbstractFieldSetter() {
                    public FieldSetter setValue(Field field1, Object value) throws ItemNotFound {
                        values.setValue(field1, value);
                        return this;
                    }
                });
            }
        }
    }

    interface FieldValueSetter {
        void setValue(Field field, Object value);
    }

    static class JsonReaderVisitor extends GSonVisitor {

        List<Function<GlobAccessor, Void>> functions = new ArrayList<>();


        Glob readGlob(JsonObject jsonObject, GlobType globType) {
            throw new RuntimeException("Bug a glob should not be created in a changeSet");
//            Field[] keyFields = globType.getKeyFields();
//            KeyBuilder keyBuilder = KeyBuilder.create(globType);
//            if (keyFields.length != 0) {
//                for (Field keyField : keyFields) {
//                    keyField.safeVisit(this, jsonObject.get(keyField.getName()), keyBuilder);
//                }
//                return globAccessor.get(keyBuilder.get());
//            } else {
//                return readGlob(jsonObject, globType);
//            }
        }

        Key readKey(JsonObject jsonObject, GlobType globType) {
            Field[] keyFields = globType.getKeyFields();
            KeyBuilder keyBuilder = KeyBuilder.create(globType);
            if (keyFields.length != 0) {
                for (Field keyField : keyFields) {
                    keyField.safeAccept(this, jsonObject.get(keyField.getName()), keyBuilder);
                }
                return keyBuilder.get();
            } else {
                throw new RuntimeException("Only object with key are expected " + globType.getName());
            }
        }

        public void visitGlobArray(GlobArrayField<?> field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
            List<Key> keys = new ArrayList<>();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                Key key = readKey(jsonElement.getAsJsonObject(), field.getTargetType());
                keys.add(key);
            }
            functions.add(new Function<GlobAccessor, Void>() {
                public Void apply(GlobAccessor globAccessor) {
                    Glob[] values = new Glob[keys.size()];
                    int i = 0;
                    for (Key key : keys) {
                        values[i++] = globAccessor.get(key);
                    }
                    fieldSetter.set(field, values);
                    return null;
                }
            });
        }

        public void visitUnionGlob(GlobUnionField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
            for (GlobType type : field.getTargetTypes()) {
                JsonElement jsonElement = element.getAsJsonObject().get(type.getName());
                if (jsonElement != null) {
                    Key key = readKey(jsonElement.getAsJsonObject(), type);
                    functions.add(globAccessor -> {
                        fieldSetter.set(field, globAccessor.get(key));
                        return null;
                    });
                }
            }
        }

        public void visitUnionGlobArray(GlobArrayUnionField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
            List<Key> keys = new ArrayList<>();
            for (JsonElement arrayElements : element.getAsJsonArray()) {
                for (GlobType type : field.getTargetTypes()) {
                    JsonElement jsonElement = arrayElements.getAsJsonObject().get(type.getName());
                    if (jsonElement != null) {
                        Key key = readKey(jsonElement.getAsJsonObject(), type);
                        keys.add(key);
                    }
                }
            }
            functions.add(globAccessor -> {
                Glob[] values = new Glob[keys.size()];
                int i = 0;
                for (Key key : keys) {
                    values[i++] = globAccessor.get(key);
                }
                fieldSetter.set(field, values);
                return null;
            });
        }

        public void visitGlob(GlobField<?> field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
            Key key = readKey(element.getAsJsonObject(), field.getTargetType());
            functions.add(globAccessor -> {
                fieldSetter.set(field, globAccessor.get(key));
                return null;
            });
        }
    }
}


