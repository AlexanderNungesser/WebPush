package de.smart.jpatemplate.rest;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.service.HttpService;
import de.smart.jpatemplate.service.NotificationService;
import de.smart.jpatemplate.service.SensorSyncService;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.math.BigDecimal;

@Path("/admin")
public class ManagementResource {

    public static String smartDataBaseURL = "http://localhost:8080/SmartDataAirquality/smartdata/records/";
    public static String STORAGE_GAMIFICATION = "?storage=gamification";
    public static String STORAGE_SMARTMONITORING = "?storage=smartmonitoring";

    
    
    
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
    // Webhook Mirror Endpoints
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_post(String payload) {
        System.out.println("=== Webhook Triggered (POST) ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }

    @PUT
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_put(String payload) {
        System.out.println("=== Webhook Triggered (PUT) ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }

    @DELETE
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_delete(String payload) {
        System.out.println("=== Webhook Triggered (DELETE) ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }


    // ───────────────────────────────────────────────────────────────
    // Webhook: tbl_observedobject change
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/tbl_observecobject_change")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleObjectChange(String payload) {
        try (JsonReader reader = Json.createReader(new StringReader(payload))) {
            JsonObject json = reader.readObject();
            SensorSyncService service = new SensorSyncService();
            service.processSensor(json);
        } catch (Exception e) {
            System.err.println("WebPush - observedobject-webhook: JSON-Parsing Fehler: " + e.getMessage());
        }
        return Response.ok().build();
    }
}
