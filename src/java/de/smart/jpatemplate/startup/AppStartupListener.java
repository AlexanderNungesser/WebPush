package de.smart.jpatemplate.startup;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.data.TriggerResult;
import de.smart.jpatemplate.data.WebhookAction;
import de.smart.jpatemplate.service.SensorSyncService;
import static de.smart.jpatemplate.rest.ManagementResource.*;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.StringReader;
import de.smart.jpatemplate.service.HttpService;
import de.smart.jpatemplate.service.PropertiesWebhookService;
import de.smart.jpatemplate.service.TriggerService;
import jakarta.json.JsonArrayBuilder;
import java.util.List;
import java.io.IOException;

/*
Application startup listener for initializing WebPush-related components.
Responsibilities:
- fetch all sensors via SmartDataAirquality: smartmonitoring.tbl_observedobjects
- creates for every mobile Sensor a group entry via "SensorSyncService"
- creates for every mobile Sensor a Post-Webhook via "PropertiesWebhookService"
- creates used Webhooks for smartmonitoring.tbl_observedobjects and gamification.triggers via "PropertiesWebhookService"
*/
@WebListener
public class AppStartupListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== Initial WebPush - Sync Startup ===");
        
        //read all Sensors
        String targetURL = SmartDataRecordsApi + "tbl_observedobject" + StorageSmartmonitoring;
        SimpleResponse sr = HttpService.get(targetURL);
        if(sr.getStatus() != 200) {
            System.err.println("WebPush - Startup Sync failed: Http " + sr.getStatus());
            return;
        }
        //load and parse json
        String jsonText = sr.readEntity(String.class);
        try(JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            JsonObject root = reader.readObject();
            JsonArray records = root.getJsonArray("records");
            
            if(records == null) {
                System.err.println("WebPush - StartupSync: 'records' not found.");
                return;
            }
            
            //iterate sensors and process Sensor
            for(JsonObject obj : records.getValuesAs(JsonObject.class)) {
                SensorSyncService.processSensor(obj);
            }
        }
        //manually creation of additional webhooks
        try {
            PropertiesWebhookService.addWebhook("tbl_observedobject", 
                    "SMARTMONITORING", 
                    WebhookAction.POST, 
                    WebPushResourceApi + "webhook/" + "tbl_observedobject_change",
                    "RECORDS", 
                    null, 
                    "tbl Observe-Objects");
            
            PropertiesWebhookService.addWebhook("Triggers", 
                    "GAMIFICATION", 
                    WebhookAction.POST, 
                    WebPushResourceApi + "webhook/" + "trigger_post",
                    "RECORDS", 
                    null, 
                    "React on new Triggers");
            
            PropertiesWebhookService.addWebhook("Triggers", 
                    "GAMIFICATION", 
                    WebhookAction.DELETE, 
                    WebPushResourceApi + "webhook/" + "trigger_delete",
                    "RECORDS", 
                    null, 
                    "React on deleted Triggers");
            
        } catch (IOException e){
            //do nothing
        }
        List<TriggerResult> triggers = TriggerService.getTriggers();
        JsonArrayBuilder resp = Json.createArrayBuilder();
        
        System.out.println("=== Job Creation for all Triggers ===");
        
        for(TriggerResult trigger : triggers){
            if (TriggerService.jobAlreadyExists(trigger)) continue;
            SimpleResponse r = TriggerService.createJobForTrigger(trigger);
            resp.add(Json.createReader(new StringReader(r.readEntity(String.class))).readObject());
        }
        
        System.out.println(resp.build());
    }
}
