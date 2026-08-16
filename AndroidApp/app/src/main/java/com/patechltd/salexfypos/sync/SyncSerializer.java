package com.patechltd.salexfypos.sync;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class SyncSerializer {

    private SyncSerializer() {
    }

    public static String toJson(Object entity) {
        if (entity == null) return "{}";
        try {
            JSONObject obj = new JSONObject();
            for (Field f : entity.getClass().getFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Object value = f.get(entity);
                if (value != null) obj.put(f.getName(), value);
            }
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        T entity;
        try {
            entity = type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        if (json == null || json.isEmpty()) return entity;
        try {
            JSONObject obj = new JSONObject(json);
            for (Field f : type.getFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!obj.has(f.getName())) continue;
                Class<?> t = f.getType();
                if (t == String.class) {
                    f.set(entity, obj.optString(f.getName()));
                } else if (t == double.class) {
                    f.setDouble(entity, obj.optDouble(f.getName(), 0));
                } else if (t == int.class) {
                    f.setInt(entity, obj.optInt(f.getName(), 0));
                } else if (t == long.class) {
                    f.setLong(entity, obj.optLong(f.getName(), 0));
                } else if (t == boolean.class) {
                    f.setBoolean(entity, obj.optBoolean(f.getName(), false));
                }
            }
        } catch (Exception ignored) {
        }
        return entity;
    }
}
