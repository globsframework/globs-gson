package org.globsframework.json.field;

import java.io.IOException;
import java.math.BigDecimal;

interface JsonWriter {

    void name(String name) throws IOException;

    void beginObject() throws IOException;

    void endObject() throws IOException;

    void nullValue() throws IOException;

    void beginArray() throws IOException;

    void endArray() throws IOException;

    void value(long value) throws IOException;

    void value(String value) throws IOException;

    void value(boolean value) throws IOException;

    void value(double value) throws IOException;

    void value(BigDecimal value) throws IOException;

    void jsonValue(String value) throws IOException;
}
