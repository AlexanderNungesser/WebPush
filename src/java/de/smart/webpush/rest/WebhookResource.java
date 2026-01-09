package de.smart.webpush.rest;

import de.smart.webpush.data.SimpleResponse;
import de.smart.webpush.service.HttpService;
import de.smart.webpush.service.SensorSyncService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;

@Path("/webhook")
public class WebhookResource {

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
    // Webhook: new Data in Sensor-Table
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/sensor_push/{tablename}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reactOnNewData(@PathParam("tablename") String tablename, String payload) {
        System.out.println("=== Webhook active: sensor_push ===");
        System.out.println("Edits in: " + tablename);

        JsonObject data;
        try {
            data = Json.createReader(new StringReader(payload)).readObject();
            if (!data.getBoolean("finished")) {
                return Response.ok()
                        .entity("Webhook fired for " + tablename)
                        .build();
            }
        } catch (Exception e) {
            return Response.ok()
                    .entity("Webhook fired for " + tablename)
                    .build();
        }

        //start sensor-specific job
        String jobName = SensorSyncService.jobNamePrefix + tablename.replace(" ", "_");
        int existingJobId = SensorSyncService.getJobIdByName(jobName);
        System.out.println("jobId: " + existingJobId);

        if (existingJobId != -1) {
            String startJobURL = HttpService.SmartDataJobsApi
                    + "start"
                    + HttpService.SmartDataUrl
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
