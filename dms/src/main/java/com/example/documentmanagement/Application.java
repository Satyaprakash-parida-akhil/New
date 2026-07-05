package com.example.documentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        String dbUrl = System.getenv("RENDER_DB_URL");
        if (dbUrl == null) {
            dbUrl = System.getenv("DATABASE_URL");
        }

        if (dbUrl != null) {
            try {
                // Parse standard URL: postgresql://username:password@hostname/database
                String cleanUrl = dbUrl.replaceFirst("postgres(ql)?://", "");
                String[] parts = cleanUrl.split("@");
                if (parts.length == 2) {
                    String credentials = parts[0];
                    String hostAndDb = parts[1];
                    
                    String[] credParts = credentials.split(":", 2);
                    String username = credParts[0];
                    String password = credParts.length > 1 ? credParts[1] : "";
                    
                    String[] hostDbParts = hostAndDb.split("/", 2);
                    String host = hostDbParts[0];
                    String database = hostDbParts.length > 1 ? hostDbParts[1] : "";
                    
                    // If hostname is short and we are running locally, append Ohio Render suffix
                    if (host.contains("dpg-") && !host.contains(".render.com") && System.getenv("RENDER") == null) {
                        host += ".ohio-postgres.render.com";
                    }
                    
                    String jdbcUrl = "jdbc:postgresql://" + host + "/" + database + 
                                     "?user=" + username + 
                                     "&password=" + password;
                    
                    if (!jdbcUrl.contains("sslmode=")) {
                        jdbcUrl += "&sslmode=require";
                    }
                    
                    System.setProperty("spring.datasource.url", jdbcUrl);
                    System.setProperty("spring.datasource.username", username);
                    System.setProperty("spring.datasource.password", password);
                    System.out.println("🚀 Configured JDBC URL: jdbc:postgresql://" + host + "/" + database + "?sslmode=require");
                } else {
                    // Fallback to basic string replacements
                    String jdbcUrl = dbUrl.replaceFirst("postgres(ql)?://", "jdbc:postgresql://");
                    System.setProperty("spring.datasource.url", jdbcUrl);
                }
            } catch (Exception e) {
                System.err.println("Error parsing database URL: " + e.getMessage());
            }
        }

        SpringApplication.run(Application.class, args);
    }
}
