package com.insurance.engine;

import com.insurance.utils.TestDataReader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The "Run Manager": run_manager.csv lists every scenario and an Execute flag (Yes/No).
 * Switching a scenario on or off is a data change - no Java edit, no recompile.
 */
public final class RunManager {

    private RunManager() {
    }

    public static List<Map<String, String>> scenariosToRun() {
        return TestDataReader.readAll("run_manager").stream()
                .filter(row -> "Yes".equalsIgnoreCase(row.get("Execute")))
                .collect(Collectors.toList());
    }
}
