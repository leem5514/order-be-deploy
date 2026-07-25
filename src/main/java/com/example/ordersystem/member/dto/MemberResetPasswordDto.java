package com.example.ordersystem.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResetPasswordDto {
    @NotEmpty(message = "email is essential")
    private String email;

    @NotEmpty(message = "asIsPassword is essential")
    private String asIsPassword;

    @NotEmpty(message = "toBePassword is essential")
    private String toBePassword;
}
