/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data.ManagementDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 *
 * @author Hannes
 */
public class TriggerDTO {
    @NotBlank
    public String type;
    public Map<String, Object> config;
    @NotNull
    public Boolean active;
}
