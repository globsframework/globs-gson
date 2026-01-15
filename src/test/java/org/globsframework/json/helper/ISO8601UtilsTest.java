package org.globsframework.json.helper;

import org.junit.Assert;
import org.junit.Test;

public class ISO8601UtilsTest {

    @Test
    public void testManyType() {
        Assert.assertEquals("2023-12-12T10:23:20Z", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20Z"), false, false, true));
        Assert.assertEquals("2023-12-12T10:23:20.111Z", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111Z"), true, false, true));
        Assert.assertEquals("2023-12-12T10:23:20.111222333Z", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111222333Z"), false, true, true));
        Assert.assertEquals("2023-12-12T10:23:20+01:00", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20+01:00"), false, false, true));
        Assert.assertEquals("2023-12-12T10:23:20.111+01:00", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111+01:00"), true, false, true));
        Assert.assertEquals("2023-12-12T10:23:20.111222333+01:00", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111222333+01:00"), false, true, true));
        Assert.assertEquals("2023-12-12T09:23:20+01:00", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20+02:00[Europe/Paris]"), false, false, false));
        Assert.assertEquals("2023-12-12T09:23:20+01:00[Europe/Paris]", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20+02:00[Europe/Paris]"), false, false, true));
        Assert.assertEquals("2023-12-12T09:23:20.111+01:00[Europe/Paris]", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111+02:00[Europe/Paris]"), true, false, true));
        Assert.assertEquals("2023-12-12T09:23:20.111222333+01:00[Europe/Paris]", ISO8601Utils.format(ISO8601Utils.parse("2023-11-12T10:23:20.111222333+02:00[Europe/Paris]"), false, true, true));
    }
}