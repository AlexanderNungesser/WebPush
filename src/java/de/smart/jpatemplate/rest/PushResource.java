package de.smart.jpatemplate.rest;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import java.security.KeyPair;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import de.smart.jpatemplate.data.PushSubscription;
import de.smart.jpatemplate.data.PushStorage;
import de.smart.jpatemplate.data.KeyManager;

@Path("/push")
public class PushResource {
    private static KeyPair KEY_PAIR = null;
    private static final String SUBJECT = "mailto:max.mustermann@hsbi.de";
    private static final PushStorage PushStorage = new PushStorage();
    
    @GET
    @Path("/key")
    public String getKey() {
        if (KEY_PAIR == null){
            KEY_PAIR = KeyManager.getKeyPair();
        }
        return KeyManager.convertPublicKey(KEY_PAIR);
    }
    
    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    public void subscribe(PushSubscription subscription) {
        PushStorage.add(subscription);
    }

    @POST
    @Path("/send")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendNotification(){
        String result = "Notifications sent to:";
        for (PushSubscription sub : PushStorage.getAll()) {
            try {
                Notification notification;
                PushService pushService = new PushService()
                                        .setKeyPair(KEY_PAIR)
                                        .setSubject(SUBJECT);

                String payload = "{\"title\":\"Hello from Payara!\",\"body\":\"This is a Push Notification.\"}";
                notification = new Notification(
                        sub.getEndpoint(),
                        sub.getUserPublicKey(),
                        sub.getAuthAsBytes(),
                        payload.getBytes()
                );
                pushService.send(notification);
            }
            catch (Exception exp) {
                return exp.getMessage();
            }
            result += "\n" + sub.getEndpoint();
        }
        return result;
    }
}

