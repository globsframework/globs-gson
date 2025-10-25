package org.globsframework.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class GlobJsonImpl implements GlobJson {
    private final GlobType globType;
    public final JsonFieldSerializer jsonFieldSerializer;
    public final JsonFieldDeSerializer jsonFieldDeSerializer;

    public GlobJsonImpl(GlobType globType, JsonFieldSerializer jsonFieldSerializer, JsonFieldDeSerializer jsonFieldDeSerializer) {
        this.globType = globType;
        this.jsonFieldSerializer = jsonFieldSerializer;
        this.jsonFieldDeSerializer = jsonFieldDeSerializer;
    }

    @Override
    public void write(Glob data, Writer writer) throws IOException {
        if (data != null && data.getType() != globType) {
            throw new IllegalArgumentException("Invalid glob type, expected " + globType.getName() + " but got " + data.getType().getName());
        }
        JsonWriter jsonWriter = new JsonWriter(writer);
        if (data == null) {
            jsonWriter.nullValue();
        }else {
            jsonWriter.beginObject();
            jsonFieldSerializer.write(data, jsonWriter);
            jsonWriter.endObject();
            jsonWriter.close();
        }
    }

    @Override
    public void write(Glob data, Writer writer, boolean withKind) throws IOException {
        if (data != null && data.getType() != globType) {
            throw new IllegalArgumentException("Invalid glob type, expected " + globType.getName() + " but got " + data.getType().getName());
        }
        JsonWriter jsonWriter = new JsonWriter(writer);
        if (data == null) {
            jsonWriter.nullValue();
        }
        else {
            jsonWriter.beginObject();
            if (withKind) {
                jsonWriter.name(GlobsGson.KIND_NAME).value(data.getType().getName());
            }
            jsonFieldSerializer.write(data, jsonWriter);
            jsonWriter.endObject();
            jsonWriter.close();
        }
    }

    @Override
    public void writeArray(Glob[] data, Writer writer) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.beginArray();
        for (Glob d : data) {
            if (d != null && d.getType() != globType) {
                throw new IllegalArgumentException("Invalid glob type, expected " + globType.getName() + " but got " + d.getType().getName());
            }
            jsonFieldSerializer.write(d, jsonWriter);
        }
        jsonWriter.endArray();
        jsonWriter.close();
    }

    @Override
    public Glob read(Reader reader) throws IOException {
        final JsonReader jsonReader = new JsonReader(reader);
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        } else {
            jsonReader.beginObject();
            final MutableGlob instantiate = globType.instantiate();
            jsonFieldDeSerializer.deserialize(jsonReader, instantiate);
            jsonReader.endObject();
            return instantiate;
        }
    }

    @Override
    public Glob[] readArray(Reader reader) throws IOException {
        List<Glob> globs = new ArrayList<>();
        final JsonReader jsonReader = new JsonReader(reader);
        jsonReader.beginArray();
        while (jsonReader.peek() != JsonToken.END_ARRAY) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                globs.add(null);
            }else {
                final MutableGlob instantiate = globType.instantiate();
                jsonReader.beginObject();
                jsonFieldDeSerializer.deserialize(jsonReader, instantiate);
                jsonReader.endObject();
                globs.add(instantiate);
            }
        }
        return globs.toArray(Glob[]::new);
    }
}
