package com.example.ordersystem.member.domain;

import com.example.ordersystem.common.domain.Address;
import com.example.ordersystem.member.dto.MemberListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Embedded
    private Address address;


    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    private LocalDateTime createdTime;

    @Builder
    public Member(String name, String email, String password, Address address, String city, String street, String zipcode, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.address = new Address(city, street, zipcode);
        this.role = role;
        this.createdTime = LocalDateTime.now();
    }

    public MemberListDto listFromEntity() {
        MemberListDto memberListDto = MemberListDto.builder().id(this.id).name(this.name).email(this.email).address(this.address).build();
        return memberListDto;
    }
}
