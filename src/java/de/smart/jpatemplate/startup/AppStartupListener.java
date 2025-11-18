package de.smart.jpatemplate.startup;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.service.SensorSyncService;
import static de.smart.jpatemplate.rest.ManagementResource.smartDataBaseURL;
import static de.smart.jpatemplate.rest.ManagementResource.STORAGE_SMARTMONITORING;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.StringReader;
import de.smart.jpatemplate.service.HttpService;


@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== Initial WebPush - Sync Startup ===");

        SensorSyncService syncService = new SensorSyncService();
        String targetURL = smartDataBaseURL + "tbl_observedobject" + STORAGE_SMARTMONITORING;
        SimpleResponse sr = HttpService.get(targetURL);
        
        if(sr.getStatus() != 200) {
            System.err.println("WebPush - Startup Sync failed: Http " + sr.getStatus());
            return;
        }
        String jsonText = sr.readEntity(String.class);
        try(JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            JsonObject root = reader.readObject();
            JsonArray records = root.getJsonArray("records");
            
            if(records == null) {
                System.err.println("WebPush - StartupSync: 'records' not found.");
                return;
            }
            
            for(JsonObject obj : records.getValuesAs(JsonObject.class)) {
                syncService.processSensor(obj);
            }
        }
    }
}
