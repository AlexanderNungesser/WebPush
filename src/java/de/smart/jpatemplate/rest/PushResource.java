package de.smart.jpatemplate.rest;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import java.security.KeyPair;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import de.smart.jpatemplate.data.PushSubscription;
import de.smart.jpatemplate.data.PushStorage;
import de.smart.jpatemplate.data.KeyManager;
import de.smart.jpatemplate.data.MessagePayload;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Path("/push")
public class PushResource {

    private static KeyPair KEY_PAIR = null;
    private static final PushStorage PushStorage = new PushStorage();
    private static final PushService pushService = new PushService();

    @GET
    @Path("/key")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKey() {
        if (KEY_PAIR == null) {
            KEY_PAIR = KeyManager.getKeyPair();
            pushService.setKeyPair(KEY_PAIR);
        }
        
        return Response.ok(Map.of("key", KeyManager.convertPublicKey(KEY_PAIR))).build();
    }

    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response subscribe(PushSubscription subscription) {
        if (subscription == null || subscription.getEndpoint() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Subscription data missing"))
                    .build();
        }
        
        PushStorage.add(subscription);
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("status", "subscribed"))
                .build();
    }
    
    @DELETE
    @Path("/subscribe/{id}")
    public Response deleteSubscription(@PathParam("id") String endpoint) {
        boolean removed = PushStorage.remove(endpoint);

        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Subscription not found"))
                    .build();
        }

        return Response.ok(Map.of("status", "deleted")).build();
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendNotification(MessagePayload payload) {
        
        if (payload == null || payload.title == null || payload.body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields"))
                    .build();
        }
        
        List<String> sentTo = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try {
            String messageJson = """
                                 {
                                    "title":    "%s",
                                    "body":     "%s",
                                    "icon":     "%s"
                                 }
                                 """.formatted(payload.title, payload.body, payload.icon);
            
            
            for (PushSubscription sub : PushStorage.getAll()) {
                try {
                    Notification notification = new Notification(
                            sub.getEndpoint(),
                            sub.getUserPublicKey(),
                            sub.getAuthAsBytes(),
                            messageJson.getBytes()
                    );

                    pushService.send(notification);
                    sentTo.add(sub.getEndpoint());
                    
                } catch (Exception e) {
                    failed.add(sub.getEndpoint());
                }
            }

            return Response.ok(Map.of("sent", sentTo, "failed", failed))
                    .build();
            
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

}
