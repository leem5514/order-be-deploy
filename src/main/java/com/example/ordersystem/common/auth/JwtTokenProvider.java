package com.example.ordersystem.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private int expiration;

    @Value("${jwt.expirationRt}")
    private int expirationRt;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    public String createToken(String email, String role) {
        // claims 는 사용자 정보이자 (페이로드 정보)
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);
        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now) // 생성일
                .setExpiration(new Date(now.getTime() + 30 * 60 * 1000L)) // 만료 시간 (밀리 초 단위로 변환) : 30분으로 세팅.
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
        return token;
    }

    public String createRefreshToken(String email, String role){
        // claims 는 사용자 정보 (페이로드 정보)
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);
        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now) // 생성 시간
                .setExpiration(new Date(now.getTime() + expirationRt * 60 * 1000L)) // 만료 시간 (밀리 초 단위로 변환) : 30분으로 세팅.
                .signWith(SignatureAlgorithm.HS256, secretKeyRt)
                .compact();
        return token;
    }
}
