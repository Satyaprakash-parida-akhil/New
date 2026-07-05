package com.example.documentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Universal Cloud Database Adapter
        String dbUrl = System.getenv("RENDER_DB_URL");
        if (dbUrl == null)
            dbUrl = System.getenv("DATABASE_URL"); // Fallback for other platforms

        String dbUser = System.getenv("RENDER_DB_USER");
        if (dbUser == null)
            dbUser = System.getenv("DATABASE_USER");

        String dbPass = System.getenv("RENDER_DB_PASS");
        if (dbPass == null)
            dbPass = System.getenv("DATABASE_PASSWORD");

        if (dbUrl != null) {
            // 1. Convert prefix
            String jdbcUrl = dbUrl.replaceFirst("postgres(ql)?://", "jdbc:postgresql://")
                    .replaceAll("//.*@", "//");

            // 2. Fix the "Short Hostname" issue (The cause of UnknownHostException)
            // If the host is just 'dpg-xxx-a', append the Render global suffix
            if (jdbcUrl.contains("dpg-") && !jdbcUrl.contains(".render.com") && System.getenv("RENDER") == null) {
                jdbcUrl = jdbcUrl.replaceFirst("(@|//)(dpg-[^:/]+)", "$1$2.ohio-postgres.render.com");
            }

            // 3. Force SSL
            if (!jdbcUrl.contains("sslmode=")) {
                jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }

            System.setProperty("spring.datasource.url", jdbcUrl);
            System.out.println("🚀 Adapted JDBC URL: " + jdbcUrl.split("@")[0] + "@***");
        }

        if (dbUser != null)
            System.setProperty("spring.datasource.username", dbUser);
        if (dbPass != null)
            System.setProperty("spring.datasource.password", dbPass);

        SpringApplication.run(Application.class, args);
    }
}
