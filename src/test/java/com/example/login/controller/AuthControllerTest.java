package com.example.login.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.login.dto.CadastroPublicoForm;
import com.example.login.entity.Perfil;
import com.example.login.entity.RegistroAuditoria;
import com.example.login.entity.Usuario;
import com.example.login.service.AuditoriaService;
import com.example.login.service.PasswordResetEmailService;
import com.example.login.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private PasswordResetEmailService passwordResetEmailService;
    @InjectMocks private AuthController controller;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers((name, locale) -> {
                    if (name.startsWith("redirect:")) return new RedirectView(name.substring(9));
                    return new AbstractView() {
                        @Override
                        protected void renderMergedOutputModel(Map<String, Object> model,
                                HttpServletRequest request, HttpServletResponse response) { }
                    };
                }).build();
    }

    @Test
    void deveExibirFormularioPublicoSemPerfil() throws Exception {
        mvc.perform(get("/cadastro"))
                .andExpect(view().name("cadastro"))
                .andExpect(model().attribute("form", org.hamcrest.Matchers.instanceOf(CadastroPublicoForm.class)));
    }

    @Test
    void deveIgnorarPrivilegiosEnviadosPeloVisitanteEAuditarSemPrincipal() throws Exception {
        Usuario salvo = new Usuario();
        salvo.setId("usuario-1");
        salvo.setUsername("maria");
        salvo.setPerfil(Perfil.USUARIO);
        when(usuarioService.cadastrarPublico("Maria", "Silva", "maria@example.test", "maria", "senha-segura"))
                .thenReturn(salvo);

        mvc.perform(formulario().param("perfil", "ADMINISTRADOR").param("ativo", "false"))
                .andExpect(redirectedUrl("/login?cadastrado"));

        verify(usuarioService).cadastrarPublico("Maria", "Silva", "maria@example.test", "maria", "senha-segura");
        verify(auditoriaService).registrar(eq("maria"), eq(RegistroAuditoria.Acao.USUARIO_CADASTRADO),
                eq(RegistroAuditoria.Resultado.SUCESSO), eq("Usuario"), eq("usuario-1"), anyString());
    }

    @Test
    void deveRecusarCadastroInvalidoAntesDeSalvar() throws Exception {
        mvc.perform(post("/cadastro").param("email", "invalido").param("senha", "123"))
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "nome", "sobrenome", "email", "username", "senha"));

        verify(usuarioService, never()).cadastrarPublico(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deveValidarCamposAposRemoverEspacos() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "   ").param("username", " a ").param("sobrenome", "Silva").param("email", "maria@example.test").param("senha", "senha-segura"))
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "nome", "username"));
    }

    @Test
    void deveMostrarDuplicidadeNoFormulario() throws Exception {
        when(usuarioService.cadastrarPublico(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("E-mail já cadastrado."));

        mvc.perform(formulario())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attribute("erro", "E-mail já cadastrado."));
    }

    private MockHttpServletRequestBuilder formulario() {
        return post("/cadastro")
                .param("nome", "Maria").param("sobrenome", "Silva")
                .param("email", "maria@example.test").param("username", "maria")
                .param("senha", "senha-segura");
    }
}
