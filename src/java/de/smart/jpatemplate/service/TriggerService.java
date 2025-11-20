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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public static String SMARTDATA_BASE_URL = "http://localhost:8080/SmartDataAirquality/smartdata/records/";
    public static String STORAGE_GAMIFICATION = "?storage=gamification";
    public static String STORAGE_SMARTMONITORING = "?storage=smartmonitoring";
    private static final String DATAJOBS = "datajobs";
    private static final String DATAJOBS_PARAMS = "datajobs_params";
    private static final String TRIGGERS = "triggers";
    private static final DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .optionalEnd()
            .toFormatter();

    public static boolean triggerAlreadyExists(JsonObject payload) {

        String cron = (payload.getJsonString("cron") != null)
                ? payload.getString("cron")
                : null;

        String timestamp = (payload.getJsonString("time_once") != null)
                ? payload.getString("time_once")
                : null;

        String targetURL = SMARTDATA_BASE_URL
                + TRIGGERS
                + STORAGE_GAMIFICATION
                + "&filter=" + ((cron == null) ? "time_once" : "cron") + ",eq,"
                + ((cron == null) ? timestamp : cron);

        SimpleResponse resp = HttpService.get(targetURL);
        String jsonText = resp.readEntity(String.class);
        JsonObject root;
        try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            root = reader.readObject();
        }
        JsonArray records = root.getJsonArray("records");
        return records == null || !records.isEmpty();
    }
    
    public static SimpleResponse deleteTrigger(JsonObject payload){
        
        int triggerId = payload.getInt("id");
        
        String targetURL = SMARTDATA_BASE_URL
                + TRIGGERS
                + "/" + triggerId
                + STORAGE_SMARTMONITORING;
        
        return HttpService.delete(targetURL);
    }

    public static TriggerResult getTrigger(String idString, JsonObject payload) {

        int triggerId = payload.getInt(idString);

        String cron = (payload.getJsonString("cron") != null)
                ? payload.getString("cron")
                : null;

        String lastTriggeredAt = (payload.getJsonString("last_triggered_at") != null)
                ? payload.getString("last_triggered_at")
                : null;

        String timestamp = (payload.getJsonString("time_once") != null)
                ? payload.getString("time_once")
                : null;

        String timeStr = (lastTriggeredAt == null)
                ? timestamp
                : lastTriggeredAt;

        ZonedDateTime baseTime = (timeStr == null)
                ? ZonedDateTime.now()
                : ZonedDateTime.of(LocalDateTime.parse(timeStr, fmt), ZoneId.systemDefault());

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
        String targetURL = SMARTDATA_BASE_URL
                + "view_triggers_with_schedule"
                + STORAGE_GAMIFICATION;

        SimpleResponse resp = HttpService.get(targetURL);
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
            triggers.add(getTrigger("trigger_id", st));
        }

        List<TriggerResult> sortedTriggers = triggers.stream()
                .sorted(Comparator.comparing(e -> e.next()))
                .collect(Collectors.toList());

        System.out.println("All: " + sortedTriggers);

        return sortedTriggers;
    }

    public static SimpleResponse createJobForTrigger(TriggerResult tr) {
        String targetURL = SMARTDATA_BASE_URL
                + DATAJOBS
                + STORAGE_SMARTMONITORING;

        JsonObjectBuilder jsonJobBody = Json.createObjectBuilder()
                .add("name", "sendNotification")
                .add("action", "SendNotification")
                .add("active", true)
                .add("start", tr.next().toLocalDateTime().format(fmt));

        String jobBody = (tr.seconds() == 0)
                ? jsonJobBody.addNull("repeatsecs").build().toString()
                : jsonJobBody.add("repeatsecs", tr.seconds()).build().toString();

        SimpleResponse resp = HttpService.post(targetURL, jobBody);
        if (resp.getStatus() != 201) {
            return null;
        }
        String jsonText = resp.readEntity(String.class);

        targetURL = SMARTDATA_BASE_URL
                + DATAJOBS_PARAMS
                + STORAGE_SMARTMONITORING;

        String paramsBody = Json.createObjectBuilder()
                .add("key", "trigger_id")
                .add("value", tr.id())
                .add("datajob_id", Integer.parseInt(jsonText))
                .add("type", "int")
                .build().toString();

        resp = HttpService.post(targetURL, paramsBody);
        if (resp.getStatus() != 201) {
            return new SimpleResponse(resp.getStatus(), resp.readEntity(String.class));
        }
        return new SimpleResponse(resp.getStatus(),
                Json.createObjectBuilder()
                        .add(DATAJOBS, Json.createReader(new StringReader(jobBody)).readObject())
                        .add(DATAJOBS_PARAMS, Json.createReader(new StringReader(paramsBody)).readObject())
                        .build().toString());
    }

    public static Optional<TriggerResult> parseCron(String cronString, ZonedDateTime reference, int triggerId) {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

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
}
