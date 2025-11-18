/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.*;
/**
 *
 * @author steidlemax
 */
public class SubscriptionApiClient {
    private static final String BASE_URL = "http://localhost:8080/SmartDataAirquality/smartdata/records/member/";
    private static final String SCHEMA = "?storage=gamification";
    private final Client client = ClientBuilder.newClient();
    private final Jsonb builder = JsonbBuilder.create();

    public void save(PushSubscription subscription) {
        Response response = null;
        try {
            String json = builder.toJson(subscription);
            response = client
                    .target(BASE_URL + SCHEMA)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(json, MediaType.APPLICATION_JSON));
        
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (response.getStatus() >= 400) {
            throw new RuntimeException("Failed to save subscription: " + response.readEntity(String.class));
        }
    }

    public boolean delete(String id) {
        Response response = client
                .target(BASE_URL+id+SCHEMA)
                .request()
                .delete();

        return response.getStatus() == 200 || response.getStatus() == 204;
    }

    public PushSubscription getSubscription(String id){
        String url = BASE_URL+id+SCHEMA;
        System.err.println(url);
        Response response = client
                .target(BASE_URL+id+SCHEMA)
                .request(MediaType.APPLICATION_JSON)
                .get();
        
        if (response.getStatus() >= 400) {
            throw new RuntimeException("Failed to fetch subscriptions: " + response.readEntity(String.class));
        }
        String rawJson = response.readEntity(String.class);
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        JsonArray array = json.getJsonArray("records");
        
        if (array.isEmpty()) {
            throw new RuntimeException("Subscription not found");
        }
        
        JsonValue v = array.get(0);
        PushSubscription s = builder.fromJson(v.toString(), PushSubscription.class);
        
        return s;
    }
    
    public List<PushSubscription> findAll() {
        Response response = client
                .target(BASE_URL+SCHEMA)
                .request(MediaType.APPLICATION_JSON)
                .get();

        if (response.getStatus() >= 400) {
            throw new RuntimeException("Failed to fetch subscriptions: " + response.readEntity(String.class));
        }
        String rawJson = response.readEntity(String.class);
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        JsonArray array = json.getJsonArray("records");

        List<PushSubscription> subscriptions = new ArrayList<>();
        for (JsonValue v : array) {
            PushSubscription s = builder.fromJson(v.toString(), PushSubscription.class);
            subscriptions.add(s);
        }
        return subscriptions;
    }
}
