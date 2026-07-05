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
                // Parse database URL using java.net.URI by mapping the scheme to http
                String httpUrl = dbUrl.replaceFirst("postgres(ql)?://", "http://");
                java.net.URI uri = new java.net.URI(httpUrl);
                
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath(); // starts with "/"
                String query = uri.getQuery();
                
                String username = "";
                String password = "";
                if (userInfo != null) {
                    String[] credParts = userInfo.split(":", 2);
                    username = java.net.URLDecoder.decode(credParts[0], java.nio.charset.StandardCharsets.UTF_8.name());
                    if (credParts.length > 1) {
                        password = java.net.URLDecoder.decode(credParts[1], java.nio.charset.StandardCharsets.UTF_8.name());
                    }
                }
                
                // If host is internal and we are running locally, append Ohio Render suffix
                if (host.contains("dpg-") && !host.contains(".render.com") && System.getenv("RENDER") == null) {
                    host += ".ohio-postgres.render.com";
                }
                
                String portStr = port != -1 ? ":" + port : "";
                String jdbcUrl = "jdbc:postgresql://" + host + portStr + path;
                
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("?user=").append(username).append("&password=").append(password);
                
                if (query != null && !query.isEmpty()) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (!param.startsWith("user=") && !param.startsWith("password=")) {
                            queryBuilder.append("&").append(param);
                        }
                    }
                }
                
                String finalUrl = jdbcUrl + queryBuilder.toString();
                if (!finalUrl.contains("sslmode=")) {
                    finalUrl += "&sslmode=require";
                }
                
                System.setProperty("spring.datasource.url", finalUrl);
                System.setProperty("spring.datasource.username", username);
                System.setProperty("spring.datasource.password", password);
                System.out.println("🚀 Configured JDBC URL: jdbc:postgresql://" + host + portStr + path + "?sslmode=require");
            } catch (Exception e) {
                System.err.println("Error parsing database URL: " + e.getMessage());
                String jdbcUrl = dbUrl.replaceFirst("postgres(ql)?://", "jdbc:postgresql://");
                System.setProperty("spring.datasource.url", jdbcUrl);
            }
        }

        SpringApplication.run(Application.class, args);
    }
}
