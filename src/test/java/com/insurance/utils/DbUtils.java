package com.insurance.utils;

import com.insurance.app.Db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain JDBC helper for database validation. Always use PreparedStatement parameters, never string concatenation. */
public final class DbUtils {

    private DbUtils() {
    }

    public static List<Map<String, Object>> query(String sql, Object... params) {
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASSWORD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        row.put(md.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + sql + " -> " + e.getMessage(), e);
        }
    }

    /** First column of the first row, or null when the query returns nothing. */
    public static Object single(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0).values().iterator().next();
    }

    public static long count(String sql, Object... params) {
        return ((Number) single(sql, params)).longValue();
    }
}
