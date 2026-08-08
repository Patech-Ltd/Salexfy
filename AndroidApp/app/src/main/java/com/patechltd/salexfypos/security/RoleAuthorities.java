package com.patechltd.salexfypos.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patechltd.salexfypos.model.Authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoleAuthorities {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<Authority> fromJson(String json) {
        List<Authority> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            List<String> names = MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
            for (String name : names) {
                try {
                    list.add(Authority.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static String toJson(List<Authority> authorities) {
        try {
            List<String> names = new ArrayList<>();
            for (Authority a : authorities) names.add(a.name());
            return MAPPER.writeValueAsString(names);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static Set<String> nameSet(List<Authority> authorities) {
        Set<String> set = new HashSet<>();
        for (Authority a : authorities) set.add(a.name());
        return set;
    }
}
