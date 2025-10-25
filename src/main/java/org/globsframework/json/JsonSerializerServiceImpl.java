package org.globsframework.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.fields.Field;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.json.annottations.JsonValueAsField;
import org.globsframework.json.field.JsonFieldSerializerVisitor;

import java.io.IOException;
import java.util.HashMap;
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
        serializerMap.put(globType, new GlobJsonFieldSerializer(serializers));
        deSerializerMap.put(globType, new GlobJsonFieldDeSerializer(deSerializers));
        serializerHidSensibleDataMap.put(globType, new GlobJsonFieldSerializer(serializersHideSensibleData));
        JsonFieldSerializerVisitor jsonFieldSerializerVisitor = new JsonFieldSerializerVisitor(serializersHideSensibleData, serializers, deSerializers, this);
        for (Field field : globType.getFields()) {
            if (field.hasAnnotation(JsonValueAsField.UNIQUE_KEY)) {
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
                        }
                        else {
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
}
