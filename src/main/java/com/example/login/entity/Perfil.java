package com.example.login.entity;

public enum Perfil {
    ADMINISTRADOR,
    USUARIO;

    public String autoridade() {
        return "ROLE_" + name();
    }
}
