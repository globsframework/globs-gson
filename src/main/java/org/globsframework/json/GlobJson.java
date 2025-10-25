package org.globsframework.json;

import org.globsframework.core.model.Glob;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public interface GlobJson {

    void write(Glob data, Writer writer) throws IOException;

    void write(Glob data, Writer writer, boolean withKind) throws IOException;

    void writeArray(Glob[] data, Writer writer) throws IOException;

    Glob read(Reader reader) throws IOException;

    Glob[] readArray(Reader reader) throws IOException;

}
