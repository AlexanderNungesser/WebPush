package de.smart.jpatemplate.rest;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import java.security.KeyPair;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import de.smart.jpatemplate.data.PushSubscription;
import de.smart.jpatemplate.data.PushStorage;
import de.smart.jpatemplate.data.KeyManager;
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
        return Response.ok("{\"key\":\"" + KeyManager.convertPublicKey(KEY_PAIR) + "\"}").build();
    }

    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response subscribe(PushSubscription subscription) {
        PushStorage.add(subscription);
        return Response.ok("{\"status\":\"subscribed\"}").build();
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendNotification(String message) {
        List<String> sentTo = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        message = """
                    {
                      "title": "Test Notification Java",
                      "body": "This is a test notification sent from the Web Push API. Java",
                      "icon": "/img/logo.png"
                    }
                    """;

        try {

            for (PushSubscription sub : PushStorage.getAll()) {
                try {
                    Notification notification = new Notification(
                            sub.getEndpoint(),
                            sub.getUserPublicKey(),
                            sub.getAuthAsBytes(),
                            message.getBytes()
                    );

                    pushService.send(notification);
                    sentTo.add(sub.getEndpoint());
                } catch (Exception e) {
                    failed.add(sub.getEndpoint());
                    e.printStackTrace();
                }
            }

            Map<String, List> result = new HashMap<>();
            result.put("sent", sentTo);
            result.put("failed", failed);

            return Response.ok(result).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

}
