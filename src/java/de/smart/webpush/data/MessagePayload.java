/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.webpush.data;

import java.util.Map;
import java.util.List;
/**
 *
 * @author steidlemax
 */
public class MessagePayload {
    public List<NotificationAction> actions;
    public List<Integer> vibration;
    public Map<String, Object> data;
    public String title;
    public String badge;
    public String image_url;
    public String body;
    public String icon_url;
    public String lang;
    public String tag;
    public boolean requireInteraction;
    public boolean renotify;
    public boolean silent;
    public long timestamp;
}
