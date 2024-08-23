package com.example.ordersystem.member.dto;

import com.example.ordersystem.common.domain.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberListDto {
    private Long id;
    private String name;
    private String email;
    private Address address;
}
