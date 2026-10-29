package com.insurance.base;

import com.insurance.utils.Config;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Creates one browser per test thread (ThreadLocal => safe for parallel runs).
 * Selenium Manager (built into Selenium 4.6+) downloads the matching driver automatically.
 */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver create() {
        boolean headless = Config.headless();
        WebDriver driver = switch (Config.browser()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) {
                    o.addArguments("-headless");
                }
                yield new FirefoxDriver(o);
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) {
                    o.addArguments("--headless=new");
                }
                yield new EdgeDriver(o);
            }
            default -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) {
                    // flags needed for containers / CI agents
                    o.addArguments("--headless=new", "--window-size=1366,768", "--no-sandbox", "--disable-dev-shm-usage");
                } else {
                    o.addArguments("--start-maximized");
                }
                yield new ChromeDriver(o);
            }
        };
        // Deliberately NO implicit wait: mixing implicit and explicit waits gives unpredictable timeouts.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver get() {
        return DRIVER.get();
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
