package de.smart.jpatemplate.rest;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import java.security.KeyPair;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


import de.smart.jpatemplate.data.PushSubscription;
import de.smart.jpatemplate.data.SubscriptionApiClient;
import de.smart.jpatemplate.data.KeyManager;
import de.smart.jpatemplate.data.MessagePayload;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;


@Path("/push")
public class PushResource {

    private static final SubscriptionApiClient client = new SubscriptionApiClient();
    private static final PushService pushService = new PushService();
    private final Jsonb builder = JsonbBuilder.create();
    private KeyPair keyPair;

    private synchronized void ensureKeyPair() {
        if (keyPair == null) {
            keyPair = KeyManager.getKeyPair();
            pushService.setKeyPair(keyPair);
        }
    }
    
    @GET
    @Path("/key")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKey() {
        ensureKeyPair();
        
        return Response.ok(Map.of("key", KeyManager.convertPublicKey(keyPair))).build();
    }

    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribe(PushSubscription subscription) {
        if (subscription == null || subscription.getEndpoint() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Subscription data missing"))
                    .build();
        }
        try {
            int id = client.save(subscription);
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("status", "subscribed"))
                    .entity(Map.of("id", id))
                    .build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
        
    }
    
    @DELETE
    @Path("/subscribe/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSubscription(@PathParam("id") String id) {
        try {
            boolean removed = client.delete(id);

            if (!removed) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Subscription not found"))
                        .build();
            }

            return Response.ok(Map.of("status", "deleted")).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendNotificationToAll(MessagePayload payload) {
        if (!isValidPayload(payload)) {
            return invalidPayloadResponse();
        }
        
        List<String> sentTo = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try {
            
            for (PushSubscription sub : client.findAll()) {
                sendToSubscription(sub, payload, sentTo, failed);
            }

            return Response.ok(Map.of("sent", sentTo, "failed", failed))
                    .build();
            
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    @POST
    @Path("/send/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendNotification(@PathParam("id") String id, MessagePayload payload) {
        if (!isValidPayload(payload)) {
            return invalidPayloadResponse();
        }
        
        List<String> sentTo = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try {  
            PushSubscription sub = client.getSubscription(id);
            
            if (sub == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Subscription not found"))
                        .build();
            }
            
            sendToSubscription(sub, payload, sentTo, failed);

            return Response.ok(Map.of("sent", sentTo, "failed", failed))
                    .build();
            
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    private boolean isValidPayload(MessagePayload payload) {
        return payload != null && payload.title != null && payload.body != null;
    }

    private Response invalidPayloadResponse() {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Missing required fields"))
                .build();
    }
    
    private void sendToSubscription(PushSubscription sub,  MessagePayload payload, 
                                    List<String> sent, List<String> failed) {
        try {
            String messageJson = builder.toJson(payload);
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    PushSubscription.getUserPublicKey(sub.getKey()),
                    PushSubscription.convertKeyToBytes(sub.getAuth()),
                    messageJson.getBytes()
            );

            pushService.send(notification);
            sent.add(sub.getEndpoint());

        } catch (Exception e) {
            failed.add(sub.getEndpoint());
        }
    }
            
}
