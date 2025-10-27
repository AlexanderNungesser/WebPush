/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.rest;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.KeyPair;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import de.smart.jpatemplate.data.PushSubscription;
import de.smart.jpatemplate.data.PushStorage;
import de.smart.jpatemplate.data.ReadVAPIDKeys;

@Path("/push")
public class PushResource {
    private static final String PUBLIC_KEY = "";
    private static final String PRIVATE_KEY = "";
    //private static KeyPair KEY_PAIR = null;
    private static final String SUBJECT = "mailto:max.steidle@hsbi.de";
    private static final PushStorage PushStorage = new PushStorage();
    
    @GET
    @Path("/key")
    public String getKey() {
        //if (KEY_PAIR == null){
        //    KEY_PAIR = ReadVAPIDKeys.getKeyPair();
        //}
        return PUBLIC_KEY;//ReadVAPIDKeys.convertVAPIDKey(KEY_PAIR);
    }
    
    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    public String subscribe(PushSubscription subscription) {
        PushStorage.add(subscription);
        return "Endpoint: " + subscription.getEndpoint() + " | Auth-Key: " + subscription.getKey() + " " +  subscription.getTest();
    }

    @POST
    @Path("/send")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendNotification() throws GeneralSecurityException, IOException, JoseException {
        String result = "Notifications gesendet an !";
        for (PushSubscription sub : PushStorage.getAll()) {
            Notification notification;
            PushService pushService = new PushService().setPrivateKey(PRIVATE_KEY).setPublicKey(PUBLIC_KEY)
                                    //.setKeyPair(KEY_PAIR)
                                    .setSubject(SUBJECT);

            String payload = "{\"title\":\"Hallo von Payara!\",\"body\":\"Dies ist eine Push Notification.\"}";
            notification = new Notification(
                    sub.getEndpoint(),
                    sub.getUserPublicKey(),
                    sub.getAuthAsBytes(),
                    payload.getBytes()
            );
            try {
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

