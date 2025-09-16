package org.globsframework.json;

import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.json.annottations.JsonHideValue;

public class JsonFieldValueVisitorHideWithWriterSensitiveData extends JsonFieldValueWithWriterVisitor {
    public static final JsonFieldValueVisitorHideWithWriterSensitiveData INSTANCE = new JsonFieldValueVisitorHideWithWriterSensitiveData();

    @Override
    public void visitString(StringField field, String value, JsonWriter jsonWriter) throws Exception {
        if (field.hasAnnotation(JsonHideValue.UNIQUE_KEY)) {
            super.visitString(field, value != null ? "••••" : null, jsonWriter);
        } else {
            super.visitString(field, value, jsonWriter);
        }
    }
}
