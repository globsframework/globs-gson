package org.globsframework.json.field;

import org.globsframework.core.model.Glob;

import java.io.IOException;

public interface JsonFieldSerializer {

    void write(Glob data, JsonWriter jsonWriter) throws IOException;
}
