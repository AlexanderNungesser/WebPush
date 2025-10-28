package de.smart.jpatemplate.data;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for storing PushSubscriptions.
 */
public class PushStorage {
    private static final List<PushSubscription> subscriptions =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Add a new subscription.
     * @param sub the subscription
     */
    public static void add(PushSubscription sub) {
        if (sub != null && sub.getEndpoint() != null) {
            boolean exists = subscriptions.stream()
                    .anyMatch(s -> s.getEndpoint().equals(sub.getEndpoint()));
            if (!exists) {
                subscriptions.add(sub);
                System.out.println("Saved new PushSubscription: " + sub.getEndpoint());
            }
        }
    }

    /**
     * Get all current subscriptions.
     * @return a list containing all subscriptions
     */
    public static List<PushSubscription> getAll() {
        return subscriptions;
    }

    /**
     * Clear current subscriptions.
     */
    public static void clear() {
        subscriptions.clear();
    }
}

