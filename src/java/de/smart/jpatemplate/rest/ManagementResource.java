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
            //System.out.println("RAW PAYLOAD = " + payload);

            JsonObject json = reader.readObject();
            //System.out.println("PARSED JSON = " + json);

            // TODO: SCHMEISS `schedule_type` RAUS
            
            String scheduleType = json.getString("schedule_type", "trigger");
            String scheduleCron = json.getString("schedule_cron", null);
            String scheduleTimestamp = json.getString("schedule_timestamp", null);

            String description = json.getString("description", "");

            JsonObjectBuilder triggerBuilder = Json.createObjectBuilder()
                    .add("description", description)
                    .add("schedule_type", scheduleType);

            if ("recurring".equalsIgnoreCase(scheduleType)) {
                triggerBuilder.add("cron", scheduleCron != null ? scheduleCron : "");
            } else if ("once".equalsIgnoreCase(scheduleType)) {
                triggerBuilder.add("time_once", scheduleTimestamp != null ? scheduleTimestamp : "");
            }

            JsonObject trigger = triggerBuilder.build();
            
            //System.out.println("FINAL TRIGGER JSON = " + trigger);
            
            final String triggerPostUrl = HttpService.SmartDataRecordsApi
                    + "triggers"
                    + HttpService.StorageGamification;
            SimpleResponse resp = HttpService.post(triggerPostUrl, trigger);

            String respbody = resp.readEntity(String.class).trim();
            int triggerId = Integer.parseInt(respbody);

            for (String key : json.keySet()) {
                if (key.startsWith("data_field_")) {
                    String index = key.substring("data_field_".length());
                    String dataField = json.getString(key);
                    String operator = json.containsKey("operator_" + index) ? json.getString("operator_" + index) : "==";
                    BigDecimal threshold = new BigDecimal(json.getString("threshold_" + index, "0"));
                    
                    JsonObject condition = Json .createObjectBuilder()
                            .add("id", Integer.parseInt(index))
                            .add("data_field", dataField)
                            .add("operator", operator)
                            .add("threshold", threshold)
                            .build();
                    
                    final String conditionPostUrl = HttpService.SmartDataRecordsApi
                            + "condition"
                            + HttpService.StorageGamification;
                    HttpService.post(conditionPostUrl, condition);

                    System.out.println("Linking Condition ID " + Integer.parseInt(index) + " to Trigger ID " + triggerId);
                
                    JsonObject triggerCond = Json.createObjectBuilder()
                            .add("trigger_id", triggerId)
                            .add("condition_id", Integer.parseInt(index))
                            .build();
                
                    final String triggerConditionsPostUrl = HttpService.SmartDataRecordsApi
                            + "trigger_conditions"
                            + HttpService.StorageGamification;
                    HttpService.post(triggerConditionsPostUrl, triggerCond);
                }
            }
            
            TriggerResult tr = TriggerService.getTrigger(triggerId, trigger);

            SimpleResponse res = TriggerService.createJobForTrigger(tr);

            return Response.status(resp.getStatus()).entity(resp.readEntity(String.class)).build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
