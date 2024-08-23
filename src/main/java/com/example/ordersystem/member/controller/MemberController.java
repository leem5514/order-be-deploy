package com.example.ordersystem.member.controller;

import com.example.ordersystem.common.auth.JwtTokenProvider;
import com.example.ordersystem.common.dto.CommonErrorDto;
import com.example.ordersystem.common.dto.CommonResDto;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.MemberListDto;
import com.example.ordersystem.member.dto.MemberLoginDto;
import com.example.ordersystem.member.dto.MemberRefreshDto;
import com.example.ordersystem.member.dto.MemberSaveDto;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//@RequestMapping("/member")
@RestController
public class MemberController {
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("2")
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    @Autowired
    public MemberController(MemberService memberService, MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider, @Qualifier("2") RedisTemplate<String, Object> redisTemplate) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

//    @GetMapping("/member/create")
//    public String memberCreateForm() {
//        return "member/create";
//    }

    @PostMapping("/member/create") // 생성
    public ResponseEntity<Object> memberCreate(@Valid @RequestBody MemberSaveDto dto) {
        Member member = memberService.memberCreate(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "success created", member.getId());

        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }

    // admin 만 회원 전체 목록 조회 가능
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/member/list")
    public ResponseEntity<?> memberList(Pageable pageable) {
        Page<MemberListDto> listDto = memberService.memberList(pageable);
        memberService.memberList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "회원 목록 조회 성공 !", listDto);
        ResponseEntity<CommonResDto> result = new ResponseEntity<>(commonResDto, HttpStatus.OK);
        return result;
    }

    // 본인은 본인 회원 정보만 조회 가능
    @GetMapping("/member/myinfo")
    public ResponseEntity memberMyInfo() {
        MemberListDto memberListDto = memberService.myInfo();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "회원 조회 성공", memberListDto);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto dto) {
        // email , pwd 일치 검증
        Member member = memberService.login(dto);
        // if 일치할경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());
        // 생성된 토큰을 CommonResDto 에 담아서 사용자에게 return
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString());

        // redis에 email 과 rt을 키 벨류로 형태 저장
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 240, TimeUnit.HOURS);

        // 생성된 토큰을 CommonResDto 에 담아 사용자에게 return.
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("token", jwtToken);
        loginInfo.put("refreshToken", refreshToken);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "로그인 성공 !", loginInfo);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<?> generateNewAccessToken(@RequestBody MemberRefreshDto dto){
        String rt = dto.getRefreshToken();
        Claims claims = null;
        try{
            // 코드를 통해 rt 검증
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody(); //getBody 는 payload 에 들어있는 것.
        }
        catch (Exception e){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(),"invalid refresh token"),HttpStatus.UNAUTHORIZED);
        }

        String email = claims.getSubject();
        String role = claims.get("role").toString();

        // redis 를 통한 rt 추가 검증 조회
        Object obj = redisTemplate.opsForValue().get(email);
        if(obj == null || !obj.toString().equals(rt)){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(),"invalid refresh token"),HttpStatus.UNAUTHORIZED);
        }

        String newAt = jwtTokenProvider.createToken(email, role);

        // 생성된 토큰을 CommonResDto 에 담아 사용자에게 return.
        Map<String, Object> info = new HashMap<>();
        info.put("token", newAt);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "AT is renewed", info);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

}
