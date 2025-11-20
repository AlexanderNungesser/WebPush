package de.smart.jpatemplate.service;

import de.smart.jpatemplate.data.SimpleResponse;
import static de.smart.jpatemplate.rest.ManagementResource.*;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/*
This class processes given Sensors.
Responsibilities:
- create entrys in gamification.groups
- create webhooks to observe data-tables (smartmonitoring.<table-name>)
*/
public class SensorSyncService {

    /*
    The method processSensor synchronizes a given sensor to gamification
    Responsibilities:
    - create a group entry for the given sensor: gamification.gorups
    - creates a webhook for table-observation via "PropertiesWebhookService"
    */
    public static void processSensor(JsonObject json) {
        String name = json.getString("name", "");
        String collection = json.getString("data_collection", "");
        int ootype_id = json.getInt("ootype_id", 0);
        
        //only mobile sensors
        if(ootype_id != 3) {
            return;
        }
        //check whether group already exists
        if (groupExists(name, collection)) {
            System.out.println("WebPush - Sensor '" + name + "' already registered in gamification.groups");
        } else {
            //create gamification.groups entry
            JsonObjectBuilder groupBuilder = Json.createObjectBuilder()
                    .add("name", name)
                    .add("data_table", collection);
            JsonObject groupJson = groupBuilder.build();
            
            String groupURL = HttpService.SmartDataRecordsApi + "groups" + HttpService.StorageGamification;
            HttpService.post(groupURL, groupJson);
        }
        
        //edit SmartDataAirquality_config.properties
        try {
            PropertiesWebhookService.addMirroringEvent(collection, name);
            
        } catch (IOException ex) {
            System.err.println("WebPush - Error writing mirroring event: " + ex.getMessage());
            return;
        }
        System.out.println("WebPush -- Sensor '" + name + "' synchronized");
    }

    
    /*
    This method checks if a given group <name & collection> is already registered in the database gamification.groups
    @param name: String of the Sensor-Name
    @param collection: String of the data-table (smartmonitoring.<data-table>)
    */
    private static boolean groupExists(String name, String collection) {
        String targetURL = HttpService.SmartDataRecordsApi + "groups" + HttpService.StorageGamification;
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
