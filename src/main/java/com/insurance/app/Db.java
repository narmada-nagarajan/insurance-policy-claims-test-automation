package com.insurance.app;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JDBC helper around an in-memory H2 database (the "insurance" DB). */
public final class Db {

    /** Tests connect to the same URL because the app runs inside the test JVM. */
    public static final String URL = "jdbc:h2:mem:insurance;DB_CLOSE_DELAY=-1";
    public static final String USER = "sa";
    public static final String PASSWORD = "";

    private Db() {
    }

    /** Thrown for DB errors; carries the SQLState so callers can map 23505 (duplicate) to HTTP 409. */
    public static class DbException extends RuntimeException {
        private final String sqlState;

        public DbException(String message, String sqlState, Throwable cause) {
            super(message, cause);
            this.sqlState = sqlState;
        }

        public String sqlState() {
            return sqlState;
        }

        public boolean isDuplicate() {
            return "23505".equals(sqlState);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Drops and recreates all tables, giving every run a clean, known database. */
    public static void reset() {
        String[] ddl = {
                "DROP TABLE IF EXISTS claims",
                "DROP TABLE IF EXISTS policies",
                "DROP TABLE IF EXISTS quotes",
                "DROP TABLE IF EXISTS customers",
                "CREATE TABLE customers ("
                        + " id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + " full_name VARCHAR(100) NOT NULL,"
                        + " dob DATE NOT NULL,"
                        + " email VARCHAR(120) NOT NULL UNIQUE,"
                        + " phone VARCHAR(20),"
                        + " state VARCHAR(30))",
                "CREATE TABLE quotes ("
                        + " id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + " customer_id BIGINT NOT NULL REFERENCES customers(id),"
                        + " coverage_type VARCHAR(20) NOT NULL,"
                        + " coverage_amount DECIMAL(12,2) NOT NULL,"
                        + " premium DECIMAL(12,2) NOT NULL,"
                        + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE policies ("
                        + " id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + " quote_id BIGINT NOT NULL UNIQUE REFERENCES quotes(id),"
                        + " customer_id BIGINT NOT NULL REFERENCES customers(id),"
                        + " status VARCHAR(20) NOT NULL,"
                        + " reason VARCHAR(200),"
                        + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE claims ("
                        + " id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + " policy_id BIGINT NOT NULL REFERENCES policies(id),"
                        + " amount DECIMAL(12,2) NOT NULL,"
                        + " description VARCHAR(200),"
                        + " status VARCHAR(20) NOT NULL,"
                        + " reason VARCHAR(200),"
                        + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
        try (Connection c = connect(); Statement st = c.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DbException("Schema setup failed: " + e.getMessage(), e.getSQLState(), e);
        }
    }

    public static List<Map<String, Object>> query(String sql, Object... args) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof Date || v instanceof Timestamp) {
                            v = v.toString();
                        }
                        row.put(md.getColumnLabel(i).toLowerCase(), v);
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DbException(e.getMessage(), e.getSQLState(), e);
        }
    }

    /** Runs an INSERT and returns the generated primary key. */
    public static long insert(String sql, Object... args) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new DbException(e.getMessage(), e.getSQLState(), e);
        }
    }

    public static int update(String sql, Object... args) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DbException(e.getMessage(), e.getSQLState(), e);
        }
    }

    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }
}
