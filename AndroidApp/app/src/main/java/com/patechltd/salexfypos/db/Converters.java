package com.patechltd.salexfypos.db;

import androidx.room.TypeConverter;

public class Converters {

    @TypeConverter
    public static String fromStringArray(java.util.List<String> value) {
        return value == null ? null : String.join(",", value);
    }

    @TypeConverter
    public static java.util.List<String> toStringArray(String value) {
        if (value == null || value.isEmpty()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(java.util.Arrays.asList(value.split(",", -1)));
    }
}
