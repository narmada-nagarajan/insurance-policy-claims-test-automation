package com.insurance.pages;

import com.insurance.utils.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Parent of all page objects: every interaction goes through an EXPLICIT wait, so page classes and tests
 * never need Thread.sleep(). Handles the two classic flaky-click exceptions (stale element, click intercepted).
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
    }

    protected WebElement visible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /** Waits until clickable, clicks, and retries up to 3 times if the DOM changed under us. */
    protected void click(By by) {
        for (int attempt = 1; ; attempt++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }
    }

    protected void type(By by, String text) {
        WebElement element = visible(by);
        element.clear();
        if (text != null && !text.isEmpty()) {
            element.sendKeys(text);
        }
    }

    protected String text(By by) {
        return visible(by).getText().trim();
    }

    /** Waits until the element has some text (messages that are filled in asynchronously). */
    protected String nonEmptyText(By by) {
        ExpectedCondition<String> hasText = d -> {
            String t = d.findElement(by).getText().trim();
            return t.isEmpty() ? null : t;
        };
        return wait.until(hasText);
    }

    /**
     * Waits until the element's text CONTAINS the fragment. Use this (not nonEmptyText) when the element may still
     * show an OLD message from a previous action - otherwise the test reads stale text and fails intermittently.
     */
    protected void waitForTextToContain(By by, String fragment) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(by, fragment));
    }

    protected void selectByVisibleText(By by, String label) {
        new Select(visible(by)).selectByVisibleText(label);
    }

    protected void selectByValue(By by, String value) {
        new Select(visible(by)).selectByValue(value);
    }

    protected String selectedText(By by) {
        return new Select(visible(by)).getFirstSelectedOption().getText();
    }

    /** Non-throwing check: false when the element is absent or hidden. */
    protected boolean isDisplayed(By by) {
        try {
            return driver.findElement(by).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
