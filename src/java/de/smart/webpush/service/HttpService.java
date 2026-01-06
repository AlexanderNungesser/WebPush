/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.webpush.service;

import de.smart.webpush.data.SimpleResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;

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
    
    public static final String SmartDataCollectionApi = "http://localhost:8080/SmartDataAirquality/smartdata/collection/";
    public static final String SmartDataRecordsApi = "http://localhost:8080/SmartDataAirquality/smartdata/records/";
    public static final String WebPushResourceApi = "http://localhost:8080/WebPush/webpush/";
    public static final String SmartDataJobsApi = "http://localhost:8080/SmartDataJobs/smartdatajobs/jobexecution/";
    public static final String StorageSmartmonitoring = "?storage=smartmonitoring";
    public static final String StorageGamification = "?storage=gamification"; 
    
    public static final String DataJobs = "datajobs";
    public static final String DataJobsParams = "datajobs_params";
    public static final String SmartDataUrl = "?smartdataurl=/SmartDataAirquality";
    
    

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
    private static SimpleResponse post(String url, String body) {
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
    public static SimpleResponse post(String url, JsonObject json) {
        return post(url, json.toString());
    }
    
    // ───────────────────────────────────────────────────────────────
    // PUT
    // ───────────────────────────────────────────────────────────────
    private static SimpleResponse put(String url, String body) {
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            return new SimpleResponse(resp.statusCode(), resp.body());

        } catch (IOException | InterruptedException e) {
            String err = "{\"error\":\"PUT request failed: " + e.getMessage() + "\"}";
            return new SimpleResponse(500, err);
        }
    }
    public static SimpleResponse put(String url, JsonObject json) {
        return put(url, json.toString());
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

    public static JsonObject getFirstRecord(String url) {
        SimpleResponse simpleResponse = HttpService.get(url);
        if (simpleResponse.getStatus() != 200) {
            return Json.createObjectBuilder().build();
        }
        try (JsonReader reader = Json.createReader(new StringReader(simpleResponse.readEntity(String.class)))) {
            JsonObject response = reader.readObject();
            return response.getJsonArray("records").getJsonObject(0);
        }
    }
}