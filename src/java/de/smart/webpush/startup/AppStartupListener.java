package de.smart.webpush.startup;

import de.smart.webpush.data.SimpleResponse;
import de.smart.webpush.data.WebhookAction;
import de.smart.webpush.service.SensorSyncService;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.StringReader;
import de.smart.webpush.service.HttpService;
import de.smart.webpush.service.PropertiesWebhookService;
import de.smart.webpush.service.TriggerService;
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
        String targetURL = HttpService.SmartDataRecordsApi + "tbl_observedobject" + HttpService.StorageSmartmonitoring;
        SimpleResponse sr = HttpService.get(targetURL);
        if (sr.getStatus() != 200) {
            System.err.println("WebPush - Startup Sync failed: Http " + sr.getStatus());
            return;
        }
        //load and parse json
        String jsonText = sr.readEntity(String.class);
        try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            JsonObject root = reader.readObject();
            JsonArray records = root.getJsonArray("records");

            if (records == null) {
                System.err.println("WebPush - StartupSync: 'records' not found.");
                return;
            }

            //iterate sensors and process Sensor
            for (JsonObject obj : records.getValuesAs(JsonObject.class)) {
                SensorSyncService.processSensor(obj);
            }
        }
        //manually creation of additional webhooks
        try {
            PropertiesWebhookService.addWebhook("tbl_observedobject",
                    "SMARTMONITORING",
                    WebhookAction.POST,
                    HttpService.WebPushResourceApi + "webhook/" + "tbl_observedobject_change",
                    "RECORDS",
                    null,
                    "tbl Observe-Objects");

        } catch (IOException e) {
            //do nothing
        }
        List<JsonObject> triggers = TriggerService.getScheduledTriggers();
        if (triggers == null || triggers.isEmpty()) {
            return;
        }
        JsonArrayBuilder resp = Json.createArrayBuilder();

        System.out.println("=== Job Creation for all Triggers ===");

        for (JsonObject trigger : triggers) {
            int triggerId = trigger.getInt("trigger_id");
            if (TriggerService.jobAlreadyExists(triggerId)) {
                continue;
            }
            SimpleResponse r = TriggerService.createJobForScheduledTrigger(triggerId, trigger);
            if (r.getStatus() != 200) {
                resp.add(Json.createObjectBuilder().add("error", "could not create Job for trigger " + trigger.getInt("id")));
            } else {
                resp.add(Json.createReader(new StringReader(r.readEntity(String.class))).readObject());
            }
        }

        System.out.println(resp.build());
    }
}
