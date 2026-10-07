package com.example.login.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppControllerAdvice {

    @Value("${app.nome:Sistema de Login}")
    private String appNome;

    @Value("${app.tema.nome:padrao}")
    private String temaNome;

    @ModelAttribute("appNome")
    public String appNome() {
        return appNome;
    }

    @ModelAttribute("temaNome")
    public String temaNome() {
        return temaNome;
    }
}
