package com.example.login.controller;

import com.example.login.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Consulta de auditoria: somente leitura, restrita ao Administrador
// (regra também no SecurityConfig).
@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("registros", auditoriaService.listar());
        return "auditoria";
    }
}
