package com.insurance.listeners;

import com.insurance.base.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Logs test progress and saves a screenshot to target/screenshots when a UI test fails (Jenkins archives it). */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("[START] " + name(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("[PASS]  " + name(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("[SKIP]  " + name(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("[FAIL]  " + name(result) + " -> " + result.getThrowable());
        WebDriver driver = DriverFactory.get();
        if (driver instanceof TakesScreenshot shooter) {
            try {
                byte[] png = shooter.getScreenshotAs(OutputType.BYTES);
                Path dir = Paths.get("target", "screenshots");
                Files.createDirectories(dir);
                String file = name(result).replaceAll("[^A-Za-z0-9_.-]", "_") + "_" + System.currentTimeMillis() + ".png";
                Files.write(dir.resolve(file), png);
                System.out.println("        screenshot: " + dir.resolve(file));
            } catch (Exception e) {
                System.out.println("        could not take screenshot: " + e.getMessage());
            }
        }
    }

    private static String name(ITestResult result) {
        Object[] params = result.getParameters();
        String suffix = params.length > 0 ? "[" + params[0] + "]" : "";
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName() + suffix;
    }
}
