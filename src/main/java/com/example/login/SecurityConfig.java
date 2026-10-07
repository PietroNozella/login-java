package com.example.login;

import com.example.login.security.AutenticacaoHandlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AutenticacaoHandlers autenticacaoHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/cadastro", "/esqueci-senha", "/redefinir-senha/**",
                        "/termos", "/privacidade", "/403", "/error",
                        "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/usuarios/**", "/auditoria/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(autenticacaoHandlers.loginSucesso())
                .failureHandler(autenticacaoHandlers.loginFalha())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(autenticacaoHandlers.logout())
            )
            .exceptionHandling(e -> e.accessDeniedHandler(autenticacaoHandlers.acessoNegado()));
        return http.build();
    }
}
