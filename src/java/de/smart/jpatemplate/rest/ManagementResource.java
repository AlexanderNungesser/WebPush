/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.rest;

import de.smart.jpatemplate.data.ManagementDTOs.NotificationDTO;
import de.smart.jpatemplate.data.ManagementDTOs.TriggerDTO;
import de.smart.jpatemplate.data.PropertiesEditor;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hannes
 */

@Path("/admin")
public class ManagementResource {
    public String smartDataBaseURL = "http://localhost:8080/SmartDataAirquality/smartdata/records/";
    public String storageURL = "?storage=gamification";
    
    private Response executeGet(String url) {
        Client client = ClientBuilder.newClient();
        try {
            return client.target(url).request(MediaType.APPLICATION_JSON).get();
        } catch(Exception e) {
            //e.printStackTrace();   
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"GET request failed: " + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } finally {
            client.close();
        }
    }
    private Response executePost(String url, Object body) {
        Client client = ClientBuilder.newClient();
        try {
            return client.target(url).request(MediaType.APPLICATION_JSON).post(Entity.json(body));
        } catch (Exception e) {
            //e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(MediaType.APPLICATION_JSON).build();
        } finally {
            client.close();
        }
    }
    
        
    @POST
    @Path("/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNotification(@Valid NotificationDTO notification) {
        String targetURL = smartDataBaseURL + "notifications" + storageURL;
        return executePost(targetURL, notification);
    }
    
    @GET
    @Path("/notification")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listNotifications() {
        String targetURL = smartDataBaseURL + "notifications" + storageURL;
        return executeGet(targetURL);
    }
    
    @POST
    @Path("/trigger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTrigger(@Valid TriggerDTO trigger) {
        String targetURL = smartDataBaseURL + "triggers" + storageURL;
        return executePost(targetURL, trigger);
    }
    
    @GET
    @Path("/trigger")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTriggers() {
        String targetURL = smartDataBaseURL + "triggers" + storageURL;
        return executeGet(targetURL);
    }
    
    
    
    
    
    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_post(String payload) {
        System.out.println("=== Webhook Triggered ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
    
    @PUT
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_put(String payload) {
        System.out.println("=== Webhook Triggered ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
    
    @DELETE
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mirror_delete(String payload) {
        System.out.println("=== Webhook Triggered ===");
        System.out.println("Payload: " + payload);
        return Response.ok().build();
    }
    
    
    /*
    Webhook for data changes in Smartmonitoring_Airquality.smartmonitoring-tbl_observedobject
    The Webhook-API checks if the new Sensor is a mobile-device and creates the gamification.group and updates the data-webhooks-properties.
    */
    @POST
    @Path("tbl_observecobject_change")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleObjectChange_1(String payload) {
        try {
            JsonReader reader = Json.createReader(new StringReader(payload));
            JsonObject json = reader.readObject();
            String name = json.getString("name", "");

            if(name.matches("^SENSORpi m\\d+.*")) {
                String collection = json.getString("data_collection", "");
                String target_url = smartDataBaseURL + "groups" + storageURL;
                Map<String, Object> groupPayload = new HashMap<>();
                groupPayload.put("name", name);
                groupPayload.put("data_table", collection);
                
                //create group in gamification.groups
                executePost(target_url, groupPayload);
                //add data-webhook in SmartDataAirquality_config.properties
                PropertiesEditor.addMirroringEvent(collection, name);
            }
        } catch (Exception e) {
            System.err.println("JSON parsing error: " + e.getMessage());
        }
        return Response.ok().build();
    }
}
