package org.globsframework.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.FieldSetter;
import org.globsframework.core.model.Glob;
import org.globsframework.json.annottations.IsJsonContent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;

public abstract class GSonVisitor implements FieldVisitorWithTwoContext<JsonElement, FieldSetter<?>> {

    public void visitInteger(IntegerField integerField, JsonElement element, FieldSetter<?> fieldSetter) {
        fieldSetter.set(integerField, element.getAsInt());
    }

    public void visitIntegerArray(IntegerArrayField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        int[] value = new int[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = jsonElement.getAsInt();
        }
        fieldSetter.set(field, value);
    }

    public void visitDouble(DoubleField doubleField, JsonElement element, FieldSetter<?> fieldSetter) {
        fieldSetter.set(doubleField, element.getAsDouble());
    }

    public void visitDoubleArray(DoubleArrayField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        double[] value = new double[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = jsonElement.getAsDouble();
        }
        fieldSetter.set(field, value);
    }

    public void visitString(StringField stringField, JsonElement element, FieldSetter<?> fieldSetter) {
        if (stringField.hasAnnotation(IsJsonContent.UNIQUE_KEY)) {
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                fieldSetter.set(stringField, GlobGSonDeserializer.GSON.toJson(jsonObject));
            } else if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                fieldSetter.set(stringField, GlobGSonDeserializer.GSON.toJson(jsonArray));
            } else {
                fieldSetter.set(stringField, GlobGSonDeserializer.GSON.toJson(element.getAsJsonPrimitive()));
            }
        } else {
            fieldSetter.set(stringField, element.getAsString());
        }
    }

    public void visitStringArray(StringArrayField field, JsonElement arrayElements, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = arrayElements.getAsJsonArray();
        String[] value = new String[asJsonArray.size()];
        int i = 0;
        for (JsonElement element : asJsonArray) {
            if (field.hasAnnotation(IsJsonContent.UNIQUE_KEY)) {
                if (element.isJsonObject()) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject != null) {
                        value[i] = GlobGSonDeserializer.GSON.toJson(jsonObject);
                    }
                } else if (element.isJsonArray()) {
                    JsonArray jsonArray = element.getAsJsonArray();
                    if (jsonArray != null) {
                        value[i] = GlobGSonDeserializer.GSON.toJson(jsonArray);
                    }
                } else {
                    value[i] = GlobGSonDeserializer.GSON.toJson(element.getAsJsonPrimitive());
                }
            } else {
                value[i] = element.getAsString();
            }
            ++i;
        }
        fieldSetter.set(field, value);
    }

    public void visitBoolean(BooleanField booleanField, JsonElement element, FieldSetter<?> fieldSetter) {
        fieldSetter.set(booleanField, element.getAsBoolean());
    }

    public void visitBooleanArray(BooleanArrayField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        boolean[] value = new boolean[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = jsonElement.getAsBoolean();
        }
        fieldSetter.set(field, value);
    }

    public void visitBigDecimal(BigDecimalField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        fieldSetter.set(field, element.getAsBigDecimal());
    }

    public void visitBigDecimalArray(BigDecimalArrayField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        BigDecimal[] value = new BigDecimal[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = jsonElement.getAsBigDecimal();
        }
        fieldSetter.set(field, value);

    }

    public void visitLong(LongField longField, JsonElement element, FieldSetter<?> fieldSetter) {
        fieldSetter.set(longField, element.getAsLong());
    }

    public void visitLongArray(LongArrayField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        long[] value = new long[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = jsonElement.getAsLong();
        }
        fieldSetter.set(field, value);
    }

    public void visitDate(DateField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        fieldSetter.set(field, LocalDate.from(DateTimeFormatter.ISO_DATE.parse(element.getAsString())));
    }

    public void visitDateTime(DateTimeField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        fieldSetter.set(field, ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(element.getAsString())));
    }

    public void visitBytes(BytesField BytesField, JsonElement element, FieldSetter<?> fieldSetter) {
        fieldSetter.set(BytesField,
                Base64.getDecoder().decode(element.getAsString()));
    }

    public void visitGlob(GlobField<?> field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        fieldSetter.set(field, readGlob(element.getAsJsonObject(), field.getTargetType()));
    }

    public void visitGlobArray(GlobArrayField<?> field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        Glob[] value = new Glob[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = readGlob(jsonElement.getAsJsonObject(), field.getTargetType());
        }
        fieldSetter.set(field, value);
    }

    public void visitUnionGlob(GlobUnionField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonObject asJsonObject = element.getAsJsonObject();
        Collection<GlobType> types = field.getTargetTypes();
        for (GlobType type : types) {
            if (asJsonObject.has(type.getName())) {
                fieldSetter.set(field, readGlob(asJsonObject.getAsJsonObject(type.getName()), type));
                return;
            }
        }
        throw new RuntimeException("For " + field.getFullName() + " one of " + field.getTargetTypes() + " is expected");
    }

    public void visitUnionGlobArray(GlobArrayUnionField field, JsonElement element, FieldSetter<?> fieldSetter) throws Exception {
        JsonArray asJsonArray = element.getAsJsonArray();
        Glob[] value = new Glob[asJsonArray.size()];
        int i = 0;
        for (JsonElement jsonElement : asJsonArray) {
            value[i++] = readInnerGlob(field, jsonElement);
        }
        fieldSetter.set(field, value);
    }

    private Glob readInnerGlob(GlobArrayUnionField field, JsonElement element) {
        JsonObject asJsonObject = element.getAsJsonObject();
        Collection<GlobType> types = field.getTargetTypes();
        for (GlobType type : types) {
            if (asJsonObject.has(type.getName())) {
                return readGlob(asJsonObject.getAsJsonObject(type.getName()), type);
            }
        }
        throw new RuntimeException("For " + field.getFullName() + " one of " + field.getTargetTypes() +
                " is expected got : " + asJsonObject);
    }

    abstract Glob readGlob(JsonObject jsonObject, GlobType globType);
}
