package com.example.ordersystem.member.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.MemberListDto;
import com.example.ordersystem.member.dto.MemberLoginDto;
import com.example.ordersystem.member.dto.MemberSaveDto;
import com.example.ordersystem.member.repository.MemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member memberCreate(MemberSaveDto dto) {

        if (memberRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다");
        }
        if (dto.getPassword().length() < 8){
            throw new IllegalArgumentException("비밀번호의 길이가 짧습니다.");
        }
        Member savedMember = memberRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        return savedMember;
    }

    public Page<MemberListDto> memberList(Pageable pageable) {
        Page<Member> memberListDtos = memberRepository.findAll(pageable);
        return memberListDtos.map(a -> a.listFromEntity());
    }

    public Member login(MemberLoginDto dto) {
        // email 존재 여부
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 이메일입니다."));

        // pwd 일치 여부 ) 들어온 dto 의 password 를 암호화해서 암호화 된 DB의 데이터와 비교.
        if(!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
        return member;
    }

    public MemberListDto myInfo() {
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().toString()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 이메일입니다."));
        return member.listFromEntity();
    }
}
