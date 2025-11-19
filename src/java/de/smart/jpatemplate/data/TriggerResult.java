package de.smart.jpatemplate.data;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public record TriggerResult(int id, ZonedDateTime next, long seconds) {
}
