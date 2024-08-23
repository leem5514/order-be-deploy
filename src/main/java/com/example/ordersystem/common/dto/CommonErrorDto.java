package com.example.ordersystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonErrorDto {
    private int status_code;
    private String error_message;

}
