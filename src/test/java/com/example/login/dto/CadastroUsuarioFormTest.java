package com.example.login.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.login.entity.Perfil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CadastroUsuarioFormTest {

    private static Validator validator;

    @BeforeAll
    static void configurar() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void deveRecusarFormularioVazio() {
        assertThat(validator.validate(new CadastroUsuarioForm())).hasSize(6);
    }

    @Test
    void deveRecusarEmailInvalidoESenhaCurta() {
        CadastroUsuarioForm form = formularioValido();
        form.setEmail("email-invalido");
        form.setSenha("1234567");

        assertThat(validator.validate(form)).hasSize(2);
    }

    @Test
    void deveAceitarFormularioValido() {
        assertThat(validator.validate(formularioValido())).isEmpty();
    }

    private CadastroUsuarioForm formularioValido() {
        CadastroUsuarioForm form = new CadastroUsuarioForm();
        form.setNome("Maria");
        form.setSobrenome("Silva");
        form.setEmail("maria@example.test");
        form.setUsername("maria");
        form.setSenha("senha-segura");
        form.setPerfil(Perfil.USUARIO);
        return form;
    }
}
