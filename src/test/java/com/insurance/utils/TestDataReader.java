package com.insurance.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads CSV data tables from src/test/resources/testdata (the "data driven" part of the framework).
 * Placeholders are resolved at read time so the data never goes stale:
 *   {{unique}} -> a per-scenario token (unique emails on every run)
 *   {{age:35}} -> a date of birth that makes the person exactly 35 years old today
 * To use Excel instead, only this class changes (read with Apache POI, return the same maps).
 */
public final class TestDataReader {

    private static final Pattern AGE = Pattern.compile("\\{\\{age:(\\d+)}}");

    private TestDataReader() {
    }

    /** All rows of testdata/&lt;name&gt;.csv as header-to-value maps. */
    public static List<Map<String, String>> readAll(String name) {
        String resource = "/testdata/" + name + ".csv";
        try (InputStream in = TestDataReader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Data file not found on classpath: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> headers = null;
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> cells = parseLine(line);
                if (headers == null) {
                    headers = cells;
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), i < cells.size() ? cells.get(i).trim() : "");
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    /** One data set (row) looked up by its DataSetId, with placeholders resolved. */
    public static Map<String, String> dataSet(String name, String dataSetId, String unique) {
        for (Map<String, String> row : readAll(name)) {
            if (dataSetId.equals(row.get("DataSetId"))) {
                Map<String, String> resolved = new LinkedHashMap<>();
                row.forEach((k, v) -> resolved.put(k, resolve(v, unique)));
                return resolved;
            }
        }
        throw new IllegalArgumentException("DataSetId '" + dataSetId + "' not found in " + name + ".csv");
    }

    static String resolve(String value, String unique) {
        String out = value.replace("{{unique}}", unique);
        return AGE.matcher(out).replaceAll(m ->
                LocalDate.now().minusYears(Long.parseLong(m.group(1))).minusDays(100).toString());
    }

    /** Minimal CSV line parser: commas separate cells, double quotes allow commas inside a cell. */
    static List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                cells.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        cells.add(cur.toString());
        return cells;
    }
}
