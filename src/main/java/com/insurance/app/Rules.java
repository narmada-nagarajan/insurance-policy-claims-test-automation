package com.insurance.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

/** Business rules of the sample insurance product. Kept separate so they are easy to unit-test and explain. */
public final class Rules {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 75;
    public static final BigDecimal UNDERWRITING_LIMIT = new BigDecimal("1000000");
    public static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("50000");

    private static final Map<String, BigDecimal> RATE = Map.of(
            "HEALTH", new BigDecimal("0.02"),
            "AUTO", new BigDecimal("0.03"),
            "HOME", new BigDecimal("0.015"));

    private Rules() {
    }

    public static boolean isValidCoverageType(String type) {
        return type != null && RATE.containsKey(type);
    }

    public static int ageOn(LocalDate dob, LocalDate today) {
        return Period.between(dob, today).getYears();
    }

    /** premium = coverage x product rate x age factor (under 25: 1.20, 25-50: 1.00, over 50: 1.30). */
    public static BigDecimal premium(String type, BigDecimal coverage, int age) {
        BigDecimal ageFactor;
        if (age < 25) {
            ageFactor = new BigDecimal("1.20");
        } else if (age <= 50) {
            ageFactor = BigDecimal.ONE;
        } else {
            ageFactor = new BigDecimal("1.30");
        }
        return coverage.multiply(RATE.get(type)).multiply(ageFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns null when the applicant passes underwriting, otherwise the rejection reason. */
    public static String underwritingRejection(int age, BigDecimal coverage) {
        if (age < MIN_AGE) {
            return "Applicant is below minimum age of " + MIN_AGE;
        }
        if (age > MAX_AGE) {
            return "Applicant is above maximum age of " + MAX_AGE;
        }
        if (coverage.compareTo(UNDERWRITING_LIMIT) > 0) {
            return "Coverage exceeds underwriting limit of 1000000";
        }
        return null;
    }
}
