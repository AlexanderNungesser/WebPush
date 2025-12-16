package de.smart.webpush.service;

import de.smart.webpush.data.WebhookAction;
import de.smart.webpush.rest.AdminResource;
import java.io.*;
import java.nio.file.*;

/*
This class provides static methods for editing a .properties file.
In this context it is used to create webhooks for Sensor- and Trigger-observation
*/
public class PropertiesWebhookService {

    private static final String SMARTDATAAIRQUALITY_PROPERTIES = "./SmartDataAirquality_config";

    /*
    This Method creates an properties-entry in the given file
    It checks if its already registered and creates it if its new.
    
    @param table: String of the observed table
    @param schema: String of the schema containing the table    | Default: Smartmonitoring
    @param action: WebhookAction to descrip the type of Mirroring   | Default: Post
    @param api: String of the mirroring-URL | Default: Records
    @param propertiesName: String name of the .properties-file  | Default: SmartDataAirquality_config.properties
    @param commentTitle: comment above the webhook-entry
    */
    public static void addWebhook(
            String table,
            String schema,
            WebhookAction action,
            String webhookUrl,
            String api,
            String propertiesName,
            String commentTitle      // z.B. "Mirroring of: xyz"
    ) throws IOException {

        // default-configuration
        if (propertiesName == null || propertiesName.isBlank())
            propertiesName = SMARTDATAAIRQUALITY_PROPERTIES;
        Path file = Path.of("./" + propertiesName + ".properties");
        if (api == null || api.isBlank()) api = "RECORDS";
        if (schema == null || schema.isBlank()) schema = "SMARTMONITORING";
        if (action == null) action = WebhookAction.POST;

        // Key generation
        String key =
                api + "_"
                + table.toUpperCase().replace(" ", "") + "_"
                + schema.toUpperCase().replace(" ", "") + "_"
                + action + "_url";

        //check and read file
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Config-file '" + propertiesName + "' not found");
        }
        String original = Files.readString(file);
        
        //check if the webhook is already registerd
        if (original.contains(key + "=")) {
            return;
        }
        
        //Build file entry
        StringBuilder block = new StringBuilder();
        block.append("\n");
        if (commentTitle != null && !commentTitle.isBlank()) {
            block.append("# ").append(commentTitle).append("\n");
        }
        block.append(key).append("=").append(webhookUrl).append("\n");
        
        Files.writeString(
                file,
                original + block,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
    
    
    /*
    Forwarding Method to create a webhook for Sensor-tables
    @param sensorTable: String of used table
    @param sensorName: String of the sensor name (used for comments)
    */
    public static void addMirroringEvent(String sensorTable, String sensorName) throws IOException {
        addWebhook(
                sensorTable,
                "SMARTMONITORING",
                WebhookAction.POST,
                HttpService.WebPushResourceApi + "webhook/sensor_push/" + sensorName.replace(" ", "_"),
                "RECORDS",
                "SmartDataAirquality_config",
                "Mirroring of: " + sensorName
        );
    }
}
