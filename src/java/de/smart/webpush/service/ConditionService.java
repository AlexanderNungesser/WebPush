package de.smart.webpush.service;

import de.smart.webpush.data.SimpleResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class ConditionService {

    // Calculates the url timestamp filtering for periodic conditions
    public static String calcPeriod(String smartDataRecordsUrl, JsonObject condition, String sensorTable, LocalDateTime start, LocalDateTime end) {
        String periodType = condition.getString("period_type");
        String time = "&start=";
        String endParam = "&end=";

        switch (periodType) {
            case "all":
                String url = smartDataRecordsUrl + sensorTable + HttpService.StorageSmartmonitoring + "&includes=ts" + "&order=ts,ASC" + "&size=1";
                JsonObject firstEntry = HttpService.getFirstRecord(url);
                String globalFirstActivity = firstEntry.getString("ts");
                time += globalFirstActivity;
                break;
            case "year":
                time += end.minusYears(1) + endParam + end;
                break;
            case "month":
                time += end.minusMonths(1) + endParam + end;
                break;
            case "week":
                time += end.minusWeeks(1) + endParam + end;
                break;
            case "day":
                time += end.minusDays(1) + endParam + end;
                break;
            case "route":
                time += start + endParam + end;
                break;
            case "date":
                LocalDate date = LocalDate.parse(condition.getString("date_start"));
                LocalDateTime dateTime = date.atTime(LocalTime.now());
                time += dateTime;
                break;
            case "time":
                LocalTime startTime = LocalTime.parse(condition.getString("time_start"));
                LocalTime endTime = LocalTime.parse(condition.getString("time_end"));

                LocalDateTime timeStart = LocalDate.now().atTime(startTime);
                LocalDateTime timeEnd = LocalDate.now().atTime(endTime);

                time += timeStart + endParam + timeEnd;
                break;
            case "range":
                LocalDate startDate = LocalDate.parse(condition.getString("date_start"));
                LocalTime startTimeRange = LocalTime.parse(condition.getString("time_start"));

                LocalDate endDate = LocalDate.parse(condition.getString("date_end"));
                LocalTime endTimeRange = LocalTime.parse(condition.getString("time_end"));

                LocalDateTime rangeStart = LocalDateTime.of(startDate, startTimeRange);
                LocalDateTime rangeEnd = LocalDateTime.of(endDate, endTimeRange);

                time += rangeStart + endParam + rangeEnd;
                break;
            default:
                break;
        }
        return time;
    }

    // Gets the measurement_process of the datapoint with ts equal to lastActivity
    public static String getMeasurementProcess(String smartDataRecordsUrl, String sensorTable, String lastActivity) {
        String measurementProcessUrl = smartDataRecordsUrl + sensorTable + HttpService.StorageSmartmonitoring + "&filter=ts,eq," + lastActivity + "&includes=measurement_process" + "&size=1";
        JsonObject measurementProcess = HttpService.getFirstRecord(measurementProcessUrl);
        return measurementProcess.getString("measurement_process");
    }

    // Gets the first ts from a measurement_process
    public static String getFirstActivity(String smartDataRecordsUrl, String sensorTable, String measurementProcess) {
        String firstActivityUrl = smartDataRecordsUrl + sensorTable + HttpService.StorageSmartmonitoring + "&filter=measurement_process,eq," + measurementProcess + "&includes=ts" + "&order=ts,ASC" + "&size=1";
        JsonObject firstActivity = HttpService.getFirstRecord(firstActivityUrl);
        return firstActivity.getString("ts");
    }

    public static JsonObject evaluateCondition(JsonObject condition, JsonObject group, String smartDataRecordsUrl) {

        String sensorTable = group.getString("data_table");
        String type = condition.getString("type");

        Map<String, String> placeholderMap = Map.of("<id>", String.valueOf(group.getInt("group_id")),
                "<collection>", sensorTable);

        String url = condition.getString("url");
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            url = url.replace(entry.getKey(), entry.getValue());
        }

        boolean isPeriodic = condition.getBoolean("periodic");

        if (isPeriodic) {
            String lActivity = group.getString("last_activity", null);
            if (lActivity == null) {
                return Json.createObjectBuilder()
                        .addNull("value")
                        .add("operator", condition.getString("operator"))
                        .add("threshold", condition.getJsonNumber("threshold"))
                        .add("status", Response.Status.BAD_REQUEST.getStatusCode())
                        .add("error", "Could not calc Period, because >last_activity< is null")
                        .build();
            }
            LocalDateTime lastActivity = LocalDateTime.parse(lActivity);
            String measurementProcess = ConditionService.getMeasurementProcess(smartDataRecordsUrl, sensorTable, lActivity);
            String fActivity = ConditionService.getFirstActivity(smartDataRecordsUrl, sensorTable, measurementProcess);
            LocalDateTime firstActivity = LocalDateTime.parse(fActivity);
            String period = ConditionService.calcPeriod(smartDataRecordsUrl, condition, sensorTable, firstActivity, lastActivity);

            url += period;
        }

        SimpleResponse dataResp = HttpService.get(url);

        if (dataResp.getStatus() != 200) {
            return Json.createObjectBuilder()
                    .addNull("value")
                    .add("operator", condition.getString("operator"))
                    .add("threshold", condition.getJsonNumber("threshold"))
                    .add("status", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .add("error", "Something went wrong during getting of " + type + " out of " + dataResp.readEntity(String.class))
                    .build();
        }
        String dataRespText = dataResp.readEntity(String.class);

        JsonObject data;
        try (JsonReader reader = Json.createReader(new StringReader(dataRespText))) {
            data = reader.readObject();
        }

        double value = 0.0;
        JsonArray records = data.getJsonArray("records");

        if (records == null || records.isEmpty()) {
            String key = data.entrySet().iterator().next().getKey();
            value = data.getJsonNumber(key).doubleValue();
        } else {
            value = records.getJsonObject(0)
                    .getJsonNumber(type).doubleValue();
        }
        return Json.createObjectBuilder()
                .add("value", value)
                .add("operator", condition.getString("operator"))
                .add("threshold", condition.getJsonNumber("threshold"))
                .build();
    }
}
