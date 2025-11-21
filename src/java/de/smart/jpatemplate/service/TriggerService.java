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
import java.math.BigDecimal;
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
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .optionalEnd()
            .toFormatter();

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

        System.out.println("All: " + sortedTriggers);

        return sortedTriggers;
    }
    
    public static boolean jobAlreadyExists(TriggerResult trigger) {
        final String jobParamsUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobsParams
                + HttpService.StorageSmartmonitoring
                + "&filter=key,eq,trigger_id"
                + "&filter=value,eq," + trigger.id();
        
        SimpleResponse jobParamsResp = HttpService.get(jobParamsUrl);
        if (jobParamsResp.getStatus() != 200) {
            return true;
        }
        String respText = jobParamsResp.readEntity(String.class);
        
        JsonObject root;
        try (JsonReader reader = Json.createReader(new StringReader(respText))) {
            root = reader.readObject();
        }
        JsonArray scheduledtriggers = root.getJsonArray("records");

        if (scheduledtriggers == null || scheduledtriggers.isEmpty()) {
            return false;
        }
        return true;
    }
    
    public static SimpleResponse createJobForTrigger(TriggerResult tr) {
        final String createJobUrl = HttpService.SmartDataRecordsApi
                + HttpService.DataJobs
                + HttpService.StorageSmartmonitoring;

        JsonObjectBuilder jsonJobBody = Json.createObjectBuilder()
                .add("name", "sendNotification")
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
        
        String startJobURL = HttpService.SmartDataJobsApi
                + "&" + HttpService.StorageSmartmonitoring.substring(1)
                + "&collection=" + HttpService.DataJobs
                + "&id=" + datajobId;

        SimpleResponse startJobResp = HttpService.get(startJobURL);
        if (startJobResp.getStatus() != 200) {
            return new SimpleResponse(startJobResp.getStatus(), startJobResp.readEntity(String.class));
        }

        return new SimpleResponse(startJobResp.getStatus(),
                Json.createObjectBuilder()
                        .add(HttpService.DataJobs, Json.createReader(new StringReader(jobBody.toString())).readObject())
                        .add(HttpService.DataJobsParams, Json.createReader(new StringReader(paramsBody.toString())).readObject())
                        .build().toString());
    }

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
    public static boolean isValidCron(String cronString) {
        try {
            Cron cron = parser.parse(cronString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
