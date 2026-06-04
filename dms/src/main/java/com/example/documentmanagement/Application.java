package com.example.documentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Programmatic environment adaptation for Render/Cloud
        String dbUrl = System.getenv("RENDER_DB_URL");
        String dbUser = System.getenv("RENDER_DB_USER");
        String dbPass = System.getenv("RENDER_DB_PASS");

        if (dbUrl != null) {
            String jdbcUrl = dbUrl.replaceFirst("postgres(ql)?://", "jdbc:postgresql://")
                    .replaceAll("//.*@", "//");

            if (!jdbcUrl.contains("sslmode=")) {
                jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }

            System.setProperty("spring.datasource.url", jdbcUrl);
            System.out.println("Adapted JDBC URL: " + jdbcUrl.split("@")[0] + "@***");
        }

        if (dbUser != null) {
            System.setProperty("spring.datasource.username", dbUser);
        }

        if (dbPass != null) {
            System.setProperty("spring.datasource.password", dbPass);
        }

        SpringApplication.run(Application.class, args);
    }
}
