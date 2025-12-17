package de.smart.webpush.rest;

import de.smart.webpush.data.SimpleResponse;
import de.smart.webpush.data.TriggerResult;
import de.smart.webpush.service.HttpService;
import de.smart.webpush.service.ScheduledTriggerService;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.math.BigDecimal;

@Path("/admin")
public class AdminResource {

    // ───────────────────────────────────────────────────────────────
    // Create Notification Endpoint
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/createNotification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNotification(String payload) {
        try (JsonReader reader = Json.createReader(new StringReader(payload))) {
            JsonObject json = reader.readObject();

            String title = json.getString("title");
            String body = json.getString("body", null);
            String iconUrl = json.getString("icon_url", null);
            String imageUrl = json.getString("image_url", null);
            boolean renotify = json.getBoolean("renotify", false);
            boolean silent = json.getBoolean("silent", false);
            int triggerId = json.getInt("trigger_id", -1);

            JsonObject notification = Json.createObjectBuilder()
                    .add("title", title)
                    .add("body", body)
                    .add("icon_url", iconUrl != null ? iconUrl : "")
                    .add("image_url", imageUrl != null ? imageUrl : "")
                    .add("renotify", renotify)
                    .add("silent", silent)
                    .add("trigger_id", triggerId)
                    .build();

            if (findExisting("notification", notification) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Notification already exists.\"}")
                        .build();
            }

            SimpleResponse resp = post("notification", notification);
            String respbody = resp.readEntity(String.class).trim();
            int notificationId = Integer.parseInt(respbody);

            JsonArray actions = json.getJsonArray("actions");
            if (actions != null) {
                for (int i = 0; i < actions.size(); i++) {
                    String actionStr = actions.getString(i);
                    int actionId = Integer.parseInt(actionStr);
                    JsonObject notifAction = Json.createObjectBuilder()
                            .add("notification_id", notificationId)
                            .add("action_id", actionId)
                            .build();
                    post("notification_action", notifAction);
                }
            }

            return Response.status(resp.getStatus()).entity(resp.readEntity(String.class)).build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Create Trigger Endpoint
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/createTrigger")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTrigger(String payload) {
        try (JsonReader reader = Json.createReader(new StringReader(payload))) {
            JsonObject json = reader.readObject();

            String description = json.getString("description", "Default Description");
            String scheduleCron = json.getString("schedule_cron", null);
            String scheduleTimestamp = json.getString("schedule_timestamp", null);

            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("description", description);

            if (scheduleCron != null) {
                if (!ScheduledTriggerService.isValidCron(scheduleCron)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"Invalid cron expression: " + scheduleCron + "\"}")
                            .build();
                }
                builder.add("cron", scheduleCron);
            }

            if (scheduleTimestamp != null) {
                scheduleTimestamp += ":00";
                builder.add("time_once", scheduleTimestamp);
            }

            JsonObject trigger = builder.build();

            SimpleResponse triggerresp = post("trigger", trigger);
            String trigrespbody = triggerresp.readEntity(String.class).trim();
            int triggerId = Integer.parseInt(trigrespbody);

            createConditions(json, triggerId);

            if (scheduleCron != null || scheduleTimestamp != null) {
                TriggerResult tr = ScheduledTriggerService.getTrigger(triggerId, trigger);
                SimpleResponse res = ScheduledTriggerService.createJobForTrigger(tr);
            }

            return Response.status(triggerresp.getStatus()).entity(triggerresp.readEntity(String.class)).build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    public void createConditions(JsonObject json, int triggerId) {
        for (String key : json.keySet()) {
            if (!key.startsWith("data_field_")) {
                continue;
            }

            String index = key.substring("data_field_".length());

            int dataField = json.getInt("data_field_" + index);
            String operator = json.getString("operator_" + index, "==");
            BigDecimal threshold = json.getJsonNumber("threshold_" + index).bigDecimalValue();

            JsonObjectBuilder conditionBuilder = Json.createObjectBuilder()
                    .add("type_id", dataField)
                    .add("operator", operator)
                    .add("threshold", threshold);

            JsonObject condition = AddPeriod(json, index, conditionBuilder);

            JsonObject existingCondition = findExisting("condition", condition);
            int conditionId;

            if (existingCondition == null) {
                SimpleResponse condResp = post("condition", condition);
                conditionId = Integer.parseInt(condResp.readEntity(String.class).trim());
            } else {
                conditionId = existingCondition.getInt("id");
            }

            JsonObject triggerCond = Json.createObjectBuilder()
                    .add("trigger_id", triggerId)
                    .add("condition_id", conditionId)
                    .build();

            post("trigger_condition", triggerCond);
        }
    }

    private JsonObject AddPeriod(JsonObject json, String index, JsonObjectBuilder conditionBuilder) {
        int periodId = json.getInt("period_" + index, 1);
        conditionBuilder.add("period_id", periodId);
        switch (periodId) {
            case 7:
                conditionBuilder.add("date_start", json.getString("period_date_" + index, ""));
                break;
            case 8:
                conditionBuilder.add("time_start", json.getString("daily_time_start_" + index));
                conditionBuilder.add("time_end", json.getString("daily_time_end_" + index));
                break;
            case 9:
                String rangeStart = json.getString("range_start_" + index, "");
                String rangeEnd = json.getString("range_end_" + index, "");

                if (!rangeStart.isEmpty()) {
                    String[] partsStart = rangeStart.split("T");
                    String dateStart = partsStart[0];
                    String timeStart = partsStart.length > 1 ? partsStart[1] : "00:00";

                    conditionBuilder.add("date_start", dateStart);
                    conditionBuilder.add("time_start", timeStart);
                }

                if (!rangeEnd.isEmpty()) {
                    String[] partsEnd = rangeEnd.split("T");
                    String dateEnd = partsEnd[0];
                    String timeEnd = partsEnd.length > 1 ? partsEnd[1] : "00:00";

                    conditionBuilder.add("date_end", dateEnd);
                    conditionBuilder.add("time_end", timeEnd);
                }
                break;
        }

        JsonObject condition = conditionBuilder.build();

        return condition;
    }

    public static JsonObject findExisting(String resource, JsonObject filterJson) {
        try {
            if (filterJson == null || filterJson.isEmpty()) {
                return null;
            }

            StringBuilder url = new StringBuilder(
                    HttpService.SmartDataRecordsApi + resource + HttpService.StorageGamification
            );

            SimpleResponse resp = HttpService.get(url.toString());
            if (resp.getStatus() != 200) {
                return null;
            }

            String body = resp.readEntity(String.class);

            try (JsonReader reader = Json.createReader(new StringReader(body))) {

                JsonObject root = reader.readObject();
                if (!root.containsKey("records")) {
                    return null;
                }

                var arr = root.getJsonArray("records");

                for (int i = 0; i < arr.size(); i++) {
                    JsonObject item = arr.getJsonObject(i);

                    boolean match = true;

                    for (String key : filterJson.keySet()) {
                        if (filterJson.isNull(key)) {
                            continue;
                        }

                        String filterValue = filterJson.get(key).toString().replace("\"", "");

                        if (!item.containsKey(key)) {
                            match = false;
                            break;
                        }

                        String itemValue = item.get(key).toString().replace("\"", "");

                        if (!itemValue.equals(filterValue)) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        return item;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public SimpleResponse post(String target, JsonObject json) {
        String url = HttpService.SmartDataRecordsApi + target + HttpService.StorageGamification;
        return HttpService.post(url, json);
    }

}
