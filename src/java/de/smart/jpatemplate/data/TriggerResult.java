package de.smart.jpatemplate.data;

import java.time.ZonedDateTime;

/**
 * Result of a scheduled trigger after the CRON-String parsing with the
 * following params: <br> - {@link #id} of the scheduled trigger <br> -
 * {@link #next} execution time of the scheduled trigger <br> - {@link #seconds}
 * after the scheduled trigger should be restartet <br>
 */
public record TriggerResult(int id, ZonedDateTime next, long seconds) {

}
