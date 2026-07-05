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
                // Remove prefix
                String cleanUrl = dbUrl.replaceFirst("^postgres(ql)?://", "");
                
                // Find last '@' to split credentials from host
                int lastAt = cleanUrl.lastIndexOf('@');
                if (lastAt == -1) {
                    throw new IllegalArgumentException("Invalid database URL: missing '@'");
                }
                
                String userInfo = cleanUrl.substring(0, lastAt);
                String remaining = cleanUrl.substring(lastAt + 1);
                
                // Split username and password
                String username = "";
                String password = "";
                int firstColon = userInfo.indexOf(':');
                if (firstColon != -1) {
                    username = userInfo.substring(0, firstColon);
                    password = userInfo.substring(firstColon + 1);
                } else {
                    username = userInfo;
                }
                
                // URL decode username and password if they were encoded
                username = java.net.URLDecoder.decode(username, java.nio.charset.StandardCharsets.UTF_8.name());
                password = java.net.URLDecoder.decode(password, java.nio.charset.StandardCharsets.UTF_8.name());
                
                // Split host/path and query parameters
                String hostPath = remaining;
                String query = "";
                int firstQuestion = remaining.indexOf('?');
                if (firstQuestion != -1) {
                    hostPath = remaining.substring(0, firstQuestion);
                    query = remaining.substring(firstQuestion + 1);
                }
                
                // Split host/port and database path
                String hostPort = hostPath;
                String path = "";
                int firstSlash = hostPath.indexOf('/');
                if (firstSlash != -1) {
                    hostPort = hostPath.substring(0, firstSlash);
                    path = hostPath.substring(firstSlash); // starts with '/'
                } else {
                    path = "/";
                }
                
                // Split host and port
                String host = hostPort;
                String portStr = "";
                int colonIndex = hostPort.indexOf(':');
                if (colonIndex != -1) {
                    host = hostPort.substring(0, colonIndex);
                    portStr = hostPort.substring(colonIndex); // e.g. ":5432"
                }
                
                // If host is internal and we are running locally, append Ohio Render suffix
                if (host.contains("dpg-") && !host.contains(".render.com") && System.getenv("RENDER") == null) {
                    host += ".ohio-postgres.render.com";
                }
                
                String jdbcUrl = "jdbc:postgresql://" + host + portStr + path;
                
                StringBuilder queryBuilder = new StringBuilder();
                if (query != null && !query.isEmpty()) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (!param.startsWith("user=") && !param.startsWith("password=")) {
                            if (queryBuilder.length() > 0) {
                                queryBuilder.append("&");
                            }
                            queryBuilder.append(param);
                        }
                    }
                }
                
                String finalUrl = jdbcUrl;
                if (queryBuilder.length() > 0) {
                    finalUrl += "?" + queryBuilder.toString();
                }
                
                if (!finalUrl.contains("sslmode=")) {
                    if (finalUrl.contains("?")) {
                        finalUrl += "&sslmode=require";
                    } else {
                        finalUrl += "?sslmode=require";
                    }
                }
                
                System.setProperty("spring.datasource.url", finalUrl);
                System.setProperty("spring.datasource.username", username);
                System.setProperty("spring.datasource.password", password);
                System.out.println("🚀 Configured JDBC URL: jdbc:postgresql://" + host + portStr + path + "?sslmode=require");
            } catch (Exception e) {
                System.err.println("Error parsing database URL: " + e.getMessage());
                e.printStackTrace();
                String jdbcUrl = dbUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                System.setProperty("spring.datasource.url", jdbcUrl);
            }
        }

        SpringApplication.run(Application.class, args);
    }
}
