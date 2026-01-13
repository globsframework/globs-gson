package org.globsframework.json;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GlobTypeSetAdapter extends TypeAdapter<GlobTypeSet> {
    GlobTypeArrayGsonAdapter globTypeArrayGsonAdapter;
    private boolean forceSort;
    private GlobTypeResolver globTypeResolver;
    private boolean ignoreUnknownAnnotation;

    public GlobTypeSetAdapter(boolean forceSort, GlobTypeResolver globTypeResolver, boolean ignoreUnknownAnnotation) {
        this.forceSort = forceSort;
        this.globTypeResolver = globTypeResolver;
        this.ignoreUnknownAnnotation = ignoreUnknownAnnotation;
        globTypeArrayGsonAdapter = new GlobTypeArrayGsonAdapter(forceSort, globTypeResolver, ignoreUnknownAnnotation);
    }

    public void write(JsonWriter out, GlobTypeSet value) throws IOException {
        out.beginArray();
        for (GlobType globType : value.globType) {
            globTypeArrayGsonAdapter.write(out, globType);
        }
        out.endArray();
    }

    public GlobTypeSet read(JsonReader in) throws IOException {
        JsonElement root = JsonParser.parseReader(in);
        if (root == null || root == JsonNull.INSTANCE) {
            return new GlobTypeSet(new GlobType[0]);
        }
        if (!root.isJsonArray()) {
            throw new RuntimeException("array expected got " + root.toString());
        }
        JsonArray asJsonArray = root.getAsJsonArray();
        Map<String, TypeWithJson> typesToRead = new LinkedHashMap<>();
        for (JsonElement jsonElement : asJsonArray) {
            JsonObject jsonType = jsonElement.getAsJsonObject();
            JsonElement jsonKind = jsonType.get(GlobsGson.TYPE_NAME);
            if (jsonKind == null) {
                throw new RuntimeException("Bug missing " + GlobsGson.TYPE_NAME + " attribute in " + asJsonArray.toString());
            }
            String kind = jsonKind.getAsString();
            typesToRead.put(kind, new TypeWithJson(new ReadGlobType(), jsonType));
        }
        GlobTypeGsonDeserializer globTypeGsonDeserializer = new GlobTypeGsonDeserializer(new GlobGSonDeserializer(), name -> {
            final GlobType type = globTypeResolver.findType(name);
            if (type != null) {
                return () -> type;
            }
            final TypeWithJson typeWithJson = typesToRead.get(name);
            return typeWithJson == null ? null : typeWithJson.readGlobType;
        }, ignoreUnknownAnnotation);

        GlobType[] readed = new GlobType[typesToRead.size()];
        int i = 0;
        for (TypeWithJson value : typesToRead.values()) {
            value.readGlobType().init(globTypeGsonDeserializer, value.jsonType);
            readed[i++] = value.readGlobType().globType;
        }
        return new GlobTypeSet(readed);
    }

    record TypeWithJson(ReadGlobType readGlobType, JsonObject jsonType) {

    }

    static class ReadGlobType implements Supplier<GlobType> {
        GlobType globType;
        @Override
        public GlobType get() {
            return globType;
        }

        void init(GlobTypeGsonDeserializer globTypeGsonDeserializer, JsonObject jsonType) {
            globType = globTypeGsonDeserializer.deserialize(jsonType);
        }
    }

}
