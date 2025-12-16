/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.webpush.data;

/**
 *
 * @author Hannes
 */
public class SimpleResponse {
    private final int status;
    private final String body;
    
    public SimpleResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
    
    public int getStatus() {
        return status;
    }
    
    public String readEntity(Class<String> type) {
        return body;
    }
}
