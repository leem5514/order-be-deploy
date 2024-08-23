package com.example.ordersystem.common.configs;


import com.example.ordersystem.common.auth.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) // PRE : 사전, POST: 사후 검증
public class SecurityConfigs {
    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                .csrf().disable()
                .cors().and() // CORS 활성화
                .httpBasic().disable()
                .authorizeRequests()
                // 자격 부여할 때 여기만 손 대면 되니까 주로 여기만 수정하게 됨.
                .antMatchers("/", "/member/create", "/doLogin", "/member/refresh-token" , "/product/list")
                .permitAll()
                .anyRequest().authenticated()
                .and()
                // 세션 로그인이 아닌 stateless 한 token 을 사용하겠다라는 의미
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 사용자로부터 받아온 토큰이 정상인지 아닌지를 검증하는 코드.
                // 로그인 시 사용자는 서버로부터 토큰을 발급 받고, 매 요청마다 해당 토큰을 http header 에 넣어 요청한다.
                // 아래 코드는 사용자로부터 받아온 토큰이 정상인지 아닌지를 검증하는 코드.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
