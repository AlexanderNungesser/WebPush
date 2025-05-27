package de.smart.jpatemplate.rest;

import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.LoggerException;
import de.fhbielefeld.scl.rest.util.ResponseObjectBuilder;
import de.smart.jpatemplate.persistence.jpa.TemplateThing;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import de.smart.jpatemplate.config.Configuration;
import javax.naming.NamingException;
import jakarta.ws.rs.Consumes;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.PUT;

/**
 * REST Service template
 *
 * @author Florian Fehring
 */
@Path("template")
@Tag(name = "Template", description = "Description of this method group")
public class TemplateResource {

    // TODO the unitName must match the configured name in the persistence.xml
    @PersistenceContext(unitName = "SmartDataPU")
    private EntityManager em;

    @Resource
    private UserTransaction utx;

    public TemplateResource() {
        // Init logging
        try {
            String moduleName = (String) new javax.naming.InitialContext().lookup("java:module/ModuleName");
            Configuration conf = new Configuration();
            // TODO rename SmartTemplate to your applications name
            Logger.getInstance("SmartTemplate", moduleName);
            Logger.setDebugMode(Boolean.parseBoolean(conf.getProperty("debugmode")));
        } catch (LoggerException | NamingException ex) {
            System.err.println("Error init logger: " + ex.getLocalizedMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Enable SmartUserAuth to activate user rights management for this resource
    //@SmartUserAuth
    @Operation(summary = "Creates a TemplateResource new",
            description = "Creates a new TemplateResource and stores it in database")
    @APIResponse(
            responseCode = "201",
            description = "Primary key of the new created dataset.",
            content = @Content(
                    mediaType = "text/plain",
                    example = "1"
            ))
    @APIResponse(
            responseCode = "500",
            description = "Error mesage",
            content = @Content(mediaType = "application/json",
                    example = "{\"errors\" : [ \" Could not create ecause of ... \"]}"))
    public Response get(
            @Parameter(description = "TemplateThings json representation") TemplateThing templateThing,
            @Context HttpHeaders headers) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();

        Configuration conf = new Configuration();

        // TODO implement createing a thing
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // Enable SmartUserAuth to activate user rights management for this resource
    //@SmartUserAuth
    @Operation(summary = "´Get a single templateThing",
            description = "TemplateThing from database.")
    @APIResponse(
            responseCode = "200",
            description = "TemplateThing requested",
            content = @Content(
                    mediaType = "application/json",
                    example = "{\"records\" : [{\"id\" :  1, \"title\" : \"Mustertitle\"}]}"
            ))
    @APIResponse(
            responseCode = "500",
            description = "Error mesage",
            content = @Content(mediaType = "application/json",
                    example = "{\"errors\" : [ \" Could not get users because of ... \"]}"))
    public Response get(@Context HttpHeaders headers) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();
        Configuration conf = new Configuration();

        // TODO implement listing of things
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // Enable SmartUserAuth to activate user rights management for this resource
    //@SmartUserAuth
    @Operation(summary = "Lists users",
            description = "Lists all users from database.")
    @APIResponse(
            responseCode = "200",
            description = "Users requested",
            content = @Content(
                    mediaType = "application/json",
                    example = "{\"records\" : [{\"id\" :  1, \"name\" : \"Mustermann\"}]}"
            ))
    @APIResponse(
            responseCode = "500",
            description = "Error mesage",
            content = @Content(mediaType = "application/json",
                    example = "{\"errors\" : [ \" Could not get users: Because of ... \"]}"))
    public Response list(@Context HttpHeaders headers) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();
        Configuration conf = new Configuration();

        // TODO implement listing of things
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }

    @POST
    @Path("delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Enable SmartUserAuth to activate user rights management for this resource
    //@SmartUserAuth
    @Operation(summary = "Delete a templateThing",
            description = "Delete templateThing stored in database")
    @APIResponse(
            responseCode = "200",
            description = "Deleted id.",
            content = @Content(
                    mediaType = "text/plain",
                    example = "1"
            ))
    @APIResponse(
            responseCode = "500",
            description = "Error mesage",
            content = @Content(mediaType = "application/json",
                    example = "{\"errors\" : [ \" Could not delete because of ... \"]}"))
    public Response delete(
            @Parameter(description = "Datasets id") Long id,
            @Context HttpHeaders headers) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();

        Configuration conf = new Configuration();

        // TODO implement delete logic here
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Enable SmartUserAuth to activate user rights management for this resource
    //@SmartUserAuth
    @Operation(summary = "Updates a templateThing",
            description = "Updates a templateThing stored in database")
    @APIResponse(
            responseCode = "200",
            description = "Primary key of the updated dataset.",
            content = @Content(
                    mediaType = "text/plain",
                    example = "1"
            ))
    @APIResponse(
            responseCode = "500",
            description = "Error mesage",
            content = @Content(mediaType = "application/json",
                    example = "{\"errors\" : [ \" Could not update because of ... \"]}"))
    public Response update(
            @Parameter(description = "TemplateThings data to update. Must contain the id.") TemplateThing user,
            @Context HttpHeaders headers) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();

        Configuration conf = new Configuration();

        // TODO implement update logic here
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }
}
