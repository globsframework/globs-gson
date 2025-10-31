package org.globsframework.json.field;

import com.google.gson.stream.JsonReader;
import org.globsframework.core.model.MutableGlob;

import java.io.IOException;

public interface JsonFieldDeSerializer {

    void deserialize(JsonReader jsonReader, MutableGlob mutableGlob) throws IOException;

}
