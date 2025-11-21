package de.smart.jpatemplate.rest;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.data.TriggerResult;
import de.smart.jpatemplate.service.HttpService;
import de.smart.jpatemplate.service.NotificationService;
import de.smart.jpatemplate.service.TriggerService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;

@Path("/admin")
public class ManagementResource {
    
    @GET
    @Path("/notification/send_random")
    public Response sendRandomNotification(
            @QueryParam("trigger_id") Integer triggerId,
            @QueryParam("group_id") int groupId
            ) {
        if(triggerId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing query parameter: trigger_id")
                    .build();
        }
        try {
            NotificationService svc = new NotificationService();
            
            JsonObject selected = svc.pickRandomNotification(triggerId);
            if(selected == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No Notification found")
                        .build();
            }
            svc.distributeNotification(selected, groupId);
            return Response.ok(selected.toString()).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("Internal error: " + e.getMessage())
                    .build();
        }
    }
    
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
            String triggerId = json.getString("trigger_id", null);

            JsonObject notification = Json.createObjectBuilder()
                    .add("title", title)
                    .add("body", body)
                    .add("icon_url", iconUrl != null ? iconUrl : "")
                    .add("image_url", imageUrl != null ? imageUrl : "")
                    .add("renotify", renotify)
                    .add("silent", silent)
                    .add("trigger_id", triggerId != null ? triggerId : "")
                    .build();

            if (findExisting("notifications", notification) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Notification already exists.\"}")
                        .build();
            }

            final String notificationPostUrl = HttpService.SmartDataRecordsApi
                    + "notifications"
                    + HttpService.StorageGamification;
            SimpleResponse resp = HttpService.post(notificationPostUrl, notification);        

            String respbody = resp.readEntity(String.class).trim();
            int notificationId = Integer.parseInt(respbody);


            for (String key : json.keySet()) {
                if (key.startsWith("action_") && json.getBoolean(key)) {
                    int actionId = Integer.parseInt(key.substring(7));
                
                    JsonObject notifAction = Json.createObjectBuilder()
                            .add("notification_id", notificationId)
                            .add("action_id", actionId)
                            .build();
                
                    final String notificationActionsPostUrl = HttpService.SmartDataRecordsApi
                            + "notification_actions"
                            + HttpService.StorageGamification;
                    HttpService.post(notificationActionsPostUrl, notifAction);
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
                if(!TriggerService.isValidCron(scheduleCron)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"Invalid cron expression: " + scheduleCron + "\"}")
                            .build();
                }
                builder.add("cron", scheduleCron);
            }

            if (scheduleTimestamp != null)
                builder.add("time_once", scheduleTimestamp);

            JsonObject trigger = builder.build();
            
            final String triggerPostUrl = HttpService.SmartDataRecordsApi
                    + "triggers"
                    + HttpService.StorageGamification;
            SimpleResponse triggerresp = HttpService.post(triggerPostUrl, trigger);

            String trigrespbody = triggerresp.readEntity(String.class).trim();
            int triggerId = Integer.parseInt(trigrespbody);

            for (String key : json.keySet()) {
                if (key.startsWith("data_field_")) {
                    String index = key.substring("data_field_".length());
                    String dataField = json.getString(key);
                    String operator = json.containsKey("operator_" + index) ? json.getString("operator_" + index) : "==";
                    BigDecimal threshold = new BigDecimal(json.getString("threshold_" + index, "0"));
                    
                    JsonObject condition = Json .createObjectBuilder()
                            .add("data_field", dataField)
                            .add("operator", operator)
                            .add("threshold", threshold)
                            .build();
                    
                    JsonObject existingCondition = findExisting("conditions", condition);
                    int conditionId;
                    if (existingCondition == null) {
                        final String conditionPostUrl = HttpService.SmartDataRecordsApi
                                + "conditions"
                                + HttpService.StorageGamification;
                        SimpleResponse conditionresp = HttpService.post(conditionPostUrl, condition);

                        String condrespbody = conditionresp.readEntity(String.class).trim();
                        conditionId = Integer.parseInt(condrespbody);
                    } else {
                        conditionId = existingCondition.getInt("id");
                    }

                    System.out.println("Linking Condition ID " + conditionId + " to Trigger ID " + triggerId);
                
                    JsonObject triggerCond = Json.createObjectBuilder()
                            .add("trigger_id", triggerId)
                            .add("condition_id", conditionId)
                            .build();
                
                    final String triggerConditionsPostUrl = HttpService.SmartDataRecordsApi
                            + "trigger_conditions"
                            + HttpService.StorageGamification;
                    HttpService.post(triggerConditionsPostUrl, triggerCond);
                }
            }
            
            TriggerResult tr = TriggerService.getTrigger(triggerId, trigger);
            SimpleResponse res = TriggerService.createJobForTrigger(tr);

            return Response.status(triggerresp.getStatus()).entity(triggerresp.readEntity(String.class)).build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    public static JsonObject findExisting(String resource, JsonObject filterJson) {
        try {
            if (filterJson == null || filterJson.isEmpty()) return null;

            StringBuilder url = new StringBuilder(
                    HttpService.SmartDataRecordsApi + resource + HttpService.StorageGamification
            );

            SimpleResponse resp = HttpService.get(url.toString());
            if (resp.getStatus() != 200) return null;

            String body = resp.readEntity(String.class);

            try (JsonReader reader = Json.createReader(new StringReader(body))) {

                JsonObject root = reader.readObject();
                System.out.println("findExisting response: " + root.toString());
                if (!root.containsKey("records")) return null;

                var arr = root.getJsonArray("records");

                for (int i = 0; i < arr.size(); i++) {
                    JsonObject item = arr.getJsonObject(i);

                    boolean match = true;

                    for (String key : filterJson.keySet()) {
                        if (filterJson.isNull(key)) continue;

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

}