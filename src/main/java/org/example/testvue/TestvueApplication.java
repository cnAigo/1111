package org.example.testvue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.PrintStream;

@SpringBootApplication
public class TestvueApplication {

    private static final PrintStream OUT = System.out;

    public static void main(String[] args) {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        OUT.println(">>> TaaS backend starting...");
        try {
            SpringApplication.run(TestvueApplication.class, args);
            OUT.println(">>> TaaS backend started successfully.");
        } catch (Exception e) {
            OUT.println(">>> FATAL: Spring Boot startup failed!");
            OUT.println(">>> Exception type: " + e.getClass().getName());
            OUT.println(">>> Message: " + e.getMessage());
            OUT.println(">>> Stack trace:");
            e.printStackTrace(OUT);

            // Also check for nested causes
            Throwable cause = e.getCause();
            while (cause != null) {
                OUT.println(">>> Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause.printStackTrace(OUT);
                cause = cause.getCause();
            }
            System.exit(1);
        }
    }
}
