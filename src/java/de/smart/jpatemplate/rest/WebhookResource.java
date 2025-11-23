/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.rest;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.service.HttpService;
import de.smart.jpatemplate.service.NotificationService;
import de.smart.jpatemplate.service.SensorSyncService;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import netscape.javascript.JSObject;

/**
 *
 * @author Hannes
 */
@Path("/webhook")
public class WebhookResource {
   
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
    @Path("/tbl_observedobject_change")
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
    
    
    // ───────────────────────────────────────────────────────────────
    // Webhooks: changes in Trigger-table -> trigger Job-Management
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/trigger_post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reactOnNewTrigger(String payload) {
        System.out.println("=== Webhook active: trigger_post ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
    
    @DELETE
    @Path("/trigger_delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reactOnTriggerChange(String payload) {
        System.out.println("=== Webhook active: trigger-delete ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
    
    
    // ───────────────────────────────────────────────────────────────
    // Webhook: new Data in Sensor-Table
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/sensor_push/{tablename}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reactOnNewData(@PathParam("tablename") String tablename, String payload) {
        System.out.println("=== Webhook active: sensor_push ===");
        System.out.println("Edits in: " + tablename);
        System.out.println("payload: " + payload);
        
        //start sensor-specific job
        String jobName = SensorSyncService.jobNamePrefix + tablename.replace(" ", "_");
        int existingJobId = SensorSyncService.getJobIdByName(jobName);
        System.out.println("jobId: "+ existingJobId);
        
        if(existingJobId != -1) {
            String startJobURL = HttpService.SmartDataJobsApi
                + "&" + HttpService.StorageSmartmonitoring.substring(1)
                + "&collection=" + HttpService.DataJobs
                + "&id=" + existingJobId;
            System.out.println(startJobURL);
            SimpleResponse resp = HttpService.get(startJobURL);
        }
        
        return Response.ok()
                .entity("Webhook fired for " + tablename)
                .build();
    }
}
