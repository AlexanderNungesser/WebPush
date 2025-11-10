/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data.ManagementDTOs;

import jakarta.validation.constraints.*;
/**
 *
 * @author Hannes
 */
public class NotificationDTO {
    @NotBlank
    public String title;
    
    public String body;
    public String icon_url;
    public String image_url;
    public Boolean renotify;
    public Boolean silent;
    
    @NotNull
    @Min(1)
    public Integer trigger_id;
}
