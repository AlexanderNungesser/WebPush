package de.smart.jpatemplate.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.smart.jpatemplate.data.SimpleResponse;
import de.smart.jpatemplate.data.TriggerResult;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TriggerService {

    private static final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    private static final DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .optionalEnd()
            .toFormatter();

    /**
     * Get a <code>TriggerResult</code> with <code>triggerId</code> from a
     * <code>payload</code>
     *
     * @param triggerId of the trigger
     * @param payload of the trigger
     * @return <code>TriggerResult</code> of the trigger
     */
    public static TriggerResult getTrigger(int triggerId, JsonObject payload) {

        String cron = (payload.getJsonString("cron") != null)
                ? payload.getString("cron")
                : null;

        String timeOnce = (payload.getJsonString("time_once") != null)
                ? payload.getString("time_once")
                : null;

        ZonedDateTime baseTime = (timeOnce == null)
                ? ZonedDateTime.now()
                : ZonedDateTime.of(LocalDateTime.parse(timeOnce, fmt), ZoneId.systemDefault());

        TriggerResult tr;

        if (cron == null) {
            tr = new TriggerResult(triggerId, baseTime, 0);
        } else {
            tr = TriggerService.parseCron(cron, baseTime, triggerId)
                    .orElseThrow(() -> new IllegalStateException("Cron konnte nicht geparst werden"));
        }
        return tr;
    }

    /**
     * Get <code>TriggerResult</code> of all schedueld triggers
     *
     * @return List of <code>TriggerResult</code>
     */
    public static List<TriggerResult> getTriggers() {
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

        List<TriggerResult> triggers = new ArrayList<>();

        for (JsonObject st : scheduledtriggers.getValuesAs(JsonObject.class)) {
            triggers.add(getTrigger(st.getInt("trigger_id"), st));
        }

        List<TriggerResult> sortedTriggers = triggers.stream()
                .sorted(Comparator.comparing(e -> e.next()))
                .collect(Collectors.toList());

        return sortedTriggers;
    }

    /**
     * Check if a job for a trigger already exists
     *
     * @param trigger that should be checked
     * @return <code>true</code> if a job already exists, otherwise
     * <code>false</code>
     */
    public static boolean jobAlreadyExists(TriggerResult trigger) {
        return (0 != getJobId(trigger.id()));
    }

    /**
     * Create a job for a scheduled trigger defined in a
     * <code>TriggerResult</code>
     *
     * @param tr <code>TriggerResult</code> of the trigger
     * @return <code>SimpleResponse</code> of the creation process
     */
    public static SimpleResponse createJobForTrigger(TriggerResult tr) {
        final String createJobUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + HttpService.StorageSmartmonitoring;

        JsonObjectBuilder jsonJobBody = Json.createObjectBuilder()
                .add("name", "timeTrigger_" + tr.id())
                .add("desc", "Job for time-based Trigger")
                .add("action", "SendNotification")
                .add("active", true)
                .add("start", tr.next().toLocalDateTime().format(fmt));

        JsonObject jobBody = (tr.seconds() == 0)
                ? jsonJobBody.addNull("repeatsecs").build()
                : jsonJobBody.add("repeatsecs", tr.seconds()).build();

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
                .add("value", tr.id())
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

    /**
     * Register a Job with <code>datajobId</code> to the <code>JobRunner</code>
     * of SmartDataJobs by starting it
     *
     * @param datajobId of the job that should be registerd
     * @return <code>SimpleResponse</code> of the register process
     */
    public static SimpleResponse registerJob(int datajobId) {
        String startJobURL = HttpService.SmartDataJobsApi
                + "&" + HttpService.StorageSmartmonitoring.substring(1)
                + "&collection=" + HttpService.DataJobs
                + "&id=" + datajobId;

        return HttpService.get(startJobURL);
    }

    /**
     * Delete a trigger from the DB defined in a <code>payload</code>
     *
     * @param payload of the trigger
     * @return <code>SimpleResponse</code> of the deletion process
     */
    public static SimpleResponse deleteTrigger(JsonObject payload) {

        int triggerId = payload.getInt("id");

        final String deleteTriggerURL = HttpService.SmartDataRecordsApi
                + "triggers"
                + "/" + triggerId
                + HttpService.StorageGamification;

        SimpleResponse deleteTriggerResp = HttpService.delete(deleteTriggerURL);

        if (deleteTriggerResp.getStatus() != 200) {
            return new SimpleResponse(deleteTriggerResp.getStatus(), deleteTriggerResp.readEntity(String.class));
        }

        int dataJobId = getJobId(triggerId);

        final String deleteJobURL = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + "/" + dataJobId
                + HttpService.StorageGamification;

        SimpleResponse deleteJobResp = HttpService.delete(deleteJobURL);

        if (deleteJobResp.getStatus() != 200) {
            return new SimpleResponse(deleteJobResp.getStatus(), deleteJobResp.readEntity(String.class));
        }

        return new SimpleResponse(deleteJobResp.getStatus(),
                Json.createObjectBuilder()
                        .add("trigger_id", triggerId)
                        .add("datajob_id", dataJobId)
                        .build().toString());
    }

    /**
     * Get the job id of a trigger's job via the <code>triggerId</code>
     *
     * @param triggerId of the job's trigger
     * @return job id
     */
    public static int getJobId(int triggerId) {
        final String jobParamsUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring
                + "&filter=key,eq,trigger_id"
                + "&filter=value,eq," + triggerId;

        SimpleResponse jobParamsResp = HttpService.get(jobParamsUrl);
        if (jobParamsResp.getStatus() != 200) {
            return 0;
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

    /**
     * Parse a CRON-String with a <code>reference</code> time of a trigger with
     * the <code>triggerId</code>
     *
     * @param cronString that should be parsed
     * @param reference from which the next execution is calculated
     * @param triggerId of the trigger
     * @return <code>Optional</code> of <code>TriggerResult</code>
     */
    public static Optional<TriggerResult> parseCron(String cronString, ZonedDateTime reference, int triggerId) {
        try {
            Cron cron = parser.parse(cronString);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime next = executionTime.nextExecution(reference).orElseThrow();
            ZonedDateTime prev = executionTime.lastExecution(reference).orElseThrow();
            long seconds = Duration.between(prev, next).getSeconds();
            return Optional.of(new TriggerResult(triggerId, next, seconds));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
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
