package org.globsframework.json.field;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.core.model.globaccessor.get.*;
import org.globsframework.core.model.globaccessor.set.*;
import org.globsframework.json.*;
import org.globsframework.json.annottations.IsJsonContent;
import org.globsframework.json.annottations.JsonAsObject;
import org.globsframework.json.annottations.JsonHideValue;
import org.globsframework.json.annottations.JsonValueAsField;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class JsonFieldSerializerVisitor implements FieldVisitor {
    private final JsonFieldSerializer[] serializersHideSensibleData;
    private final JsonFieldSerializer[] jsonFieldSerializers;
    private final JsonFieldDeSerializer[] jsonFieldDeSerializers;
    private final JsonSerializerServiceImpl jsonSerializerService;

    public JsonFieldSerializerVisitor(JsonFieldSerializer[] serializersHideSensibleData, JsonFieldSerializer[] jsonFieldSerializers, JsonFieldDeSerializer[] jsonFieldDeSerializers, JsonSerializerServiceImpl jsonSerializerService) {
        this.serializersHideSensibleData = serializersHideSensibleData;
        this.jsonFieldSerializers = jsonFieldSerializers;
        this.jsonFieldDeSerializers = jsonFieldDeSerializers;
        this.jsonSerializerService = jsonSerializerService;
    }

    public void visitInteger(IntegerField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetIntAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetIntAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new IntJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new IntJsonFieldDeSerializer(setAccessor);
    }

    private boolean specific(Field field) {
        final ToStringFieldJsonSerializer registered = field.getRegistered(ToStringFieldJsonSerializer.class);
        if (registered != null) {
            jsonFieldSerializers[field.getIndex()] = new JsonFieldSerializer() {
                @Override
                public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
                    if (data.isSet(field)) {
                        jsonWriter.name(field.getName());
                        jsonWriter.value(registered.serialize(data));
                    }
                }
            };
            jsonFieldDeSerializers[field.getIndex()] = new JsonFieldDeSerializer() {
                public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
                    registered.deserialize(jsonReader.nextString(), mutableGlob);
                }
            };
            return true;
        } else {
            return false;
        }
    }

    public void visitIntegerArray(IntegerArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetIntArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetIntArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new IntArrayJsonFieldSerializer(field, getAccessor);
        jsonFieldDeSerializers[field.getIndex()] = new IntArrayJsonFieldDeSerializer(setAccessor);
    }

    public void visitDouble(DoubleField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetDoubleAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetDoubleAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new DoubleJsonFieldSerializer(field, getAccessor);
        jsonFieldDeSerializers[field.getIndex()] = new DoubleJsonFieldDeSerializer(setAccessor);
    }

    public void visitDoubleArray(DoubleArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetDoubleArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetDoubleArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new DoubleArrayJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new DoubleArrayJsonFieldDeSerializer(setAccessor);
    }

    public void visitBigDecimal(BigDecimalField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetBigDecimalAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetBigDecimalAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new BigDecimalJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new BigDecimalJsonFieldDeSerializer(setAccessor);
    }


    public void visitBigDecimalArray(BigDecimalArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetBigDecimalArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetBigDecimalArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new BigDecimalArrayJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new BigDecimalArrayJsonFieldDeSerializer(setAccessor);
    }

    public void visitString(StringField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetStringAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetStringAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        if (field.hasAnnotation(JsonHideValue.UNIQUE_KEY)) {
            serializersHideSensibleData[field.getIndex()] = new HideStringJsonFieldSerializer(getAccessor, field);
        }
        final boolean isJsonContent = field.hasAnnotation(IsJsonContent.UNIQUE_KEY);
        if (isJsonContent) {
            jsonFieldSerializers[field.getIndex()] = new JsonStringJsonFieldSerializer(getAccessor, field);
            jsonFieldDeSerializers[field.getIndex()] = new JsonStringJsonFieldDeSerializer(field, setAccessor);
        } else {
            jsonFieldSerializers[field.getIndex()] = new StringJsonFieldSerializer(getAccessor, field);
            jsonFieldDeSerializers[field.getIndex()] = new StringJsonFieldDeSerializer(setAccessor);
        }
    }

    public void visitStringArray(StringArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetStringArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetStringArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        final boolean isJsonContent = field.hasAnnotation(IsJsonContent.UNIQUE_KEY);
        jsonFieldSerializers[field.getIndex()] = new StringArrayJsonFieldSerializer(getAccessor, field, isJsonContent);
        jsonFieldDeSerializers[field.getIndex()] = new StringArrayJsonFieldDeSerializer(isJsonContent, field, setAccessor);
    }

    public void visitBoolean(BooleanField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetBooleanAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetBooleanAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new BooleanJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new BooleanJsonFieldDeSerializer(setAccessor);
    }

    public void visitBooleanArray(BooleanArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetBooleanArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetBooleanArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new BooleanArrayJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new BooleanArrayJsonFieldDeSerializer(setAccessor);
    }

    public void visitLong(LongField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetLongAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetLongAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new LongJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new LongJsonFieldDeSerializer(setAccessor);
    }

    public void visitLongArray(LongArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetLongArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetLongArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new LongArrayJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new LongArrayJsonFieldDeSerializer(setAccessor);
    }

    public void visitDate(DateField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetDateAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetDateAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        DateTimeFormatter dateTimeFormatter = GSonUtils.getCachedDateFormatter(field);
        jsonFieldSerializers[field.getIndex()] = new DateJsonFieldSerializer(getAccessor, field, dateTimeFormatter);
        jsonFieldDeSerializers[field.getIndex()] = new DateJsonFieldDeSerializer(setAccessor, dateTimeFormatter);
    }

    public void visitDateTime(DateTimeField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetDateTimeAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetDateTimeAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        GSonUtils.FormaterForDateTime formattedDateTime = GSonUtils.getCachedDateTimeFormatter(field);
        jsonFieldSerializers[field.getIndex()] = new DataTimeJsonFieldSerializer(getAccessor, field, formattedDateTime);
        jsonFieldDeSerializers[field.getIndex()] = new DateTimeJsonFieldDeSerializer(setAccessor, formattedDateTime);
    }

    public void visitBytes(BytesField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetBytesAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetBytesAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new BytesJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new BytesJsonFieldDeSerializer(setAccessor);
    }

    public void visitGlob(GlobField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetGlobAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetGlobAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        final GlobType targetType = field.getTargetType();
        final JsonFieldSerializer jsonFieldSerializer = jsonSerializerService.getSerializer(targetType);
        jsonFieldSerializers[field.getIndex()] = new GlobJsonFieldSerializer(getAccessor, field, jsonFieldSerializer);
        final JsonFieldDeSerializer jsonFieldDeSerializer = jsonSerializerService.getDeSerializer(targetType);
        jsonFieldDeSerializers[field.getIndex()] = new GlobJsonFieldDeSerializer(targetType, jsonFieldDeSerializer, setAccessor);
    }

    public void visitGlobArray(GlobArrayField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetGlobArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetGlobArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        final GlobType targetType = field.getTargetType();
        final JsonFieldSerializer jsonFieldSerializer = jsonSerializerService.getSerializer(targetType);
        final boolean hasObject = field.hasAnnotation(JsonAsObject.UNIQUE_KEY);
        final JsonFieldDeSerializer jsonFieldDeSerializer = jsonSerializerService.getDeSerializer(targetType);
        if (!hasObject) {
            jsonFieldSerializers[field.getIndex()] = new GlobArrayJsonFieldSerializer(getAccessor, field, jsonFieldSerializer);
            jsonFieldDeSerializers[field.getIndex()] = new GlobArrayJsonFieldDeSerializer(targetType, jsonFieldDeSerializer, setAccessor);
        } else {
            Field fieldValueToUseAsName = field.getTargetType().findFieldWithAnnotation(JsonValueAsField.UNIQUE_KEY);
            if (fieldValueToUseAsName == null) {
                throw new RuntimeException("A field with " + JsonValueAsField.TYPE.getName() + " annotation is expected after " +
                                           JsonAsObject.TYPE.getName() + " for " + field.getFullName());
            }
            StringField typedFieldToUseAsName = fieldValueToUseAsName.asStringField();

            jsonFieldSerializers[field.getIndex()] = new NamedGlobArrayJsonFieldSerializer(getAccessor, field, typedFieldToUseAsName, fieldValueToUseAsName, jsonFieldSerializer);
            jsonFieldDeSerializers[field.getIndex()] = new NamedGlobArrayJsonFieldDeSerializer(targetType, typedFieldToUseAsName, jsonFieldDeSerializer, setAccessor);
        }
    }

    public void visitUnionGlob(GlobUnionField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetGlobAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetGlobAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new GlobUnionJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new GlobUnionJsonFieldDeSerializer(field, setAccessor);
    }

    public void visitUnionGlobArray(GlobArrayUnionField field) throws Exception {
        if (specific(field)) {
            return;
        }
        final GlobGetGlobArrayAccessor getAccessor = field.getGlobType().getGetAccessor(field);
        final GlobSetGlobArrayAccessor setAccessor = field.getGlobType().getSetAccessor(field);
        jsonFieldSerializers[field.getIndex()] = new GlobUnionArrayJsonFieldSerializer(getAccessor, field);
        jsonFieldDeSerializers[field.getIndex()] = new GlobUnionArrayJsonFieldDeSerializer(field, setAccessor);
    }

    private static class IntJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetIntAccessor getAccessor;
        private final IntegerField field;
        private final String name;

        public IntJsonFieldSerializer(GlobGetIntAccessor getAccessor, IntegerField field) {
            this.getAccessor = getAccessor;
            this.field = field;
            name = field.getName();
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Integer value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            } else {
                if (getAccessor.isSet(data)) {
                    jsonWriter.name(name);
                    jsonWriter.nullValue();
                }
            }
        }
    }

    private static class IntJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetIntAccessor setAccessor;

        public IntJsonFieldDeSerializer(GlobSetIntAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, jsonReader.nextInt());
        }
    }

    private static class IntArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final IntegerArrayField field;
        private final GlobGetIntArrayAccessor getAccessor;

        public IntArrayJsonFieldSerializer(IntegerArrayField field, GlobGetIntArrayAccessor getAccessor) {
            this.field = field;
            this.getAccessor = getAccessor;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final int[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (int i : value) {
                    jsonWriter.value(i);
                }
                jsonWriter.endArray();
            } else {
                if (getAccessor.isSet(data)) {
                    jsonWriter.name(field.getName());
                    jsonWriter.nullValue();
                }
            }

        }
    }

    private static class IntArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetIntArrayAccessor setAccessor;

        public IntArrayJsonFieldDeSerializer(GlobSetIntArrayAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            int[] values = new int[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextInt();
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, Arrays.copyOf(values, count));
        }
    }

    private static class DoubleJsonFieldSerializer implements JsonFieldSerializer {
        final String name;
        private final GlobGetDoubleAccessor getAccessor;

        public DoubleJsonFieldSerializer(DoubleField field, GlobGetDoubleAccessor getAccessor) {
            this.getAccessor = getAccessor;
            name = field.getName();
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Double value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(name);
                jsonWriter.value(value.doubleValue());
            } else {
                if (getAccessor.isSet(data)) {
                    jsonWriter.name(name);
                    jsonWriter.nullValue();
                }
            }
        }
    }

    private static class LongJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetLongAccessor getAccessor;
        private final LongField field;

        public LongJsonFieldSerializer(GlobGetLongAccessor getAccessor, LongField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Long value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value.longValue());
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class LongJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetLongAccessor setAccessor;

        public LongJsonFieldDeSerializer(GlobSetLongAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, jsonReader.nextLong());
        }
    }

    private static class BooleanArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetBooleanArrayAccessor setAccessor;

        public BooleanArrayJsonFieldDeSerializer(GlobSetBooleanArrayAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            boolean[] values = new boolean[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextBoolean();
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, Arrays.copyOf(values, count));
        }
    }

    private static class BooleanArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetBooleanArrayAccessor getAccessor;
        private final BooleanArrayField field;

        public BooleanArrayJsonFieldSerializer(GlobGetBooleanArrayAccessor getAccessor, BooleanArrayField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final boolean[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (boolean v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class BooleanJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetBooleanAccessor setAccessor;

        public BooleanJsonFieldDeSerializer(GlobSetBooleanAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, jsonReader.nextBoolean());
        }
    }

    private static class BooleanJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetBooleanAccessor getAccessor;
        private final BooleanField field;

        public BooleanJsonFieldSerializer(GlobGetBooleanAccessor getAccessor, BooleanField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Boolean value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value.booleanValue());
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class StringArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final boolean isJsonContent;
        private final StringArrayField field;
        private final GlobSetStringArrayAccessor setAccessor;

        public StringArrayJsonFieldDeSerializer(boolean isJsonContent, StringArrayField field, GlobSetStringArrayAccessor setAccessor) {
            this.isJsonContent = isJsonContent;
            this.field = field;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            String[] values = new String[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                if (isJsonContent) {
                    JsonElement parse = JsonParser.parseReader(jsonReader);
                    if (parse.isJsonArray()) {
                        values[count++] = GlobGSonDeserializer.GSON.toJson(parse.getAsJsonArray());
                    } else if (parse.isJsonObject()) {
                        values[count++] = GlobGSonDeserializer.GSON.toJson(parse.getAsJsonObject());
                    } else if (parse.isJsonPrimitive()) {
                        values[count++] = GlobGSonDeserializer.GSON.toJson(parse.getAsJsonPrimitive());
                    } else {
                        throw new RuntimeException(parse + " not managed in " + field.getFullName());
                    }
                } else {
                    values[count++] = jsonReader.nextString();
                }
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }
    }

    private static class StringArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetStringArrayAccessor getAccessor;
        private final StringArrayField field;
        private final boolean isJsonContent;

        public StringArrayJsonFieldSerializer(GlobGetStringArrayAccessor getAccessor, StringArrayField field, boolean isJsonContent) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.isJsonContent = isJsonContent;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final String[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (String v : value) {
                    if (isJsonContent) {
                        jsonWriter.jsonValue(v);
                    } else {
                        jsonWriter.value(v);
                    }
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class StringJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetStringAccessor setAccessor;

        public StringJsonFieldDeSerializer(GlobSetStringAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            final JsonToken jsonToken = jsonReader.peek();
            switch (jsonToken) {
                case STRING:
                case NUMBER:
                    setAccessor.set(mutableGlob, jsonReader.nextString());
                    break;
                case BOOLEAN:
                    setAccessor.set(mutableGlob, Boolean.toString(jsonReader.nextBoolean()));
                    break;
            }
        }
    }

    private static class JsonStringJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final StringField field;
        private final GlobSetStringAccessor setAccessor;

        public JsonStringJsonFieldDeSerializer(StringField field, GlobSetStringAccessor setAccessor) {
            this.field = field;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            JsonElement parse = JsonParser.parseReader(jsonReader);
            if (parse.isJsonArray()) {
                setAccessor.set(mutableGlob, GlobGSonDeserializer.GSON.toJson(parse.getAsJsonArray()));
            } else if (parse.isJsonObject()) {
                setAccessor.set(mutableGlob, GlobGSonDeserializer.GSON.toJson(parse.getAsJsonObject()));
            } else if (parse.isJsonPrimitive()) {
                setAccessor.set(mutableGlob,  GlobGSonDeserializer.GSON.toJson(parse.getAsJsonPrimitive()));
            } else {
                throw new RuntimeException(parse.toString() + " not managed in " + field.getFullName());
            }
        }
    }

    private static class JsonStringJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetStringAccessor getAccessor;
        private final StringField field;

        public JsonStringJsonFieldSerializer(GlobGetStringAccessor getAccessor, StringField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final String value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.jsonValue(value);
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class StringJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetStringAccessor getAccessor;
        private final StringField field;

        public StringJsonFieldSerializer(GlobGetStringAccessor getAccessor, StringField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final String value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class HideStringJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetStringAccessor getAccessor;
        private final StringField field;

        public HideStringJsonFieldSerializer(GlobGetStringAccessor getAccessor, StringField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final String value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value("*****");
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class NamedGlobArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobType targetType;
        private final StringField typedFieldToUseAsName;
        private final JsonFieldDeSerializer jsonFieldDeSerializer;
        private final GlobSetGlobArrayAccessor setAccessor;

        public NamedGlobArrayJsonFieldDeSerializer(GlobType targetType, StringField typedFieldToUseAsName, JsonFieldDeSerializer jsonFieldDeSerializer, GlobSetGlobArrayAccessor setAccessor) {
            this.targetType = targetType;
            this.typedFieldToUseAsName = typedFieldToUseAsName;
            this.jsonFieldDeSerializer = jsonFieldDeSerializer;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            List<Glob> objs = new ArrayList<>();
            jsonReader.beginObject();
            while (jsonReader.peek() != JsonToken.END_OBJECT) {
                MutableGlob newObj = targetType.instantiate();
                String value = jsonReader.nextName();
                newObj.set(typedFieldToUseAsName, value);
                jsonFieldDeSerializer.deserialize(jsonReader, newObj);
                jsonReader.beginObject();
                GlobGSonDeserializer.read(jsonReader, targetType, newObj);
                jsonReader.endObject();
                objs.add(newObj);
            }
            jsonReader.endObject();
            setAccessor.set(mutableGlob, objs.toArray(Glob[]::new));
        }
    }

    private static class NamedGlobArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetGlobArrayAccessor getAccessor;
        private final GlobArrayField field;
        private final StringField typedFieldToUseAsName;
        private final Field fieldValueToUseAsName;
        private final JsonFieldSerializer jsonFieldSerializer;

        public NamedGlobArrayJsonFieldSerializer(GlobGetGlobArrayAccessor getAccessor, GlobArrayField field, StringField typedFieldToUseAsName, Field fieldValueToUseAsName, JsonFieldSerializer jsonFieldSerializer) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.typedFieldToUseAsName = typedFieldToUseAsName;
            this.fieldValueToUseAsName = fieldValueToUseAsName;
            this.jsonFieldSerializer = jsonFieldSerializer;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Glob[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginObject();
                for (Glob glob : value) {
                    String fieldName = glob.get(typedFieldToUseAsName);
                    if (fieldName == null) {
                        throw new RuntimeException("Value can not be null for a JsonValueAsField field " + fieldValueToUseAsName.getFullName());
                    }
                    jsonWriter.name(fieldName);
                    jsonWriter.beginObject();
                    jsonFieldSerializer.write(glob, jsonWriter);
                    jsonWriter.endObject();
                }
                jsonWriter.endObject();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class GlobArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobType targetType;
        private final JsonFieldDeSerializer jsonFieldDeSerializer;
        private final GlobSetGlobArrayAccessor setAccessor;

        public GlobArrayJsonFieldDeSerializer(GlobType targetType, JsonFieldDeSerializer jsonFieldDeSerializer, GlobSetGlobArrayAccessor setAccessor) {
            this.targetType = targetType;
            this.jsonFieldDeSerializer = jsonFieldDeSerializer;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            List<Glob> objs = new ArrayList<>();
            jsonReader.beginArray();
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (jsonReader.peek() == JsonToken.NULL) {
                    objs.add(null);
                }
                else {
                    final MutableGlob instantiate = targetType.instantiate();
                    jsonReader.beginObject();
                    jsonFieldDeSerializer.deserialize(jsonReader, instantiate);
                    jsonReader.endObject();
                    objs.add(instantiate);
                }
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, objs.toArray(Glob[]::new));
        }
    }

    private static class GlobArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetGlobArrayAccessor getAccessor;
        private final GlobArrayField field;
        private final JsonFieldSerializer jsonFieldSerializer;

        public GlobArrayJsonFieldSerializer(GlobGetGlobArrayAccessor getAccessor, GlobArrayField field, JsonFieldSerializer jsonFieldSerializer) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.jsonFieldSerializer = jsonFieldSerializer;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Glob[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (Glob v : value) {
                    if (v == null) {
                        jsonWriter.nullValue();
                    }
                    else {
                        jsonWriter.beginObject();
                        jsonFieldSerializer.write(v, jsonWriter);
                        jsonWriter.endObject();
                    }
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class GlobJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobType targetType;
        private final JsonFieldDeSerializer jsonFieldDeSerializer;
        private final GlobSetGlobAccessor setAccessor;

        public GlobJsonFieldDeSerializer(GlobType targetType, JsonFieldDeSerializer jsonFieldDeSerializer, GlobSetGlobAccessor setAccessor) {
            this.targetType = targetType;
            this.jsonFieldDeSerializer = jsonFieldDeSerializer;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            final MutableGlob instantiate = targetType.instantiate();
            jsonReader.beginObject();
            jsonFieldDeSerializer.deserialize(jsonReader, instantiate);
            jsonReader.endObject();
            setAccessor.set(mutableGlob, instantiate);
        }
    }

    private static class GlobJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetGlobAccessor getAccessor;
        private final GlobField field;
        private final JsonFieldSerializer jsonFieldSerializer;

        public GlobJsonFieldSerializer(GlobGetGlobAccessor getAccessor, GlobField field, JsonFieldSerializer jsonFieldSerializer) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.jsonFieldSerializer = jsonFieldSerializer;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Glob value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginObject();
                jsonFieldSerializer.write(value, jsonWriter);
                jsonWriter.endObject();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class BytesJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetBytesAccessor setAccessor;

        public BytesJsonFieldDeSerializer(GlobSetBytesAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, Base64.getDecoder().decode(jsonReader.nextString()));
        }
    }

    private static class BytesJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetBytesAccessor getAccessor;
        private final BytesField field;

        public BytesJsonFieldSerializer(GlobGetBytesAccessor getAccessor, BytesField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final byte[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(Base64.getEncoder().encodeToString(value));
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class DataTimeJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetDateTimeAccessor getAccessor;
        private final DateTimeField field;
        private final GSonUtils.FormaterForDateTime formattedDateTime;

        public DataTimeJsonFieldSerializer(GlobGetDateTimeAccessor getAccessor, DateTimeField field, GSonUtils.FormaterForDateTime formattedDateTime) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.formattedDateTime = formattedDateTime;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final ZonedDateTime value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(formattedDateTime.format(value));
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class DateTimeJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetDateTimeAccessor setAccessor;
        private final GSonUtils.FormaterForDateTime formattedDateTime;

        public DateTimeJsonFieldDeSerializer(GlobSetDateTimeAccessor setAccessor, GSonUtils.FormaterForDateTime formattedDateTime) {
            this.setAccessor = setAccessor;
            this.formattedDateTime = formattedDateTime;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, formattedDateTime.parse(jsonReader.nextString()));
        }
    }

    private static class DateJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetDateAccessor setAccessor;
        private final DateTimeFormatter dateTimeFormatter;

        public DateJsonFieldDeSerializer(GlobSetDateAccessor setAccessor, DateTimeFormatter dateTimeFormatter) {
            this.setAccessor = setAccessor;
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, LocalDate.from(dateTimeFormatter.parse(jsonReader.nextString())));
        }
    }

    private static class DateJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetDateAccessor getAccessor;
        private final DateField field;
        private final DateTimeFormatter dateTimeFormatter;

        public DateJsonFieldSerializer(GlobGetDateAccessor getAccessor, DateField field, DateTimeFormatter dateTimeFormatter) {
            this.getAccessor = getAccessor;
            this.field = field;
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final LocalDate value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(dateTimeFormatter.format(value));
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class LongArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetLongArrayAccessor setAccessor;

        public LongArrayJsonFieldDeSerializer(GlobSetLongArrayAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            long[] values = new long[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextLong();
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, Arrays.copyOf(values, count));
        }
    }

    private static class LongArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetLongArrayAccessor getAccessor;
        private final LongArrayField field;

        public LongArrayJsonFieldSerializer(GlobGetLongArrayAccessor getAccessor, LongArrayField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final long[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (long v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class BigDecimalArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetBigDecimalArrayAccessor setAccessor;

        public BigDecimalArrayJsonFieldDeSerializer(GlobSetBigDecimalArrayAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            BigDecimal[] values = new BigDecimal[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = new BigDecimal(jsonReader.nextString());
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, Arrays.copyOf(values, count));
        }
    }

    private static class BigDecimalArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetBigDecimalArrayAccessor getAccessor;
        private final BigDecimalArrayField field;

        public BigDecimalArrayJsonFieldSerializer(GlobGetBigDecimalArrayAccessor getAccessor, BigDecimalArrayField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final BigDecimal[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (BigDecimal v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }

        }
    }

    private static class BigDecimalJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetBigDecimalAccessor getAccessor;
        private final BigDecimalField field;

        public BigDecimalJsonFieldSerializer(GlobGetBigDecimalAccessor getAccessor, BigDecimalField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final BigDecimal value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class BigDecimalJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetBigDecimalAccessor setAccessor;

        public BigDecimalJsonFieldDeSerializer(GlobSetBigDecimalAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, new BigDecimal(jsonReader.nextString()));
        }
    }

    private static class DoubleArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetDoubleArrayAccessor setAccessor;

        public DoubleArrayJsonFieldDeSerializer(GlobSetDoubleArrayAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            double[] values = new double[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextDouble();
            }
            jsonReader.endArray();
            setAccessor.set(mutableGlob, Arrays.copyOf(values, count));
        }
    }

    private static class DoubleArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetDoubleArrayAccessor getAccessor;
        private final DoubleArrayField field;

        public DoubleArrayJsonFieldSerializer(GlobGetDoubleArrayAccessor getAccessor, DoubleArrayField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final double[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (double v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private static class DoubleJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobSetDoubleAccessor setAccessor;

        public DoubleJsonFieldDeSerializer(GlobSetDoubleAccessor setAccessor) {
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            setAccessor.set(mutableGlob, jsonReader.nextDouble());
        }
    }

    private class GlobUnionArrayJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobArrayUnionField field;
        private final GlobSetGlobArrayAccessor setAccessor;

        public GlobUnionArrayJsonFieldDeSerializer(GlobArrayUnionField field, GlobSetGlobArrayAccessor setAccessor) {
            this.field = field;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginArray();
            List<Glob> objs = new ArrayList<>();
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                jsonReader.beginObject();
                String name = jsonReader.nextName();
                jsonReader.beginObject();
                final GlobType targetType = field.getTargetType(name);
                final MutableGlob instantiate = targetType.instantiate();
                final JsonFieldDeSerializer deSerializer = jsonSerializerService.getDeSerializer(targetType);
                deSerializer.deserialize(jsonReader, instantiate);
                objs.add(instantiate);
                jsonReader.endObject();
                jsonReader.endObject();
            }
            jsonReader.endArray();
            mutableGlob.set(field, objs.toArray(Glob[]::new));
        }
    }

    private class GlobUnionArrayJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetGlobArrayAccessor getAccessor;
        private final GlobArrayUnionField field;

        public GlobUnionArrayJsonFieldSerializer(GlobGetGlobArrayAccessor getAccessor, GlobArrayUnionField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, org.globsframework.json.field.JsonWriter jsonWriter) throws IOException {
            final Glob[] value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (Glob v : value) {
                    jsonWriter.beginObject();
                    jsonWriter.name(v.getType().getName());
                    jsonWriter.beginObject();
                    final JsonFieldSerializer serializer = jsonSerializerService.getSerializer(v.getType());
                    serializer.write(v, jsonWriter);
                    jsonWriter.endObject();
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }

    private class GlobUnionJsonFieldDeSerializer implements JsonFieldDeSerializer {
        private final GlobUnionField field;
        private final GlobSetGlobAccessor setAccessor;

        public GlobUnionJsonFieldDeSerializer(GlobUnionField field, GlobSetGlobAccessor setAccessor) {
            this.field = field;
            this.setAccessor = setAccessor;
        }

        @Override
        public void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException {
            jsonReader.beginObject();
            String name = jsonReader.nextName();
            if (jsonReader.peek() == JsonToken.NULL) {
                mutableGlob.set(field, null);
            }
            else {
                GlobType globType = field.getTargetType(name);
                final JsonFieldDeSerializer deSerializer = jsonSerializerService.getDeSerializer(globType);
                final MutableGlob ch = globType.instantiate();
                jsonReader.beginObject();
                deSerializer.deserialize(jsonReader, ch);
                jsonReader.endObject();
                mutableGlob.set(field, ch);
            }
            jsonReader.endObject();
        }
    }

    private class GlobUnionJsonFieldSerializer implements JsonFieldSerializer {
        private final GlobGetGlobAccessor getAccessor;
        private final GlobUnionField field;

        public GlobUnionJsonFieldSerializer(GlobGetGlobAccessor getAccessor, GlobUnionField field) {
            this.getAccessor = getAccessor;
            this.field = field;
        }

        @Override
        public void write(Glob data, JsonWriter jsonWriter) throws IOException {
            final Glob value = getAccessor.get(data);
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginObject();
                jsonWriter.name(value.getType().getName());
                jsonWriter.beginObject();
                jsonSerializerService.getSerializer(value.getType())
                        .write(value, jsonWriter);
                jsonWriter.endObject();
                jsonWriter.endObject();
            } else if (getAccessor.isSet(data)) {
                jsonWriter.name(field.getName());
                jsonWriter.nullValue();
            }
        }
    }
}
