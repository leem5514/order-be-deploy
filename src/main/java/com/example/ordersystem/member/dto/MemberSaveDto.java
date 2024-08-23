package com.example.ordersystem.member.dto;

import com.example.ordersystem.common.domain.Address;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MemberSaveDto {
    private String name;
    @NotEmpty(message = "email is essential")
    private String email;

    @NotEmpty(message = "email is essential")
    //@Size(min = 8, message = "password minimum length is 8")
    private String password;

    private Address address;
//    private String city;
//    private String street;
//    private String zipcode;
    private Role role = Role.USER;

    public Member toEntity(String password) {
        Member member = Member.builder()
                .name(this.name)
                .email(this.email)
                .password(password)
//                .address(Address.builder().city(this.city).street(this.street).zipcode(this.zipcode).build())
                .address(this.address)
                .role(this.role)
                .build();
        return member;
    }
}
