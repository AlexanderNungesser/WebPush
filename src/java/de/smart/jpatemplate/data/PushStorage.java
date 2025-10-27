/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import java.util.*;
/**
 *
 * @author maxst
 */
public class PushStorage {
    // Thread-sichere Liste für parallele Zugriffe
    private static final List<PushSubscription> subscriptions =
            Collections.synchronizedList(new ArrayList<>());

    // Neue Subscription speichern (wenn noch nicht vorhanden)
    public static void add(PushSubscription sub) {
        if (sub != null && sub.getEndpoint() != null) {
            boolean exists = subscriptions.stream()
                    .anyMatch(s -> s.getEndpoint().equals(sub.getEndpoint()));
            if (!exists) {
                subscriptions.add(sub);
                System.out.println("Neue PushSubscription gespeichert: " + sub.getEndpoint());
            }
        }
    }

    // Alle gespeicherten Subscriptions abrufen
    public static List<PushSubscription> getAll() {
        return subscriptions;
    }

    // Alle Subscriptions löschen (z. B. beim Server-Neustart)
    public static void clear() {
        subscriptions.clear();
    }
}

