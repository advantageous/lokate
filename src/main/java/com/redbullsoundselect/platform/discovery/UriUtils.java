package com.redbullsoundselect.platform.discovery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by gcc on 4/22/16.
 */
public class UriUtils {

    private UriUtils() {
    }

    public static Map<String, String> splitQuery(String query) {
        if (query == null) return Collections.emptyMap();
        final Map<String, String> queryPairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&|;");
        for (final String pair : pairs) {
            final int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }
}
