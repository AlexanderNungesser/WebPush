package de.smart.jpatemplate.rest;

import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.data.TriggerResult;
import de.smart.jpatemplate.service.TriggerService;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/trigger")
public class TriggerResource {

    // ───────────────────────────────────────────────────────────────
    // Webhook Mirror Endpoints
    // ───────────────────────────────────────────────────────────────
    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_post(JsonObject payload) {
        System.out.println("=== Webhook Triggered (POST) ===");
        System.out.println("Payload: " + payload);
//        
//        /* TODO: hier ist trigger schon erstellt, aber wenn schon einer existiert,
//            schmeißt SmartData API fehler und erstellt keinen zweiten.
//        Ohne aktuelle exists Prüfung wird dann pro Aufruf job+params erstellt/dupliziert
//        Mit aktuelle exists Prüfung werden gar keine job+params erstellt
//        */
//        
//        boolean exists = TriggerService.triggerAlreadyExists(payload);
//        if (exists) {
//            SimpleResponse resp = TriggerService.deleteTrigger(payload);
//            return Response.serverError().status(500, "Trigger " + payload + " already exists").entity(resp.readEntity(String.class)).build();
//        }
        TriggerResult tr = TriggerService.getTrigger("id", payload);

        SimpleResponse resp = TriggerService.createJobForTrigger(tr);
        return Response.status(resp.getStatus()).entity(resp.readEntity(String.class)).build();
    }

    @PUT
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_put(String payload) {
        System.out.println("=== Webhook Triggered (PUT) ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
}
