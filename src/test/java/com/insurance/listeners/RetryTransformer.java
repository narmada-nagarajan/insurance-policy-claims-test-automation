package com.insurance.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Attaches the RetryAnalyzer to UI tests only; API and DB tests are deterministic and never retried. */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        Class<?> owner = testClass != null ? testClass : (testMethod != null ? testMethod.getDeclaringClass() : null);
        if (owner != null && owner.getName().contains(".tests.ui.")) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
