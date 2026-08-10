package org.globsframework.json.field;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.fields.Field;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.GlobJson;
import org.globsframework.json.GlobJsonService;
import org.globsframework.json.annottations.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSerializerServiceImpl implements GlobJsonService {
    private final Map<GlobType, JsonFieldSerializer> serializerMap = new HashMap<>();
    private final Map<GlobType, JsonFieldSerializer> serializerHidSensibleDataMap = new HashMap<>();
    private final Map<GlobType, JsonFieldDeSerializer> deSerializerMap = new HashMap<>();

    @Override
    public GlobJson get(GlobType type) {
        return new GlobJsonImpl(type, getSerializer(type), getDeSerializer(type));
    }

    // synchronized to allow recursivity of GlobType
    public synchronized JsonFieldDeSerializer getDeSerializer(GlobType globType) {
        final JsonFieldDeSerializer jsonFieldDeSerializer = deSerializerMap.get(globType);
        if (jsonFieldDeSerializer != null) {
            return jsonFieldDeSerializer;
        }
        init(globType);
        return deSerializerMap.get(globType);
    }

    public synchronized JsonFieldSerializer getSerializer(GlobType globType) {
        final JsonFieldSerializer jsonFieldSerializer = serializerMap.get(globType);
        if (jsonFieldSerializer != null) {
            return jsonFieldSerializer;
        }
        init(globType);
        return serializerMap.get(globType);
    }

    public synchronized JsonFieldSerializer getSerializerHide(GlobType globType) {
        final JsonFieldSerializer jsonFieldSerializer = serializerHidSensibleDataMap.get(globType);
        if (jsonFieldSerializer != null) {
            return jsonFieldSerializer;
        }
        init(globType);
        return serializerHidSensibleDataMap.get(globType);
    }

    private void init(GlobType globType) {
        JsonFieldSerializer[] serializers = new JsonFieldSerializer[globType.getFieldCount()];
        JsonFieldSerializer[] serializersHideSensibleData = new JsonFieldSerializer[globType.getFieldCount()];
        JsonFieldDeSerializer[] deSerializers = new JsonFieldDeSerializer[globType.getFieldCount()];

        if (globType.hasAnnotation(JsonFlatten.UNIQUE_KEY)) {
            // hopefully no recursivity on this kind of object.
            final GlobArrayField<?> fieldTargetArray =
                    globType.getFieldWithAnnotation(JsonFlattenTargetArray.UNIQUE_KEY)
                            .asGlobArrayField();
            final GlobType targetType = fieldTargetArray.getTargetType();
            final StringField targetField = targetType.getFieldWithAnnotation(JsonFlattenAttribute.UNIQUE_KEY)
                    .asStringField();
            final Field simpleField = targetType.findFieldWithAnnotation(JsonFlattenTargetAttribute.UNIQUE_KEY);
            if (simpleField != null) {
                JsonFieldSerializer[] subSerializers = new JsonFieldSerializer[targetType.getFieldCount()];
                JsonFieldSerializer[] subSerializersHideSensibleData = new JsonFieldSerializer[targetType.getFieldCount()];
                JsonFieldDeSerializer[] subDeSerializers = new JsonFieldDeSerializer[targetType.getFieldCount()];
                JsonFieldSerializerVisitor jsonFieldSerializerVisitor =
                        new JsonFieldSerializerVisitor(subSerializersHideSensibleData, subSerializers, subDeSerializers, this);
                simpleField.safeAccept(jsonFieldSerializerVisitor);
                deSerializerMap.put(globType,
                        new GlobJsonWithFlattenFieldDeSerializer(deSerializers, fieldTargetArray, targetField,
                                subDeSerializers[simpleField.getIndex()]));

                serializerMap.put(globType,
                        new GlobJsonWithFlattenFieldSerializer(fieldTargetArray, targetField,
                                new JsonFieldSerializer() {
                                    @Override
                                    public void write(Glob data, JsonWriter jsonWriter) throws IOException {
                                        subSerializers[simpleField.getIndex()].write(data,
                                                new NoNameJsonWriter(jsonWriter));
                                    }
                                }));
            } else {
                final JsonFieldDeSerializer deSerializer = getDeSerializer(targetType);
                deSerializerMap.put(globType,
                        new GlobJsonWithFlattenFieldDeSerializer(deSerializers, fieldTargetArray, targetField,
                                new JsonFieldDeSerializer() {
                                    @Override
                                    public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
                                        jsonReader.beginObject();
                                        deSerializer.deserialize(jsonReader, mutableGlob);
                                        jsonReader.endObject();
                                    }
                                }));
                final JsonFieldSerializer serializer = getSerializer(targetType);
                serializerMap.put(globType,
                        new GlobJsonWithFlattenFieldSerializer(fieldTargetArray, targetField,
                                new JsonFieldSerializer() {
                                    @Override
                                    public void write(Glob data, JsonWriter jsonWriter) throws IOException {
                                        jsonWriter.beginObject();
                                        serializer.write(data, jsonWriter);
                                        jsonWriter.endObject();
                                    }
                                }));


            }
            serializerHidSensibleDataMap.put(globType, new GlobJsonFieldSerializer(serializersHideSensibleData));
        }
        else {
            serializerMap.put(globType, new GlobJsonFieldSerializer(serializers));
            deSerializerMap.put(globType, new GlobJsonFieldDeSerializer(deSerializers));
            serializerHidSensibleDataMap.put(globType, new GlobJsonFieldSerializer(serializersHideSensibleData));
        }
        JsonFieldSerializerVisitor jsonFieldSerializerVisitor = new JsonFieldSerializerVisitor(serializersHideSensibleData, serializers, deSerializers, this);
        for (Field field : globType.getFields()) {
            if (field.hasAnnotation(JsonValueAsField.UNIQUE_KEY) || field.hasAnnotation(JsonFlattenAttribute.UNIQUE_KEY)) {
                continue;
            }
            field.safeAccept(jsonFieldSerializerVisitor);
        }
        for (int i = 0; i < serializersHideSensibleData.length; i++) {
            if (serializersHideSensibleData[i] == null) {
                serializersHideSensibleData[i] = serializers[i];
            }
        }
    }

    private static class GlobJsonFieldSerializer implements JsonFieldSerializer {
        private final JsonFieldSerializer[] serializers;

        public GlobJsonFieldSerializer(JsonFieldSerializer[] serializers) {
            this.serializers = serializers;
        }

        @Override
        public void write(Glob data, JsonWriter jsonWriter) throws IOException {
            for (JsonFieldSerializer serializer : serializers) {
                if (serializer != null) {
                    serializer.write(data, jsonWriter);
                }
            }
        }
    }

    private static class GlobJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final JsonFieldDeSerializer[] deSerializers;

        public GlobJsonFieldDeSerializer(JsonFieldDeSerializer[] deSerializers) {
            this.deSerializers = deSerializers;
        }

        @Override
        public void deserialize(JsonReader in, MutableGlob mutableGlob) throws IOException {
            final GlobType type = mutableGlob.getType();
            while (in.hasNext() && in.peek() == JsonToken.NAME) {
                String name = in.nextName();
                Field field = type.findField(name);
                if (field != null) {
                    if (in.peek() != JsonToken.NULL) {
                        final JsonFieldDeSerializer deSerializer = deSerializers[field.getIndex()];
                        if (deSerializer != null) {
                            deSerializer.deserialize(in, mutableGlob);
                        } else {
                            in.skipValue();
                        }
                    } else {
                        in.skipValue();
                        mutableGlob.setValue(field, null);
                    }
                } else {
                    in.skipValue();
                }
            }
        }
    }

    private static class GlobJsonWithFlattenFieldSerializer implements JsonFieldSerializer {
        private final GlobArrayField<?> arrayField;
        private final StringField flattenFieldAttribute;
        private final JsonFieldSerializer flattenGlobSerializer;

        public GlobJsonWithFlattenFieldSerializer(GlobArrayField<?> arrayField, StringField flattenFieldAttribute,
                                                  JsonFieldSerializer flattenGlobSerializer) {
            this.arrayField = arrayField;
            this.flattenFieldAttribute = flattenFieldAttribute;
            this.flattenGlobSerializer = flattenGlobSerializer;
        }

        @Override
        public void write(Glob data, JsonWriter jsonWriter) throws IOException {
            final Glob[] globs = data.get(arrayField);
            if (globs != null) {
                for (Glob glob : globs) {
                    jsonWriter.name(glob.get(flattenFieldAttribute));
                    flattenGlobSerializer.write(glob, jsonWriter);
                }
            }
        }
    }

    private static class GlobJsonWithFlattenFieldDeSerializer implements JsonFieldDeSerializer {
        private final JsonFieldDeSerializer[] deSerializers;
        private final GlobType subType;
        private final GlobArrayField<?> arrayField;
        private final StringField flattenFieldAttribute;
        private final JsonFieldDeSerializer flattenGlobDeSerializer;

        public GlobJsonWithFlattenFieldDeSerializer(JsonFieldDeSerializer[] deSerializers,
                                                    GlobArrayField<?> arrayField, StringField flattenFieldAttribute,
                                                    JsonFieldDeSerializer flattenGlobDeSerializer) {
            this.deSerializers = deSerializers;
            this.arrayField = arrayField;
            subType = arrayField.getTargetType();
            this.flattenFieldAttribute = flattenFieldAttribute;
            this.flattenGlobDeSerializer = flattenGlobDeSerializer;
        }

        @Override
        public void deserialize(JsonReader in, MutableGlob mutableGlob) throws IOException {
            List<Glob> globs = new ArrayList<>();
            final GlobType type = mutableGlob.getType();
            while (in.hasNext() && in.peek() == JsonToken.NAME) {
                String name = in.nextName();
                Field field = type.findField(name);
                if (field != null) {
                    if (in.peek() != JsonToken.NULL) {
                        final JsonFieldDeSerializer deSerializer = deSerializers[field.getIndex()];
                        if (deSerializer != null) {
                            deSerializer.deserialize(in, mutableGlob);
                        } else {
                            in.skipValue();
                        }
                    } else {
                        in.skipValue();
                        mutableGlob.setValue(field, null);
                    }
                } else {
                    final MutableGlob subData = subType.instantiate();
                    subData.set(flattenFieldAttribute, name);
                    flattenGlobDeSerializer.deserialize(in, subData);
                    globs.add(subData);
                }
            }
            mutableGlob.set(arrayField, globs.toArray(Glob[]::new));
        }
    }

    public static class NoNameJsonWriter implements JsonWriter {
        private final JsonWriter jsonWriter;

        public NoNameJsonWriter(JsonWriter jsonWriter) {
            this.jsonWriter = jsonWriter;
        }

        @Override
        public void name(String name) throws IOException {
        }

        @Override
        public void beginObject() throws IOException {
            jsonWriter.beginObject();
        }

        @Override
        public void endObject() throws IOException {
            jsonWriter.endObject();
        }

        @Override
        public void nullValue() throws IOException {
            jsonWriter.nullValue();
        }

        @Override
        public void beginArray() throws IOException {
            jsonWriter.beginArray();
        }

        @Override
        public void endArray() throws IOException {
            jsonWriter.endArray();
        }

        @Override
        public void value(long value) throws IOException {
            jsonWriter.value(value);
        }

        @Override
        public void value(String value) throws IOException {
            jsonWriter.value(value);
        }

        @Override
        public void value(boolean value) throws IOException {
            jsonWriter.value(value);
        }

        @Override
        public void value(double value) throws IOException {
            jsonWriter.value(value);
        }

        @Override
        public void value(BigDecimal value) throws IOException {
            jsonWriter.value(value);
        }

        @Override
        public void jsonValue(String value) throws IOException {
            jsonWriter.jsonValue(value);
        }
    }
}
