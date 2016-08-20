package io.advantageous.discovery.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UriUtils {

    private UriUtils() {
        throw new IllegalStateException("this class should not be instantiated.");
    }

     public static Map<String, String> splitQuery(String query) {
        if (query == null) return Collections.emptyMap();
        final Map<String, String> queryPairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&|;");
        for (final String pair : pairs) {
            final int idx = pair.indexOf('=');
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }
}
