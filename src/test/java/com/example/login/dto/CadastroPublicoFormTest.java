package com.example.login.dto;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class CadastroPublicoFormTest {
    @Test
    void deveValidarCamposSemExigirPerfil() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var form = new CadastroPublicoForm();
            assertThat(validator.validate(form)).hasSize(5);
            form.setNome("Maria");
            form.setSobrenome("Silva");
            form.setEmail("maria@example.test");
            form.setUsername("maria");
            form.setSenha("senha-segura");
            assertThat(validator.validate(form)).isEmpty();
        }
    }
}
