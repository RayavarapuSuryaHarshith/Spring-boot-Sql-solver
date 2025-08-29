package com.example.webhooksolver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.InputStream;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Run on startup (no controller required)
    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            String baseUrl = System.getProperty("webhook.baseUrl", "https://bfhldevapigw.healthrx.co.in/hiring");
            String generatePath = System.getProperty("webhook.generatePath", "/generateWebhook/JAVA");
            String testPath = System.getProperty("webhook.testPath", "/testWebhook/JAVA");
            // Configure user details (change via -D properties or application.properties)
            String name = System.getProperty("user.name", "John Doe");
            String regNo = System.getProperty("user.regNo", "REG12347");
            String email = System.getProperty("user.email", "john@example.com");

            String generateUrl = baseUrl + generatePath;
            System.out.println("Sending generation request to: " + generateUrl);

            Map<String, String> body = new HashMap<>();
            body.put("name", name);
            body.put("regNo", regNo);
            body.put("email", email);

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String,String>> request = new HttpEntity<>(body, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map resp = response.getBody();
                    Object webhookObj = resp.get("webhook");
                    Object accessTokenObj = resp.get("accessToken");
                    if (webhookObj == null || accessTokenObj == null) {
                        System.out.println("Response missing expected fields. Response body: " + resp);
                        return;
                    }
                    String webhook = webhookObj.toString();
                    String accessToken = accessTokenObj.toString();
                    System.out.println("Webhook: " + webhook);
                    System.out.println("AccessToken: " + accessToken);

                    // Decide question based on last two digits of regNo
                    String digits = regNo.replaceAll("\D+", "");
                    String lastTwo = digits.length() >= 2 ? digits.substring(digits.length()-2) : digits;
                    int num = 0;
                    try { num = Integer.parseInt(lastTwo); } catch(Exception ex) {}
                    boolean odd = (num % 2) == 1;

                    String questionInfo = odd ? "/questions/question1.sql" : "/questions/question2.sql";
                    String finalQuery = readResourceAsString(questionInfo);
                    if (finalQuery == null) {
                        System.out.println("Could not read question file: " + questionInfo);
                        finalQuery = "-- FINAL QUERY PLACEHOLDER\nSELECT 1;"; 
                    }

                    // Store result locally (in temp folder)
                    java.nio.file.Path out = java.nio.file.Paths.get("final-query.sql").toAbsolutePath();
                    java.nio.file.Files.writeString(out, finalQuery, StandardCharsets.UTF_8);
                    System.out.println("Wrote final query to: " + out);

                    // Submit final query to webhook
                    String submitUrl = baseUrl + testPath;
                    // The task description says to POST to testWebhook/JAVA; however the webhook URL returned may be different.
                    // We'll prefer to use the returned 'webhook' if it looks like a valid URL:
                    String targetUrl = webhook.startsWith("http") ? webhook : submitUrl;
                    System.out.println("Submitting final query to: " + targetUrl);

                    Map<String,String> submitBody = new HashMap<>();
                    submitBody.put("finalQuery", finalQuery);

                    HttpHeaders submitHeaders = new HttpHeaders();
                    submitHeaders.setContentType(MediaType.APPLICATION_JSON);
                    // Use Bearer token style (common for JWT). If the API expects raw token, change accordingly.
                    submitHeaders.set("Authorization", "Bearer " + accessToken);

                    HttpEntity<Map<String,String>> submitRequest = new HttpEntity<>(submitBody, submitHeaders);

                    ResponseEntity<String> submitResp = restTemplate.postForEntity(targetUrl, submitRequest, String.class);
                    System.out.println("Submission response status: " + submitResp.getStatusCode());
                    System.out.println("Submission response body: " + submitResp.getBody());
                } else {
                    System.out.println("Generation request failed: " + response.getStatusCode());
                }
            } catch (Exception ex) {
                System.out.println("Error during workflow: " + ex.getMessage());
                ex.printStackTrace();
            }
        };
    }

    private static String readResourceAsString(String resourcePath) {
        try {
            ClassPathResource r = new ClassPathResource(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            try (InputStream is = r.getInputStream(); Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
                s.useDelimiter("\\A");
                return s.hasNext() ? s.next() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
