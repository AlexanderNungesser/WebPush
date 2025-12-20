package de.smart.webpush.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.smart.webpush.data.SimpleResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TriggerService {

    private static final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    /**
     * Get a scheduled trigger from a <code>JsonObject</code>
     *
     * @param trigger <code>JsonObject</code> of the trigger
     * @return <code>JsonObject</code> of the scheduled trigger
     */
    public static JsonObject getScheduledTrigger(JsonObject trigger) {

        String cron = trigger.getString("cron", null);

        String timeOnce = trigger.getString("time_once", null);

        JsonObjectBuilder schedule = Json.createObjectBuilder();

        if (timeOnce != null) {
            schedule.add("next", LocalDateTime.parse(timeOnce).toString())
                    .add("seconds", 0);
        } else if (cron != null) {
            schedule.addAll(TriggerService.parseCron(cron));
        }
        return merge(trigger, schedule.build());
    }

    // Merges two JsonObject's into one
    private static JsonObject merge(JsonObject original, JsonObject updates) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        original.forEach(builder::add);
        updates.forEach(builder::add);

        return builder.build();
    }

    /**
     * Get <code>JsonObject</code> of all schedueld triggers
     *
     * @return List of <code>JsonObject</code>
     */
    public static List<JsonObject> getScheduledTriggers() {
        final String triggerGetUrl = HttpService.SmartDataRecordsApi
                + "view_triggers_with_schedule"
                + HttpService.StorageGamification;

        SimpleResponse resp = HttpService.get(triggerGetUrl);
        if (resp.getStatus() != 200) {
            return null;
        }
        String jsonText = resp.readEntity(String.class);

        JsonObject root;
        try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            root = reader.readObject();
        }
        JsonArray scheduledtriggers = root.getJsonArray("records");

        if (scheduledtriggers == null || scheduledtriggers.isEmpty()) {
            return null;
        }

        List<JsonObject> triggers = new ArrayList<>();

        for (JsonObject scheduledTrigger : scheduledtriggers.getValuesAs(JsonObject.class)) {
            triggers.add(getScheduledTrigger(scheduledTrigger));
        }

        List<JsonObject> sortedTriggers = triggers.stream()
                .sorted(Comparator.comparing(e -> LocalDateTime.parse(e.getString("next"))))
                .collect(Collectors.toList());

        return sortedTriggers;
    }

    /**
     * Check if a job for a scheduled trigger already exists
     *
     * @param triggerId of the scheduled trigger that should be checked
     * @return <code>true</code> if a job already exists, otherwise
     * <code>false</code>
     */
    public static boolean jobAlreadyExists(int triggerId) {
        return (0 != getJobId(triggerId));
    }

    /**
     * Create a job for a scheduled trigger
     *
     * @param triggerId of the scheduled trigger
     * @param trigger <code>JsonObject</code> of the scheduled trigger
     * @return <code>SimpleResponse</code> of the creation process
     */
    public static SimpleResponse createJobForScheduledTrigger(int triggerId, JsonObject trigger) {
        final String createJobUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + HttpService.StorageSmartmonitoring;

        JsonObjectBuilder jsonJobBody = Json.createObjectBuilder()
                .add("name", "timeTrigger_" + triggerId)
                .add("desc", "Job for time-based Trigger")
                .add("action", "SendNotification")
                .add("active", true)
                .add("start", trigger.getString("next"));

        JsonObject jobBody = (trigger.getInt("seconds") == 0)
                ? jsonJobBody.addNull("repeatsecs").build()
                : jsonJobBody.add("repeatsecs", trigger.getInt("seconds")).build();

        SimpleResponse createJobResp = HttpService.post(createJobUrl, jobBody);
        if (createJobResp.getStatus() != 201) {
            return new SimpleResponse(createJobResp.getStatus(), createJobResp.readEntity(String.class));
        }
        String jsonText = createJobResp.readEntity(String.class);
        int datajobId = Integer.parseInt(jsonText);
        final String jobParamsUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring;

        JsonObject paramsBody = Json.createObjectBuilder()
                .add("key", "trigger_id")
                .add("value", triggerId)
                .add("datajob_id", datajobId)
                .add("type", "int")
                .build();

        SimpleResponse jobParamsResp = HttpService.post(jobParamsUrl, paramsBody);
        if (jobParamsResp.getStatus() != 201) {
            return new SimpleResponse(jobParamsResp.getStatus(), jobParamsResp.readEntity(String.class));
        }

        SimpleResponse registerJobResp = registerJob(datajobId);

        if (registerJobResp.getStatus() != 200) {
            return new SimpleResponse(registerJobResp.getStatus(), registerJobResp.readEntity(String.class));
        }

        return new SimpleResponse(registerJobResp.getStatus(),
                Json.createObjectBuilder()
                        .add(HttpService.DataJobs, Json.createReader(new StringReader(jobBody.toString())).readObject())
                        .add(HttpService.DataJobsParams, Json.createReader(new StringReader(paramsBody.toString())).readObject())
                        .build().toString());
    }

    //Register a Job with datajobId to the JobRunner of SmartDataJobs by starting it
    private static SimpleResponse registerJob(int datajobId) {
        String startJobURL = HttpService.SmartDataJobsApi
                + "start"
                + HttpService.SmartDataUrl
                + "&" + HttpService.StorageSmartmonitoring.substring(1)
                + "&collection=" + HttpService.DataJobs
                + "&id=" + datajobId;

        return HttpService.get(startJobURL);
    }

    /**
     * Delete a trigger from the DB defined in a <code>JsonObject</code>
     *
     * @param triggerId of the trigger
     * @return <code>SimpleResponse</code> of the deletion process
     */
    public static SimpleResponse deleteTrigger(int triggerId) {
        final String deleteTriggerURL = HttpService.SmartDataRecordsApi
                + "trigger"
                + "/" + triggerId
                + HttpService.StorageGamification;
        SimpleResponse deleteTriggerResp = HttpService.delete(deleteTriggerURL);
        if (deleteTriggerResp.getStatus() != 200) {
            return new SimpleResponse(deleteTriggerResp.getStatus(), deleteTriggerResp.readEntity(String.class));
        }

        int dataJobId = getJobId(triggerId);
        if (dataJobId == 0) {
            return new SimpleResponse(200, deleteTriggerResp.readEntity(String.class));
        } else if (dataJobId == -1) {
            return new SimpleResponse(500, "ERROR FETCHING JOBID OF TRIGGER " + triggerId);
        }
        SimpleResponse deactivateJobResp = deactivateJob(dataJobId);
        if (deactivateJobResp.getStatus() != 200) {
            return new SimpleResponse(deactivateJobResp.getStatus(), deactivateJobResp.readEntity(String.class));
        }

        return new SimpleResponse(deactivateJobResp.getStatus(),
                Json.createObjectBuilder()
                        .add("trigger_id", triggerId)
                        .add("datajob_id", dataJobId)
                        .build().toString());
    }

    // Deactivates a Job
    private static SimpleResponse deactivateJob(int dataJobId) {
        final String deactivateJobUrl = HttpService.SmartDataJobsApi
                + "deactivate"
                + HttpService.SmartDataUrl
                + "&" + HttpService.StorageSmartmonitoring.substring(1)
                + "&collection=" + HttpService.DataJobs
                + "&id=" + dataJobId;
        return HttpService.get(deactivateJobUrl);
    }

    /**
     * Get the job id of a trigger's job via the <code>triggerId</code>
     *
     * @param triggerId of the job's trigger
     * @return job id if succesfull, -1 if an error occured and 0 if no job was
     * found
     */
    public static int getJobId(int triggerId) {
        final String jobParamsUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring
                + "&filter=key,eq,trigger_id"
                + "&filter=value,eq," + triggerId;

        SimpleResponse jobParamsResp = HttpService.get(jobParamsUrl);
        if (jobParamsResp.getStatus() != 200) {
            return -1;
        }
        String respText = jobParamsResp.readEntity(String.class);

        JsonObject root;
        try (JsonReader reader = Json.createReader(new StringReader(respText))) {
            root = reader.readObject();
        }

        JsonArray records = root.getJsonArray("records");
        if (records == null || records.isEmpty()) {
            return 0;
        }
        return records.getJsonObject(0).getInt("datajob_id", 0);
    }

    // Parse a CRON-String with a reference time
    private static JsonObjectBuilder parseCron(String cronString) {
        try {
            Cron cron = parser.parse(cronString);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime reference = ZonedDateTime.now();
            ZonedDateTime next = executionTime.nextExecution(reference).orElseThrow();
            ZonedDateTime prev = executionTime.lastExecution(reference).orElseThrow();
            long seconds = Duration.between(prev, next).getSeconds();

            return Json.createObjectBuilder()
                    .add("next", next.toLocalDateTime().toString())
                    .add("seconds", seconds);

        } catch (Exception e) {
            e.printStackTrace();
            return Json.createObjectBuilder();
        }
    }

    /**
     * Check if a CRON-String is valid
     *
     * @param cronString that should be checked
     * @return <code>true</code> if its valid, otherwise <code>false</code>
     */
    public static boolean isValidCron(String cronString) {
        try {
            Cron cron = parser.parse(cronString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
