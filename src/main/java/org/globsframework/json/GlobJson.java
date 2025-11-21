package org.globsframework.json;

import org.globsframework.core.model.Glob;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

public interface GlobJson {

    void write(Glob data, Writer writer) throws IOException;

    // kind is added only on the first level.
    void write(Glob data, Writer writer, boolean withKind) throws IOException;

    void write(Collection<Glob> data, Writer writer) throws IOException;

    void write(Collection<Glob> data, Writer writer, boolean withKind) throws IOException;

    default void write(Glob[] data, Writer writer) throws IOException{
        write(Arrays.asList(data), writer);
    }

    default void write(Glob[] data, Writer writer, boolean withKind) throws IOException{
        write(Arrays.asList(data), writer, withKind);
    }

    Glob read(Reader reader) throws IOException;

    Glob[] readArray(Reader reader) throws IOException;

}
