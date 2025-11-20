/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.service;

import static de.smart.jpatemplate.rest.ManagementResource.*;

import de.smart.jpatemplate.data.SimpleResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.StringReader;

/**
 *
 * @author Hannes
 */
public class NotificationService {
    
    //return random fitting notification
    public JsonObject pickRandomNotification(int triggerId) {
        String targetURL = HttpService.SmartDataRecordsApi 
                + "notifications" 
                + HttpService.StorageGamification
                + "&filter=trigger_id,eq,"
                + triggerId;
            SimpleResponse resp = HttpService.get(targetURL);
            if(resp.getStatus() != 200) {
                return null;
            }
            String jsonText = resp.readEntity(String.class);
            
            JsonObject root;
            try(JsonReader reader = Json.createReader(new StringReader(jsonText))){
                root = reader.readObject();
            }
            JsonArray records = root.getJsonArray("records");
            
            if(records == null || records.isEmpty()) {
                return null;
            }
            int randomIndex = (int) (Math.random() * records.size());
            return records.getJsonObject(randomIndex);
    }
    
    //split Notification to all the Group-Member
    public void distributeNotification(JsonObject notification, int groupId) {
        JsonArray members = loadMembers(groupId);
        if(members == null || members.isEmpty()) {
            return;
        }
        for(JsonObject member: members.getValuesAs(JsonObject.class)) {
            try {
                JsonObject personalized = personalize(notification, member);
                sendNotificationToMember(personalized, member);
            } catch (Exception e) {
                //do nothing
            }
        }
    }
    
    //load all Member to a specific group (based on DB-View)
    private JsonArray loadMembers(int groupId) {
        String targetURL = HttpService.SmartDataRecordsApi
                + "view_group_members"
                + HttpService.StorageGamification
                + "&filter=group_id,eq,"
                + groupId;
        
        SimpleResponse resp = HttpService.get(targetURL);
        if(resp.getStatus() != 200) {
            return null;
        }
        
        String jsonText = resp.readEntity(String.class);
        JsonObject root;
        try(JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            root = reader.readObject();
        }
        return root.getJsonArray("records");
    }
    
    private JsonObject personalize(JsonObject notification, JsonObject member) {
        String memberName = member.getString("member_name", "");
        if(memberName.isEmpty()) {
            memberName = "User";
        }
        
        String originalTitle = notification.getString("title", "");
        String originalBody = notification.getString("body", "");
        
        String resolvedTitle = originalTitle.replace("<name>", memberName);
        String resolvedBody = originalBody.replace("<name>", memberName);
        
        JsonObjectBuilder builder = Json.createObjectBuilder();
        notification.forEach(builder::add);
        builder.add("title", resolvedTitle);
        builder.add("body", resolvedBody);
        
        return builder.build();
    }
    
    private void sendNotificationToMember(JsonObject notification, JsonObject member) {
        String name = member.getString("member_name", "");        
        String endpoint = member.getString("member_endpoint", "");
        
        if(endpoint.isEmpty()) {
            return;
        }

        //use Push-Service - API
        System.out.println("title: " + notification.getString("title", "") 
                + "; body: " + notification.getString("body", "") 
                + " -> " + name + "("+ endpoint + ")");
    }
}
