/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import jakarta.json.Json;
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

    public void save(PushSubscription subscription) {
        Response response = null;
        try {
            String json = """
                        {
                          "name": "%s",
                          "endpoint": "%s",
                          "key": "%s",
                          "auth": "%s"
                        }
                        """.formatted(
                                subscription.getName(),
                                subscription.getEndpoint(),
                                subscription.getKey(),   // oder encoded value
                                subscription.getAuth()
                        );

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

    public boolean delete(String endpoint) {
        Response response = client
                .target(BASE_URL+SCHEMA)
                .path(endpoint)
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
        System.out.println(array);
        if (array.isEmpty()) {
            System.err.println("In if");
            throw new RuntimeException("Subscription not found");
        }
        
        JsonObject obj = array.get(0).asJsonObject();
        
        PushSubscription s = new PushSubscription();
        s.setEndpoint(obj.getString("endpoint", null));
        s.setAuth(obj.getString("auth", null));
        s.setName(obj.getString("name", null));
        s.setKey(obj.getString("key", null));
        
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
            JsonObject obj = v.asJsonObject();

            PushSubscription s = new PushSubscription();
            s.setEndpoint(obj.getString("endpoint", null));
            s.setAuth(obj.getString("auth", null));
            s.setName(obj.getString("name", null));
            s.setKey(obj.getString("key", null));

            subscriptions.add(s);
        }
        return subscriptions;
    }
}
