package org.globsframework.json.helper;

import java.time.*;
/*
code from
 https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java
 */


/**
 * Utilities methods for manipulating dates in iso8601 format. This is much much faster and GC friendly than
 * using SimpleDateFormat so highly suitable if you (un)serialize lots of date objects.
 */
public class ISO8601Utils {


    /**
     * Format date into yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     *
     * @param date   the date to format
     * @param millis true to include millis precision otherwise false
     * @param nano   true to include nano precision otherwise false (used if millis is also set)
     * @return the date formatted as yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     */
    public static String format(ZonedDateTime date, boolean millis, boolean nano, boolean withZone) {

        StringBuilder formatted = new StringBuilder(40);

        padInt(formatted, date.getYear(), "yyyy".length());
        formatted.append('-');
        padInt(formatted, date.getMonthValue() + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, date.getDayOfMonth(), "dd".length());
        formatted.append('T');
        padInt(formatted, date.getHour(), "hh".length());
        formatted.append(':');
        padInt(formatted, date.getMinute(), "mm".length());
        formatted.append(':');
        padInt(formatted, date.getSecond(), "ss".length());
        if (nano) {
            formatted.append('.');
            padInt(formatted, date.getNano(), "nnnnnnnnn".length());
        } else if (millis) {
            formatted.append('.');
            padInt(formatted, date.getNano() / 1_000_000, "sss".length());
        }

        final ZoneOffset offset = date.getOffset();
        formatted.append(offset.getId());
        if (withZone) {
            if (offset != date.getZone()) {
                formatted.append("[")
                        .append(date.getZone().getId())
                        .append("]");
            }
        }

        return formatted.toString();
    }

    /*
    /**********************************************************
    /* Parsing
    /**********************************************************
     */

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     *
     * @param date ISO string to parse in the appropriate format.
     * @return the parsed date
     * @throws IllegalArgumentException if the date is not in the appropriate format
     */
    public static ZonedDateTime parse(String date) {
        int offset = 0;

        // extract year
        int year = parseInt(date, offset, offset += 4);
        checkOffset(date, offset, '-');

        // extract month
        int month = parseInt(date, offset += 1, offset += 2);
        checkOffset(date, offset, '-');

        // extract day
        int day = parseInt(date, offset += 1, offset += 2);
        checkOffset(date, offset, 'T');

        // extract hours, minutes, seconds and milliseconds
        int hour = parseInt(date, offset += 1, offset += 2);
        checkOffset(date, offset, ':');

        int minutes = parseInt(date, offset += 1, offset += 2);
        checkOffset(date, offset, ':');

        int seconds = parseInt(date, offset += 1, offset += 2);
        // milliseconds can be optional in the format
        int nano = 0; // always use 0 otherwise returned date will include millis of current time
        if (date.charAt(offset) == '.') {
            final IntOffset intOffset = parseAllInt(date, offset += 1, date.length());
            if (intOffset.len == 3) {
                nano = intOffset.value * 1_000_000;
            } else if (intOffset.len == 9) {
                nano = intOffset.value;
            } else {
                throw new NumberFormatException("Expecting milli or nano second int " + date + " at position " + offset);
            }
            offset += intOffset.len;
        }

        char timezoneIndicator = date.charAt(offset);
        if (!(timezoneIndicator == '+' || timezoneIndicator == '-' || timezoneIndicator == 'Z')) {
            throw new IndexOutOfBoundsException("Invalid time zone indicator " + timezoneIndicator + " in " + date);
        }

        int zIndex;
        if ((zIndex = date.indexOf('[', offset)) != -1) {
            final ZoneId zone;
            zone = ZoneId.of(date.substring(offset, zIndex));
            return ZonedDateTime.of(LocalDateTime.of(LocalDate.of(year, month, day),
                    LocalTime.of(hour, minutes, seconds, nano)), zone).withZoneSameInstant(ZoneId.of(date.substring(zIndex + 1, date.length() - 1)));
        } else {
            final ZoneId zone;
            zone = ZoneId.of(date.substring(offset));
            return ZonedDateTime.of(LocalDateTime.of(LocalDate.of(year, month, day),
                    LocalTime.of(hour, minutes, seconds, nano)), zone);
        }
    }

    /**
     * Check if the expected character exist at the given offset of the
     *
     * @param value    the string to check at the specified offset
     * @param offset   the offset to look for the expected character
     * @param expected the expected character
     * @throws IndexOutOfBoundsException if the expected character is not found
     */
    private static void checkOffset(String value, int offset, char expected) throws IndexOutOfBoundsException {
        char found = value.charAt(offset);
        if (found != expected) {
            throw new IndexOutOfBoundsException("Expected '" + expected + "' character but found '" + found + "' in " + value);
        }
    }


    record IntOffset(int value, int len) {
    }

    /**
     * Parse an integer located between 2 given offsets in a string
     *
     * @param value      the string to parse
     * @param beginIndex the start index for the integer in the string
     * @param maxIndex   the end index for the integer in the string
     * @return the IntOffset
     * @throws NumberFormatException if the value is not a number
     */
    private static IntOffset parseAllInt(String value, int beginIndex, int maxIndex) throws NumberFormatException {
        if (beginIndex < 0 || maxIndex > value.length() || beginIndex > maxIndex) {
            throw new NumberFormatException(value);
        }
        // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
        int i = beginIndex;
        int result = 0;
        int digit;
        if (i < maxIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                return new IntOffset(-result, i - beginIndex - 1);
            }
            result = -digit;
        }
        while (i < maxIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                return new IntOffset(-result, i - beginIndex - 1);
            }
            result *= 10;
            result -= digit;
        }
        return new IntOffset(-result, i - beginIndex - 1);
    }

    /**
     * Parse an integer located between 2 given offsets in a string
     *
     * @param value      the string to parse
     * @param beginIndex the start index for the integer in the string
     * @param endIndex   the end index for the integer in the string
     * @return the int
     * @throws NumberFormatException if the value is not a number
     */
    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
        int i = beginIndex;
        int result = 0;
        int digit;
        if (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result = -digit;
        }
        while (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result *= 10;
            result -= digit;
        }
        return -result;
    }

    /**
     * Zero pad a number to a specified length
     *
     * @param buffer buffer to use for padding
     * @param value  the integer value to pad if necessary.
     * @param length the length of the string we should zero pad
     */
    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }
}

