package com.insurance.base;

import com.insurance.app.InsuranceApp;
import com.insurance.utils.Config;

import java.io.IOException;

/** Starts the application under test once per JVM, whichever test class runs first (suite, IDE or single class). */
public final class AppServer {

    private static InsuranceApp app;

    private AppServer() {
    }

    public static synchronized void ensureStarted() {
        if (app != null) {
            return;
        }
        InsuranceApp started = new InsuranceApp();
        try {
            started.start(Config.port());
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the app on port " + Config.port()
                    + " (is another process using it? try -Dapp.port=8090)", e);
        }
        app = started;
        Runtime.getRuntime().addShutdownHook(new Thread(started::stop));
    }
}
