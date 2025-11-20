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

    public static String SMARTDATA_BASE_URL = "http://localhost:8080/SmartDataAirquality/smartdata/records/";
    public static String SMARTDATA_STARTJOB_URL = "http://localhost:8080/SmartDataJobs/smartdatajobs/jobexecution/start?smartdataurl=/SmartDataAirquality";
    public static String STORAGE_GAMIFICATION = "?storage=gamification";
    public static String STORAGE_SMARTMONITORING = "?storage=smartmonitoring";
    private static final String DATAJOBS = "datajobs";
    private static final String DATAJOBS_PARAMS = "datajobs_params";
    private static final DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
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
            triggers.add(getTrigger(st.getInt("trigger_id"), st));
        }

        List<TriggerResult> sortedTriggers = triggers.stream()
                .sorted(Comparator.comparing(e -> e.next()))
                .collect(Collectors.toList());

        System.out.println("All: " + sortedTriggers);

        return sortedTriggers;
    }

    public static SimpleResponse createJobForTrigger(TriggerResult tr) {
        String createJobURL = SMARTDATA_BASE_URL
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

        SimpleResponse createJobResp = HttpService.post(createJobURL, jobBody);
        if (createJobResp.getStatus() != 201) {
            return new SimpleResponse(createJobResp.getStatus(), createJobResp.readEntity(String.class));
        }
        String jsonText = createJobResp.readEntity(String.class);
        int datajobId = Integer.parseInt(jsonText);
        String jobParamsURL = SMARTDATA_BASE_URL
                + DATAJOBS_PARAMS
                + STORAGE_SMARTMONITORING;

        String paramsBody = Json.createObjectBuilder()
                .add("key", "trigger_id")
                .add("value", tr.id())
                .add("datajob_id", datajobId)
                .add("type", "int")
                .build().toString();

        SimpleResponse jobParamsResp = HttpService.post(jobParamsURL, paramsBody);
        if (jobParamsResp.getStatus() != 201) {
            return new SimpleResponse(jobParamsResp.getStatus(), jobParamsResp.readEntity(String.class));
        }
        
        String startJobURL = SMARTDATA_STARTJOB_URL
                + "&" + STORAGE_SMARTMONITORING.substring(1)
                + "&collection=" + DATAJOBS
                + "&id=" + datajobId;
        
        SimpleResponse startJobResp = HttpService.get(startJobURL);
        if (startJobResp.getStatus() != 200) {
            return new SimpleResponse(startJobResp.getStatus(), startJobResp.readEntity(String.class));
        }
        
        return new SimpleResponse(startJobResp.getStatus(),
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
