package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.annotations.AllCoreAnnotations;
import org.globsframework.core.metamodel.fields.DoubleField;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobModel;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfReadWriteTest {

//    static {
//        System.setProperty("org.globsframework.builder", "org.globsframework.model.generator.object.GeneratorGlobFactoryService");
//        System.setProperty("globsframework.field.no.check", "true");
//        GlobFactoryService.Builder.reset();
//    }

    public static class PlainJavaObject {
        public PlainJavaObject(String str_1, String str_2, Integer anInt, Double aDouble) {
            this.str_1 = str_1;
            this.str_2 = str_2;
            this.anInt = anInt;
            this.aDouble = aDouble;
        }

        String str_1;
        String str_2;
        Integer anInt;
        Double aDouble;
    }

    @Test
    public void oldJson() {
        List<PlainJavaObject> collect = IntStream.range(0, 1000)
                .mapToObj(i ->
                        new PlainJavaObject("str_1_" + i, "str_2_" + i, i, (double) i))
                .collect(Collectors.toList());

        GsonBuilder gsonBuilder = new GsonBuilder();
        final Gson gson = gsonBuilder.create();
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        writePlain(collect, gson);
        String s = writePlain(collect, gson);
        readPlain(gson, s);
        readPlain(gson, s);
        readPlain(gson, s);
        readPlain(gson, s);
        readPlain(gson, s);
        readPlain(gson, s);
        readPlain(gson, s);
    }

    @Test
    public void perf() throws IOException {
        GlobTypeBuilder globTypeBuilder = DefaultGlobTypeBuilder.init("perf");
        StringField str_1 = globTypeBuilder.declareStringField("str_1");
        StringField str_2 = globTypeBuilder.declareStringField("str_2");
        IntegerField anInt = globTypeBuilder.declareIntegerField("anInt");
        DoubleField aDouble = globTypeBuilder.declareDoubleField("aDouble");
//        DateTimeField aDate = globTypeBuilder.declareDateTimeField("aDate", JsonDateTimeFormatType.TYPE.instantiate()
//                .set(JsonDateTimeFormatType.useFastIso8601, true));

        GlobType globType = globTypeBuilder.build();

        List<Glob> collect = IntStream.range(0, 1000)
                .mapToObj(i ->
                        globType.instantiate()
                                .set(str_1, "str_1_" + i)
                                .set(str_2, "str_2_" + i)
                                .set(anInt, i)
//                                .set(aDate, ZonedDateTime.now(Clock.systemUTC()).plusHours((long) (Math.random() * 10)))
                                .set(aDouble, i))
                .collect(Collectors.toList());
        DefaultGlobModel globTypes = new DefaultGlobModel(AllCoreAnnotations.MODEL, globType);
        Gson gson = GlobsGson.create(globTypes::getType);
        String s = "";
        final Glob[] array = collect.toArray(Glob[]::new);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        write(gson, array);
        s = write(gson, array);
        read(s, globType);
        read(s, globType);
        read(s, globType);
        read(s, globType);
        read(s, globType);
        read(s, globType);
        read(s, globType);
        read(s, globType);
    }

    private String write(Gson gson, Glob[] array) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        JsonWriter jsonWriter = new JsonWriter(new GSonUtils.StringWriterToBuilder(stringBuilder));
        long start = System.nanoTime();
        StringBuilder writer = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            writer = new StringBuilder();
            writer.append(GSonUtils.encode(array, false));
//            gson.toJson(collect, new GSonUtils.StringWriterToBuilder(writer));
        }
        long end = System.nanoTime();
        System.out.println("write " + (end - start) / 1000000. + "ms size : " + writer.length() + " s=" + writer.substring(0, 100));  // 1100ms puis 600ms
        return writer.toString();
    }

    private String writePlain(List<PlainJavaObject> collect, Gson gson) {
        long start = System.nanoTime();
        StringBuilder writer = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            writer = new StringBuilder();
            gson.toJson(collect, new GSonUtils.StringWriterToBuilder(writer));
        }
        long end = System.nanoTime();
        System.out.println("write " + (end - start) / 1000000. + "ms size : " + writer.length() + " s=" + writer.substring(0, 100));  // 1100ms puis 600ms
        return writer.toString();
    }

    private void read(String s, GlobType globType) {
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Glob[] globList = GSonUtils.decodeArray(s, globType);
            Assert.assertEquals(globList.length, 1000);
        }
        long end = System.nanoTime();
        System.out.println("read " + (end - start) / 1000000. + "ms => " + ((1000. * 1000.) / ((end - start) / 1000000.) * 1000.) + " objects/s");  // 600ms (1.7Millions par second)
    }

    private void readPlain(Gson gson, String s) {
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            List<Glob> globList = gson.fromJson(new GSonUtils.NoLockStringReader(s), new TypeToken<List<PlainJavaObject>>() {
            }.getType());
            Assert.assertEquals(globList.size(), 1000);
        }
        long end = System.nanoTime();
        System.out.println("read " + (end - start) / 1000000. + "ms => " + ((1000. * 1000.) / ((end - start) / 1000000.) * 1000.) + " objects/s");  // 600ms (1.7Millions par second)
    }

}
