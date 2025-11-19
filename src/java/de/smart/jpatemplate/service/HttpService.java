/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.service;

import de.smart.jpatemplate.data.SimpleResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 *
 * @author Hannes
 */

public class HttpService {

    private static final HttpClient http = HttpClient.newHttpClient();

    // ───────────────────────────────────────────────────────────────
    // GET
    // ───────────────────────────────────────────────────────────────
    public static SimpleResponse get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            return new SimpleResponse(resp.statusCode(), resp.body());

        } catch (Exception e) {
            String err = "{\"error\":\"GET request failed: " + e.getMessage() + "\"}";
            return new SimpleResponse(500, err);
        }
    }

    // ───────────────────────────────────────────────────────────────
    // POST
    // ───────────────────────────────────────────────────────────────
    public static SimpleResponse post(String url, String body) {
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            return new SimpleResponse(resp.statusCode(), resp.body());

        } catch (Exception e) {
            String err = "{\"error\":\"POST request failed: " + e.getMessage() + "\"}";
            return new SimpleResponse(500, err);
        }
    }
    
    // ───────────────────────────────────────────────────────────────
    // DELETE
    // ───────────────────────────────────────────────────────────────
    public static SimpleResponse delete(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            return new SimpleResponse(resp.statusCode(), resp.body());

        } catch (Exception e) {
            String err = "{\"error\":\"DELETE request failed: " + e.getMessage() + "\"}";
            return new SimpleResponse(500, err);
        }
    }
}