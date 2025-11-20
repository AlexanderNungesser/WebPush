package de.smart.jpatemplate.service;

import de.smart.jpatemplate.data.WebhookAction;
import de.smart.jpatemplate.rest.ManagementResource;
import java.io.*;
import java.nio.file.*;

public class PropertiesWebhookService {

    private static final String DEFAULT_PROPERTIES = "./SmartDataAirquality_config.properties";

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
            propertiesName = "SmartDataAirquality_config";

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

        if (!Files.exists(file)) {
            throw new FileNotFoundException("Config-file '" + propertiesName + "' not found");
        }
        String original = Files.readString(file);
        
        if (original.contains(key + "=")) {
            return;
        }
        
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
    
    public static void addMirroringEvent(String sensorTable, String sensorName) throws IOException {
        addWebhook(
                sensorTable,
                "SMARTMONITORING",
                WebhookAction.POST,
                ManagementResource.WebPushResourceApi + "webhook/sensor_push/" + sensorTable,
                "RECORDS",
                "SmartDataAirquality_config",
                "Mirroring of: " + sensorName
        );
    }
}
