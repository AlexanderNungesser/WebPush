package de.smart.jpatemplate.service;

import de.smart.jpatemplate.data.PropertiesEditor;
import de.smart.jpatemplate.data.SimpleResponse;
import static de.smart.jpatemplate.rest.ManagementResource.STORAGE_GAMIFICATION;
import static de.smart.jpatemplate.rest.ManagementResource.smartDataBaseURL;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class SensorSyncService {

    public void processSensor(JsonObject json) {
        String name = json.getString("name", "");
        String collection = json.getString("data_collection", "");
        int ootype_id = json.getInt("ootype_id", 0);
        
        // only mobile sensors
        if(ootype_id != 3) {
            return;
        }
        // check whether group already exists
        if (groupExists(name, collection)) {
            System.out.println("WebPush - Group already registered in gamification.groups");
        } else {
            //create gamification.groups entry
            Map<String, String> groupPayload = new HashMap<>();
            groupPayload.put("name", name);
            groupPayload.put("data_table", collection);

            String groupURL = smartDataBaseURL + "groups" + STORAGE_GAMIFICATION;
            HttpService.post(groupURL, groupPayload.toString());
        }
        
        //edit SmartDataAirquality_config.properties
        try {
            PropertiesEditor.addMirroringEvent(collection, name);
        } catch (IOException ex) {
            System.err.println("WebPush - Error writing mirroring event: " + ex.getMessage());
            return;
        }
        System.out.println("WebPush -- tbl_observedobject synchronized");
    }

    
    private boolean groupExists(String name, String collection) {
        String targetURL = smartDataBaseURL + "groups" + STORAGE_GAMIFICATION;
        try {
            SimpleResponse response = HttpService.get(targetURL);

            if (response.getStatus() != 200) {
                System.err.println("WebPush - cannot request gamification.groups.");
                return false;
            }
            String jsonText = response.readEntity(String.class);
            
            try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
                JsonObject root = reader.readObject();
                JsonArray records = root.getJsonArray("records");

                if (records == null) {
                    return false;
                }
                for (JsonObject obj : records.getValuesAs(JsonObject.class)) {
                    String existingName  = obj.getString("name", "");
                    String existingTable = obj.getString("data_table", "");
                    
                    if (existingName.equals(name) && existingTable.equals(collection)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("WebPush - Error while checking groupExists: " + e.getMessage());
        }
        return false;
    }
}
