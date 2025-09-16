package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.functional.FunctionalKey;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeResolver;
import org.globsframework.core.metamodel.fields.DateField;
import org.globsframework.core.metamodel.fields.DateTimeField;
import org.globsframework.core.metamodel.fields.Field;
import org.globsframework.core.model.FieldSetter;
import org.globsframework.core.model.FieldValues;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.utils.Strings;
import org.globsframework.json.annottations.JsonDateFormat;
import org.globsframework.json.annottations.JsonDateTimeFormat;
import org.globsframework.json.helper.ISO8601Utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GSonUtils {

    public static Map<String, DateTimeFormatter> CACHE_DATE = new ConcurrentHashMap<>();
    public static Map<Field, FormaterForDateTime> CACHE_DATE_TIME = new ConcurrentHashMap<>();

    public static Glob decode(String json, GlobType globType) {
        return decode(new NoLockStringReader(json), globType);
    }

    public static Glob decode(Reader reader, GlobType globType) {
        Glob glob = null;
        try {
            JsonReader in = new JsonReader(reader);
            in.beginObject();
            glob = GlobGSonDeserializer.readFields(in, globType);
            in.endObject();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return glob;
    }

    public static void decode(Reader reader, GlobType globType, FieldSetter fieldSetter) {
        try {
            JsonReader in = new JsonReader(reader);
            in.beginObject();
            GlobGSonDeserializer.read(in, globType, fieldSetter);
            in.endObject();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
    }

    public static Glob decode(String json, GlobTypeResolver resolver) {
        try {
            return GlobGSonDeserializer.read(new JsonReader(new NoLockStringReader(json)), resolver);
        } catch (IOException e) {
            throw new RuntimeException("Fail to read", e);
        }
    }

    public static Glob decode(Reader reader, GlobTypeResolver resolver) {
        try {
            return GlobGSonDeserializer.read(new JsonReader(reader), resolver);
        } catch (IOException e) {
            throw new RuntimeException("Fail to read", e);
        }
    }

    public static Glob[] decodeArray(String str, GlobType globType) {
        return decodeArray(new NoLockStringReader(str), globType);
    }

    public static Glob[] decodeArray(Reader reader, GlobType globType) {
        List<Glob> globs = new ArrayList<>();
        decodeArray(reader, globType, globs::add);
        return globs.toArray(new Glob[0]);
    }

    public static long decodeArray(String str, GlobType globType, Consumer<Glob> consumer) {
        return decodeArray(new NoLockStringReader(str), globType, consumer);
    }

    public static long decodeArray(Reader reader, GlobType globType, Consumer<Glob> consumer) {
        long count = 0;
        try {
            JsonReader in = new JsonReader(reader);
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                in.beginObject();
                Glob e = GlobGSonDeserializer.readFields(in, globType);
                consumer.accept(e);
                count++;
                in.endObject();
            }
            in.endArray();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return count;
    }

    public static Glob[] decodeArray(Reader reader, GlobTypeResolver resolver) {
        List<Glob> globs = new ArrayList<>();
        decodeArray(reader, resolver, globs::add);
        return globs.toArray(new Glob[0]);
    }

    public static long decodeArray(Reader reader, GlobTypeResolver resolver, Consumer<Glob> consumer) {
        long count = 0;
        try {
            JsonReader in = new JsonReader(reader);
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                Glob e = GlobGSonDeserializer.read(in, resolver);
                consumer.accept(e);
                count++;
            }
            in.endArray();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return count;
    }

    public static String encodeHidSensitiveData(Glob glob) {
        StringBuilder stringBuilder = new StringBuilder();
        Writer out = new StringWriterToBuilder(stringBuilder);
        encode(out, glob, true, false, true);
        return stringBuilder.toString();
    }

    public static String encodeWithKind(Glob glob) {
        return encode(glob, true);
    }

    public static String encodeWithoutKind(Glob glob) {
        return encode(glob, false);
    }

    public static String encode(Glob glob, boolean withKind) {
        StringBuilder stringBuilder = new StringBuilder();
        Writer out = new StringWriterToBuilder(stringBuilder);
        encode(out, glob, withKind);
        return stringBuilder.toString();
    }

    public static String encode(FunctionalKey functionalKey) {
        StringBuilder stringBuilder = new StringBuilder();
        Writer out = new StringWriterToBuilder(stringBuilder);
        encodeFieldValues(out, functionalKey);
        return stringBuilder.toString();
    }

    public static String encode(Key key, boolean withKind) {
        StringBuilder stringBuilder = new StringBuilder();
        Writer out = new StringWriterToBuilder(stringBuilder);
        encode(out, key, withKind, false);
        return stringBuilder.toString();
    }

    public static String niceEncode(Glob glob, boolean withKind) {
        StringBuilder stringBuilder = new StringBuilder();
        Writer out = new StringWriterToBuilder(stringBuilder);
        encode(out, glob, withKind, true, false);
        return stringBuilder.toString();
    }

    public static String encodeGlobType(GlobType globType) {
        if (globType == null) {
            return null;
        }
        GlobTypeSet globTypeSet = GlobTypeSet.export(globType);
        Gson gson = GlobsGson.create(name -> null);
        return gson.toJson(globTypeSet);
    }

    public static GlobType decodeGlobType(String json, GlobTypeResolver resolver, boolean ignore) {
        Gson gson = GlobsGson.createBuilder(resolver, ignore).create();
        GlobTypeSet globTypeSet = gson.fromJson(json, GlobTypeSet.class);
        return globTypeSet.globType[0];
    }

    public static Optional<GlobType> decodeOptGlobType(String json, GlobTypeResolver resolver, boolean ignore) {
        Gson gson = GlobsGson.createBuilder(resolver, ignore).create();
        GlobTypeSet globTypeSet = gson.fromJson(json, GlobTypeSet.class);
        return globTypeSet.globType.length == 0 ? Optional.empty() : Optional.of(globTypeSet.globType[0]);
    }

    public static void encode(Writer out, Glob glob, boolean withKind) {
        encode(out, glob, withKind, false, false);
    }

    public static void encode(StringBuilder out, Glob glob, boolean withKind) {
        encode(out, glob, withKind, false);
    }

    // pour éviter le sync de StringBuffer lorsqu'on utilise un StringWriter
    public static void encode(StringBuilder stringBuilder, Glob glob, boolean withKind, boolean nice) {
        encode(new StringWriterToBuilder(stringBuilder), glob, withKind, nice, false);
    }

    public static void encode(Writer out, Glob glob, boolean withKind, boolean nice, boolean hideSensitiveData) {
        try {
            JsonWriter jsonWriter = new JsonWriter(out);
            if (nice) {
                jsonWriter.setIndent(" ");
            }
            jsonWriter.beginObject();
            if (withKind) {
                jsonWriter.name(GlobsGson.KIND_NAME).value(glob.getType().getName());
            }
            JsonFieldValueWithWriterVisitor jsonFieldValueVisitor;
            if (hideSensitiveData) {
                jsonFieldValueVisitor = JsonFieldValueVisitorHideWithWriterSensitiveData.INSTANCE;
            } else {
                jsonFieldValueVisitor = JsonFieldValueWithWriterVisitor.INSTANCE;
            }
            glob.safeAccept(jsonFieldValueVisitor, jsonWriter);
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encodeFieldValues(Writer out, FieldValues fieldValues) {
        try {
            JsonWriter jsonWriter = new JsonWriter(out);
            jsonWriter.beginObject();;
            fieldValues.safeAccept(JsonFieldValueWithWriterVisitor.INSTANCE, jsonWriter);
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encode(Writer out, Key key, boolean withKind, boolean nice) {
        try {
            JsonWriter jsonWriter = new JsonWriter(out);
            if (nice) {
                jsonWriter.setIndent(" ");
            }
            jsonWriter.beginObject();
            if (withKind) {
                jsonWriter.name(GlobsGson.KIND_NAME).value(key.getGlobType().getName());
            }
            key.safeAcceptOnKeyField(JsonFieldValueWithWriterVisitor.INSTANCE, jsonWriter);
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DateTimeFormatter getCachedDateFormatter(DateField field) {
        DateTimeFormatter dateConverter;
        if (field.hasAnnotation(JsonDateFormat.UNIQUE_KEY)) {
            Glob annotation = field.getAnnotation(JsonDateFormat.UNIQUE_KEY);
            String pattern = annotation.get(JsonDateFormat.FORMAT);
            DateTimeFormatter dateTimeFormatter = CACHE_DATE.get(pattern);
            if (dateTimeFormatter == null) {
                dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
                CACHE_DATE.put(pattern, dateTimeFormatter);
            }
            dateConverter = dateTimeFormatter;
        } else {
            dateConverter = DateTimeFormatter.ISO_DATE;
        }
        return dateConverter;
    }

    public interface FormaterForDateTime {
        ZonedDateTime parse(String date);

        String format(ZonedDateTime date);
    }

    static class DefaultFormater implements FormaterForDateTime {
        private final DateTimeFormatter dateTimeFormatter;

        public DefaultFormater(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public ZonedDateTime parse(String date) {
            return ZonedDateTime.from(dateTimeFormatter.parse(date));
        }

        @Override
        public String format(ZonedDateTime date) {
            return dateTimeFormatter.format(date);
        }
    }

    static class DefaultLocalFormater implements FormaterForDateTime {
        private final DateTimeFormatter dateTimeFormatter;
        private final ZoneId zone;

        public DefaultLocalFormater(DateTimeFormatter dateTimeFormatter, ZoneId zone) {
            this.dateTimeFormatter = dateTimeFormatter;
            this.zone = zone;
        }

        @Override
        public ZonedDateTime parse(String date) {
            return ZonedDateTime.of(LocalDateTime.from(dateTimeFormatter.parse(date)), zone);
        }

        @Override
        public String format(ZonedDateTime date) {
            return dateTimeFormatter.format(date);
        }
    }

    static class IsoWithZoneFormater implements FormaterForDateTime {
        private final static FormaterForDateTime formaterForDateTime = new IsoWithZoneFormater();

        public ZonedDateTime parse(String date) {
            return ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(date));
        }

        public String format(ZonedDateTime date) {
            return DateTimeFormatter.ISO_DATE_TIME.format(date);
        }
    }

    static class IsoFastOffsetFormater implements FormaterForDateTime {
        private static IsoFastOffsetFormater formater = new IsoFastOffsetFormater();

        public ZonedDateTime parse(String date) {
            return ISO8601Utils.parse(date);
        }

        public String format(ZonedDateTime date) {
            return ISO8601Utils.format(date, false, true);
        }
    }

    static class IsoFastStrictOffsetFormater implements FormaterForDateTime {
        private static IsoFastStrictOffsetFormater formater = new IsoFastStrictOffsetFormater();

        public ZonedDateTime parse(String date) {
            return ISO8601Utils.parse(date);
        }

        public String format(ZonedDateTime date) {
            return ISO8601Utils.format(date, false, false);
        }
    }

    public static FormaterForDateTime getCachedDateTimeFormatter(DateTimeField field) {
        final FormaterForDateTime formaterForDateTime = CACHE_DATE_TIME.get(field);
        if (formaterForDateTime != null) {
            return formaterForDateTime;
        }
        FormaterForDateTime dateConverter;
        Glob annotation = field.findAnnotation(JsonDateTimeFormat.UNIQUE_KEY);
        if (annotation != null) {
            if (annotation.isTrue(JsonDateTimeFormat.useFastIso8601)) {
                dateConverter = IsoFastOffsetFormater.formater;
            } else if (annotation.isTrue(JsonDateTimeFormat.strictIso8601)) {
                dateConverter = IsoFastStrictOffsetFormater.formater;
            } else {
                String pattern = annotation.get(JsonDateTimeFormat.format);
                if (Strings.isNotEmpty(pattern)) {
                    final boolean forceLocal = annotation.isTrue(JsonDateTimeFormat.useLocalZone);
                    if (forceLocal) {
                        dateConverter = new DefaultLocalFormater(DateTimeFormatter.ofPattern(pattern), ZoneId.systemDefault());
                    } else {
                        dateConverter = new DefaultFormater(DateTimeFormatter.ofPattern(pattern));
                    }
                } else {
                    dateConverter = IsoWithZoneFormater.formaterForDateTime;
                }
            }
            final String s = annotation.get(JsonDateTimeFormat.nullValue);
            if (Strings.isNotEmpty(s)) {
                dateConverter = new FilterNullFormater(s, dateConverter);
            }
        } else {
            dateConverter = IsoWithZoneFormater.formaterForDateTime;
        }
        CACHE_DATE_TIME.put(field, dateConverter);
        return dateConverter;
    }

    public static String encodeHidSensitiveData(Glob[] glob) {
        return encode(glob, true, true);
    }

    public static String encodeWithKind(Glob[] glob) {
        return encode(glob, true, false);
    }

    public static String encodeWithoutKind(Glob[] glob) {
        return encode(glob, false, false);
    }

    public static String encode(Glob[] glob, boolean withKind) {
        return encode(glob, withKind, false);
    }

    private static String encode(Glob[] glob, boolean withKind, boolean hideSensitiveData) {
        StringBuilder stringBuilder = new StringBuilder();
        StringWriterToBuilder out = new StringWriterToBuilder(stringBuilder);
        encode(out, glob, withKind, hideSensitiveData);
        return stringBuilder.toString();
    }

    public static void encode(Writer writer, Glob[] glob) {
        encode(writer, glob, false, false);
    }

    public static void encode(Writer writer, Glob[] glob, boolean withKind) {
        encode(writer, glob, withKind, false);
    }

    public static void encode(Writer writer, Glob[] glob, boolean withKind, boolean hideSensitiveData) {
        try {
            JsonWriter jsonWriter = new JsonWriter(writer);

            JsonFieldValueWithWriterVisitor jsonFieldValueVisitor;
            if (hideSensitiveData) {
                jsonFieldValueVisitor = JsonFieldValueVisitorHideWithWriterSensitiveData.INSTANCE;
            } else {
                jsonFieldValueVisitor = JsonFieldValueWithWriterVisitor.INSTANCE;
            }

            jsonWriter.beginArray();
            for (Glob v : glob) {
                jsonWriter.beginObject();
                if (withKind) {
                    jsonWriter.name(GlobsGson.KIND_NAME).value(v.getType().getName());
                }
                v.safeAccept(jsonFieldValueVisitor, jsonWriter);
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
        } catch (IOException e) {
            throw new RuntimeException("In encode", e);
        }
    }

    public static String normalize(String json) {
        Gson gson = new Gson();
        return gson.toJson(JsonParser.parseReader(new NoLockStringReader(json)));
    }

    public static class WriteGlob {
        private final Writer writer;
        private final JsonWriter jsonWriter;
        private boolean withKind;

        public WriteGlob(Writer writer, boolean withKind) {
            this.writer = writer;
            jsonWriter = new JsonWriter(writer);
            this.withKind = withKind;
            try {
                jsonWriter.beginArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void push(Glob glob) {
            try {
                jsonWriter.beginObject();
                if (withKind) {
                    jsonWriter.name(GlobsGson.KIND_NAME).value(glob.getType().getName());
                }
                glob.safeAccept(JsonFieldValueWithWriterVisitor.INSTANCE, jsonWriter);
                jsonWriter.endObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void end() {
            try {
                jsonWriter.endArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class StringWriterToBuilder extends Writer {
        private final StringBuilder stringBuilder;

        public StringWriterToBuilder(StringBuilder stringBuilder) {
            this.stringBuilder = stringBuilder;
        }

        public void write(int c) throws IOException {
            stringBuilder.append(((char) c));
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            stringBuilder.append(cbuf, off, len);
        }

        public void write(char[] cbuf) throws IOException {
            stringBuilder.append(cbuf);
        }

        public void write(String str) throws IOException {
            stringBuilder.append(str);
        }

        public void write(String str, int off, int len) throws IOException {
            stringBuilder.append(str, off, off + len);
        }

        public void flush() throws IOException {

        }

        public void close() throws IOException {

        }

    }

    public static class NoLockStringReader extends Reader {
        private final String str;
        private final int length;
        private int next = 0;
        private int mark = 0;

        public NoLockStringReader(String s) {
            if (s == null) {
                throw new NullPointerException();
            }
            this.str = s;
            this.length = s.length();
        }


        public int read() throws IOException {
            if (next >= length)
                return -1;
            return str.charAt(next++);
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                    ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (next >= length)
                return -1;
            int n = Math.min(length - next, len);
            str.getChars(next, next + n, cbuf, off);
            next += n;
            return n;
        }

        public long skip(long ns) throws IOException {
            if (next >= length)
                return 0;
            // Bound skip by beginning and end of the source
            long n = Math.min(length - next, ns);
            n = Math.max(-next, n);
            next += n;
            return n;
        }

        public boolean ready() throws IOException {
            return true;
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readAheadLimit) throws IOException {
            if (readAheadLimit < 0) {
                throw new IllegalArgumentException("Read-ahead limit < 0");
            }
            mark = next;
        }

        public void reset() throws IOException {
            next = mark;
        }

        public void close() {
        }
    }

    private static class FilterNullFormater implements FormaterForDateTime {
        private final String dateToIgnore;
        private final FormaterForDateTime dateConverter;

        public FilterNullFormater(String DateToIgnore, FormaterForDateTime dateConverter) {
            dateToIgnore = DateToIgnore;
            this.dateConverter = dateConverter;
        }

        @Override
        public ZonedDateTime parse(String date) {
            return dateToIgnore.equalsIgnoreCase(date) ? null : dateConverter.parse(date);
        }

        @Override
        public String format(ZonedDateTime date) {
            return dateConverter.format(date);
        }
    }
}
