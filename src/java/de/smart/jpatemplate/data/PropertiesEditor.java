package de.smart.jpatemplate.data;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class PropertiesEditor {
    private static final String PROPERTIES_PATH = "./SmartDataAirquality_config.properties";

    public static void addMirroringEvent(String sensorTable, String sensorName) throws IOException {
        File file = new File(PROPERTIES_PATH);
        if(!file.exists()) {
            throw new FileNotFoundException("Konfigurationsdatei nicht gefunden: " + PROPERTIES_PATH);
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        String key = "RECORDS_" + sensorTable.toUpperCase().replace(" ", "") + "_SMARTMONITORING_POST_url";

        if (props.containsKey(key)) {
            //System.out.println("[INFO] Mirroring Event existiert bereits: " + key);
            return;
        }
        String url = "http://localhost:8080/WebPush/smarttemplate/admin/webhook";
        String originalContent = Files.readString(Path.of(PROPERTIES_PATH));

        StringBuilder appendBlock = new StringBuilder();
        appendBlock.append("\n\n# Mirroring of: ").append(sensorName).append("\n");
        appendBlock.append(key).append("=").append(url).append("\n");

        String newContent = originalContent + appendBlock.toString();
        Files.writeString(Path.of(PROPERTIES_PATH), newContent);
        //System.out.println("[SUCCESS] Mirroring Event hinzugefügt: " + key);
    }
}
