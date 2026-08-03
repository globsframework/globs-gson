package org.globsframework.json;

import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.Glob;
import org.globsframework.json.annottations.IsJsonContent;
import org.globsframework.json.annottations.JsonAsObject;
import org.globsframework.json.annottations.JsonValueAsField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;

public class JsonFieldValueWithWriterVisitor implements FieldValueVisitorWithContext<JsonWriter> {
    public final static JsonFieldValueWithWriterVisitor INSTANCE = new JsonFieldValueWithWriterVisitor();

    public void visitInteger(IntegerField field, Integer value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitIntegerArray(IntegerArrayField field, int[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (int i : value) {
                jsonWriter.value(i);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDouble(DoubleField field, Double value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitDoubleArray(DoubleArrayField field, double[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (double v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBigDecimal(BigDecimalField field, BigDecimal value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitBigDecimalArray(BigDecimalArrayField field, BigDecimal[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (BigDecimal v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitString(StringField field, String value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (field.hasAnnotation(IsJsonContent.UNIQUE_KEY)) {
            jsonWriter.jsonValue(value);
        } else {
            jsonWriter.value(value);
        }
    }

    public void visitStringArray(StringArrayField field, String[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (String v : value) {
                if (field.hasAnnotation(IsJsonContent.UNIQUE_KEY)) {
                    jsonWriter.jsonValue(v);
                } else {
                    jsonWriter.value(v);
                }
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBoolean(BooleanField field, Boolean value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitBooleanArray(BooleanArrayField field, boolean[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (boolean v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitLong(LongField field, Long value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitLongArray(LongArrayField field, long[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (long v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDate(DateField field, LocalDate value, JsonWriter jsonWriter) throws Exception {
        DateTimeFormatter cachedDateTimeFormatter = GSonUtils.getCachedDateFormatter(field);
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(cachedDateTimeFormatter.format(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDateTime(DateTimeField field, ZonedDateTime value, JsonWriter jsonWriter) throws Exception {
        GSonUtils.FormaterForDateTime timeFormatter = GSonUtils.getCachedDateTimeFormatter(field);
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(timeFormatter.format(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBytes(BytesField field, byte[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(Base64.getEncoder().encodeToString(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitGlob(GlobField<?> field, Glob value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginObject();
            addGlobAttributes(value, jsonWriter);
            jsonWriter.endObject();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitGlobArray(GlobArrayField<?> field, Glob[] value, JsonWriter jsonWriter) throws Exception {
        if (field.hasAnnotation(JsonAsObject.UNIQUE_KEY)) {
            jsonWriter.name(field.getName());
            jsonWriter.beginObject();
            Field fieldValueToUseAsName = field.getTargetType().findFieldWithAnnotation(JsonValueAsField.UNIQUE_KEY);
            if (fieldValueToUseAsName == null) {
                throw new RuntimeException("A field with " + JsonValueAsField.TYPE.getName() + " annotation is expected after " +
                        JsonAsObject.TYPE.getName() + " for " + field.getFullName());
            }
            for (Glob glob : value) {
                if (glob != null) {
                    Object value1 = glob.getValue(fieldValueToUseAsName);
                    if (value1 == null) {
                        throw new RuntimeException("Value can not be null for a JsonValueAsField field " + fieldValueToUseAsName.getFullName());
                    }
                    jsonWriter.name(Objects.toString(value1));
                    jsonWriter.beginObject();

                    glob.safeAccept(new AbstractFieldValueVisitor<JsonWriter>() {
                        public void notManaged(Field field, Object value, JsonWriter jsonWriter) throws Exception {
                            if (field != fieldValueToUseAsName) {
                                field.acceptValue(JsonFieldValueWithWriterVisitor.this, value, jsonWriter);
                            }
                        }
                    }, jsonWriter);

                    jsonWriter.endObject();
                }
            }
            jsonWriter.endObject();
        } else {
            jsonWriter.name(field.getName());
            if (value != null) {
                jsonWriter.beginArray();
                for (Glob v : value) {
                    if (v != null) {
                        jsonWriter.beginObject();
                        addGlobAttributes(v, jsonWriter);
                        jsonWriter.endObject();
                    }
                    else {
                        jsonWriter.nullValue();
                    }
                }
                jsonWriter.endArray();
            } else {
                jsonWriter.nullValue();
            }
        }
    }

    public void addGlobAttributes(Glob v, JsonWriter jsonWriter) throws Exception {
        v.accept(this, jsonWriter);
    }

    public void visitUnionGlob(GlobUnionField field, Glob value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginObject();
            jsonWriter.name(value.getType().getName());
            jsonWriter.beginObject();
            addGlobAttributes(value, jsonWriter);
            jsonWriter.endObject();
            jsonWriter.endObject();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitUnionGlobArray(GlobArrayUnionField field, Glob[] value, JsonWriter jsonWriter) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (Glob v : value) {
                if (v != null) {
                    jsonWriter.beginObject();
                    jsonWriter.name(v.getType().getName());
                    jsonWriter.beginObject();
                    addGlobAttributes(v, jsonWriter);
                    jsonWriter.endObject();
                    jsonWriter.endObject();
                }
                else {
                    jsonWriter.nullValue();
                }
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }
}
