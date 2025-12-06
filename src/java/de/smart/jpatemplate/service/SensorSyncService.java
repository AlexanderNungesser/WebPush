package de.smart.jpatemplate.service;

import de.smart.jpatemplate.data.SimpleResponse;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;

/*
This class processes given Sensors.
Responsibilities:
- create entrys in gamification.groups
- create webhooks to observe data-tables (smartmonitoring.<table-name>)
 */
public class SensorSyncService {

    public static final String jobNamePrefix = "eventTrigger_";

    /*
    The method processSensor synchronizes a given sensor to gamification and smartmonitoring
    Responsibilities:
    - create a group entry for the given sensor: gamification.groups
    - creates a webhook for table-observation via "PropertiesWebhookService"
    - create a SmartDataJob with params in smartmonitoring.datajobs / smartmonitoring.datajobs_params
     */
    public static void processSensor(JsonObject json) {
        final String name = json.getString("name", "");
        final String collection = json.getString("data_collection", "");
        final int ootype_id = json.getInt("ootype_id", 0);

        //only mobile sensors
        if (ootype_id != 3 && ootype_id != 4) {
            return;
        }

        //check gamification.groups
        int groupId = getGroupId(name, collection);
        if (groupId == -1) {
            groupId = createGroup(name, collection);
            if (groupId == -1) {
                return;
            }
        }

        //check datajobs
        String jobName = jobNamePrefix + name.replace(" ", "_");
        int existingJobId = getJobIdForGroups(groupId, jobName, collection);
        if (existingJobId == -1) {
            int newJobId = createEventJob(jobName, groupId, collection);
            if (newJobId == -1) {
                return;
            }
        }

        //edit SmartDataAirquality_config.properties
        try {
            PropertiesWebhookService.addMirroringEvent(collection, name);
        } catch (IOException ex) {
            System.err.println("WebPush - Error writing mirroring event: " + ex.getMessage());
            return;
        }
        System.out.println("WebPush -- Sensor '" + name + "' synchronized");
    }

    /*
    This method checks if a given group <name & collection> is already registered in the database gamification.groups
    @param name: String of the Sensor-Name
    @param collection: String of the data-table (smartmonitoring.<data-table>)
     */
    private static int getGroupId(String name, String collection) {
        String targetURL = HttpService.SmartDataRecordsApi + "group" + HttpService.StorageGamification;

        try {
            SimpleResponse response = HttpService.get(targetURL);
            if (response.getStatus() != 200) {
                return -1;
            }
            String jsonText = response.readEntity(String.class);
            try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
                JsonObject root = reader.readObject();
                JsonArray records = root.getJsonArray("records");
                if (records == null) {
                    return -1;
                }

                for (JsonObject obj : records.getValuesAs(JsonObject.class)) {
                    String existingName = obj.getString("name", "");
                    String existingTable = obj.getString("data_table", "");
                    if (existingName.equals(name) && existingTable.equals(collection)) {
                        return obj.getInt("id");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("WebPush - error in getGroupId: " + e.getMessage());
        }
        return -1;
    }

    /*
    This Method creates a new Group via SmartData
    @param name: String group-name
    @param collection: Sensor-table name
    return -1 if it fails
     */
    private static int createGroup(String name, String collection) {
        int groupId = -1;
        if (!sensorTableExists(collection)) {
            return groupId;
        }
        JsonObjectBuilder groupBuilder = Json.createObjectBuilder()
                .add("name", name)
                .add("data_table", collection);
        String lastActivity = getLastActivity(collection);
        if (lastActivity != null) {
            groupBuilder.add("last_activity", lastActivity);
        }
        JsonObject groupJson = groupBuilder.build();

        final String groupURL = HttpService.SmartDataRecordsApi + "group" + HttpService.StorageGamification;
        SimpleResponse resp = HttpService.post(groupURL, groupJson);
        if (resp.getStatus() != 201) {
            System.err.println("WebPush - Could not create a new group for sensor '" + name + "'. HTTP: " + resp.getStatus());
            return groupId;
        }
        String respbody = resp.readEntity(String.class).trim();
        groupId = Integer.parseInt(respbody);
        return groupId;
    }

    private static String getLastActivity(String sensorTable) {
        String lastTsUrl = HttpService.SmartDataRecordsApi + sensorTable + HttpService.StorageSmartmonitoring + "&includes=ts" + "&order=ts,DESC" + "&size=1";
        SimpleResponse resp = HttpService.get(lastTsUrl);
        JsonArray records = Json.createReader(new StringReader(
                resp.readEntity(String.class)
        )).readObject().getJsonArray("records");
        if (records == null || records.isEmpty()) {
            return null;
        }
        return records.getJsonObject(0).getString("ts");
    }

    private static boolean sensorTableExists(String sensorTable) {
        String checkUrl = HttpService.SmartDataCollectionApi + sensorTable + HttpService.StorageSmartmonitoring;
        SimpleResponse resp = HttpService.get(checkUrl);
        return resp.getStatus() != 404;
    }

    /*
    getJobIdForGroups checks if a job with the given name already exists
    It also checks if the job-parameter are already created
    @param groupId: int gamification.groupId
    @param jobName: Name of the job to be tested
    @param collection: String name of the sensor-table
     */
    private static int getJobIdForGroups(int groupId, String jobName, String collection) {
        try {
            //1)job by name
            int jobId = getJobIdByName(jobName);
            if (jobId == -1) {
                return -1;
            }

            //2) check parameter group_id
            boolean groupIdExists = isJobParameterExisting(jobId, "group_id", groupId);
            if (!groupIdExists) {
                return -1;
            }

            //3) check parameter 
            boolean SensorCollectionExists = isJobParameterExisting(jobId, "sensor_table", collection);
            if (!SensorCollectionExists) {
                return -1;
            }

            return jobId;
        } catch (Exception e) {
            System.err.println("WebPush - error in getJobIdForGroups: " + e.getMessage());
        }
        return -1;
    }

    public static int getJobIdByName(String jobName) {
        final String jobUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + HttpService.StorageSmartmonitoring
                + "&filter=name,eq," + jobName;
        System.out.println(jobUrl);

        SimpleResponse resp = HttpService.get(jobUrl);
        if (resp.getStatus() != 200) {
            return -1;
        }

        JsonObject root = Json.createReader(new StringReader(resp.readEntity(String.class))).readObject();
        JsonArray jobs = root.getJsonArray("records");
        if (jobs == null || jobs.isEmpty()) {
            return -1;
        }
        return jobs.getJsonObject(0).getInt("id");
    }

    /*
    helper function to check if a parameter already exists
    @param jobId: int id of the job
    @param key: String name of the parameter-key
    @param value: Object of the tested value
     */
    private static boolean isJobParameterExisting(int jobId, String key, Object value) {
        String paramsURL = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring
                + "&filter=datajob_id,eq," + jobId
                + "&filter=key,eq," + key
                + "&filter=value,eq," + value;

        SimpleResponse resp = HttpService.get(paramsURL);
        if (resp.getStatus() != 200) {
            return false;
        }

        JsonObject root = Json.createReader(new StringReader(resp.readEntity(String.class))).readObject();
        JsonArray parameter = root.getJsonArray("records");

        if (parameter == null || parameter.isEmpty()) {
            return false;
        }
        return true;
    }

    /*
    This Method handles the creation of a new Event-based job. 
    It creates via SmartData a new job entry and if its sucessfull two params will be added to the params table
    @param jobName: String of the new job-name
    @param groupId: Int reference on the gamification.group
    @param collection: String of the sensor-table of the given pi
    return -1 if it fails
     */
    private static int createEventJob(String jobName, int groupId, String collection) {
        int jobId = -1;

        // create a new job entry
        final String jobAction = "CheckEventTriggers";
        final String jobDesc = "Job for event-based Triggers";

        JsonObject newJob = Json.createObjectBuilder()
                .add("name", jobName)
                .add("desc", jobDesc)
                .add("action", jobAction)
                .add("active", true)
                .build();

        final String createJobUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + HttpService.StorageSmartmonitoring;

        SimpleResponse newJobResp = HttpService.post(createJobUrl, newJob);
        if (newJobResp.getStatus() != 201) {
            System.err.println("WebPush - Error while creating a new event-based job for: " + jobName);
            return -1;
        }
        String respbody = newJobResp.readEntity(String.class).trim();
        jobId = Integer.parseInt(respbody);
        if (jobId == -1) {
            return -1;
        }

        // create a new job-params entry
        final String jobParamsUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring;

        JsonObject groupIdParam = Json.createObjectBuilder()
                .add("key", "group_id")
                .add("value", groupId)
                .add("datajob_id", jobId)
                .add("type", "int")
                .build();
        JsonObject sensorParam = Json.createObjectBuilder()
                .add("key", "sensor_table")
                .add("value", collection)
                .add("datajob_id", jobId)
                .add("type", "string")
                .build();

        SimpleResponse jobParamsGroupResp = HttpService.post(jobParamsUrl, groupIdParam);
        if (jobParamsGroupResp.getStatus() != 201) {
            System.err.println("WebPush - Error while creating a new job-parameter -groupId- for: " + jobName);
            return -1;
        }
        SimpleResponse jobParamsSensorResp = HttpService.post(jobParamsUrl, sensorParam);
        if (jobParamsSensorResp.getStatus() != 201) {
            System.err.println("WebPush - Error while create a new job-parameter -sensor_table- for: " + jobName);
            return -1;
        }

        return jobId;
    }
}
