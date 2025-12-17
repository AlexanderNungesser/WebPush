package de.smart.webpush.rest;

import de.fhbielefeld.scl.rest.util.ResponseObjectBuilder;
import de.smart.webpush.data.SimpleResponse;
import de.smart.webpush.service.HttpService;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import org.eclipse.microprofile.openapi.annotations.Operation;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/condition")
public class ConditionResource {

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count",
            description = "Counts all datasets that are in the collection in the given period")
    @APIResponse(
            responseCode = "200",
            description = "Count from all datasets in the given period")
    @APIResponse(
            responseCode = "404",
            description = "Collection could not be found")
    @APIResponse(
            responseCode = "500",
            description = "Internal error")
    public Response getCount(@Parameter(description = "SmartData URL", required = true, example = "/SmartData") @QueryParam("smartdataurl") String smartdataurl,
            @Parameter(description = "Collections name", required = true, example = "col1") @QueryParam("collection") String collection,
            @Parameter(description = "Storage name", schema = @Schema(type = STRING, defaultValue = "public")) @QueryParam("storage") String storage,
            @Parameter(description = "Start date", required = true, example = "2020-12-24T18:00") @QueryParam("start") String start,
            @Parameter(description = "End date", example = "2020-12-24T19:00") @QueryParam("end") String end) {
        ResponseObjectBuilder rob = new ResponseObjectBuilder();

        if (smartdataurl == null) {
            rob.setStatus(Response.Status.BAD_REQUEST);
            rob.addErrorMessage("Parameter >smartdataurl< is missing.");
            return rob.toResponse();
        }

        if (smartdataurl.startsWith("/")) {
            smartdataurl = "http://localhost:8080" + smartdataurl;
        }

        smartdataurl += "/smartdata/records/";

        if (collection == null) {
            rob.setStatus(Response.Status.BAD_REQUEST);
            rob.addErrorMessage("Parameter >collection< is missing.");
            return rob.toResponse();
        }

        smartdataurl += collection
                + "?storage=" + storage
                + "&countonly=true";

        if (start == null) {
            rob.setStatus(Response.Status.BAD_REQUEST);
            rob.addErrorMessage("Parameter >start< is missing.");
            return rob.toResponse();
        }

        String filter;
        if (end != null) {
            filter = "&filter=ts,ge," + start + "&filter=ts,lt," + end;
        } else {
            filter = "&filter=ts,gt," + start;
        }

        smartdataurl += filter;

        SimpleResponse countResp = HttpService.get(smartdataurl);

        if (countResp.getStatus() != 200) {
            rob.setStatus(Response.Status.fromStatusCode(countResp.getStatus()));
            rob.addErrorMessage(countResp.readEntity(String.class));
            return rob.toResponse();
        }

        String respText = countResp.readEntity(String.class);

        JsonObject resp;
        try (JsonReader reader = Json.createReader(new StringReader(respText))) {
            resp = reader.readObject();
        }

        JsonArray records = resp.getJsonArray("records");
        if (records == null || records.isEmpty()) {
            rob.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
            rob.addErrorMessage("No records returned from SmartData.");
            return rob.toResponse();
        }

        JsonObject obj = records.getJsonObject(0);
        if (!obj.containsKey("count")) {
            rob.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
            rob.addErrorMessage("Count field missing in response.");
            return rob.toResponse();
        }

        int count = obj.getInt("count");

        rob.add("count", count);
        rob.setStatus(Response.Status.OK);
        return rob.toResponse();
    }
}
