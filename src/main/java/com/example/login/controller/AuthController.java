package com.example.login.controller;

import com.example.login.dto.CadastroPublicoForm;
import com.example.login.dto.CadastroUsuarioForm;
import com.example.login.entity.Perfil;
import com.example.login.entity.RegistroAuditoria;
import com.example.login.service.AuditoriaService;
import com.example.login.service.PasswordResetEmailService;
import com.example.login.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private PasswordResetEmailService passwordResetEmailService;

    @InitBinder("form")
    public void prepararFormulario(WebDataBinder binder) {
        for (String campo : new String[] {"nome", "sobrenome", "email", "username"}) {
            binder.registerCustomEditor(String.class, campo, new StringTrimmerEditor(true));
        }
    }

    @GetMapping("/cadastro")
    public String cadastroForm(Model model) {
        model.addAttribute("form", new CadastroPublicoForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastro(
            @Valid @ModelAttribute("form") CadastroPublicoForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "cadastro";
        }
        try {
            var usuario = usuarioService.cadastrarPublico(
                    form.getNome(), form.getSobrenome(), form.getEmail(),
                    form.getUsername(), form.getSenha());
            auditoriaService.registrar(usuario.getUsername(), RegistroAuditoria.Acao.USUARIO_CADASTRADO,
                    RegistroAuditoria.Resultado.SUCESSO, "Usuario", usuario.getId(),
                    AuditoriaService.ip(request));
        } catch (IllegalArgumentException e) {
            auditoriaService.registrar(form.getUsername(), RegistroAuditoria.Acao.USUARIO_CADASTRADO,
                    RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
            model.addAttribute("erro", e.getMessage());
            return "cadastro";
        }
        return "redirect:/login?cadastrado";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String acessoNegado() {
        return "403";
    }

    // --- Cadastro controlado: só ADMINISTRADOR (regra também no SecurityConfig) ---
    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("form", new CadastroUsuarioForm());
        model.addAttribute("perfis", Perfil.values());
        return "usuarios-novo";
    }

    @PostMapping("/usuarios/novo")
    public String novoUsuario(
            @Valid @ModelAttribute("form") CadastroUsuarioForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirect,
            Principal principal,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("perfis", Perfil.values());
            return "usuarios-novo";
        }
        try {
            var cadastrado = usuarioService.cadastrar(
                    form.getNome(),
                    form.getSobrenome(),
                    form.getEmail(),
                    form.getUsername(),
                    form.getSenha(),
                    form.getPerfil());
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.USUARIO_CADASTRADO,
                    RegistroAuditoria.Resultado.SUCESSO, "Usuario", cadastrado.getId(),
                    AuditoriaService.ip(request));
        } catch (IllegalArgumentException e) {
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.USUARIO_CADASTRADO,
                    RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("perfis", Perfil.values());
            return "usuarios-novo";
        }
        redirect.addFlashAttribute("ok", "Usuário cadastrado com sucesso.");
        return "redirect:/usuarios/novo";
    }

    // --- Troca de senha (usuário logado) ---
    @GetMapping("/minha-senha")
    public String minhaSenhaForm() {
        return "minha-senha";
    }

    @PostMapping("/minha-senha")
    public String minhaSenha(
            Principal principal,
            @RequestParam String senhaAtual,
            @RequestParam String senhaNova,
            Model model,
            HttpServletRequest request) {
        if (senhaNova == null || senhaNova.length() < 8 || senhaNova.length() > 72) {
            model.addAttribute("erro", "A nova senha deve ter entre 8 e 72 caracteres.");
            return "minha-senha";
        }
        try {
            usuarioService.trocarSenha(principal.getName(), senhaAtual, senhaNova);
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.SENHA_TROCADA,
                    RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
        } catch (IllegalArgumentException e) {
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.SENHA_TROCADA,
                    RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
            model.addAttribute("erro", e.getMessage());
            return "minha-senha";
        }
        model.addAttribute("ok", "Senha alterada com sucesso.");
        return "minha-senha";
    }

    @GetMapping("/esqueci-senha")
    public String esqueciForm() {
        return "esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String esqueci(@RequestParam String email, Model model, HttpServletRequest request) {
        email = email.trim().toLowerCase(Locale.ROOT);
        String token = usuarioService.solicitarRecovery(email);
        auditoriaService.registrar(email, RegistroAuditoria.Acao.RECOVERY_SOLICITADO,
                RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
        // Mensagem sempre igual, exista ou não a conta.
        model.addAttribute("ok", "Se houver uma conta com esse e-mail, enviaremos um link de redefinição.");
        if (token != null) {
            try {
                passwordResetEmailService.enviar(email, token);
                log.info("E-mail de recuperação enviado.");
            } catch (MailException e) {
                // A resposta permanece neutra para não revelar se o e-mail existe.
                log.error("Não foi possível enviar o e-mail de recuperação.", e);
            }
        }
        return "esqueci-senha";
    }

    @GetMapping("/redefinir-senha/{token}")
    public String redefinirForm(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha/{token}")
    public String redefinir(
            @PathVariable String token,
            @RequestParam String senhaNova,
            Model model,
            HttpServletRequest request) {
        if (senhaNova == null || senhaNova.length() < 8 || senhaNova.length() > 72) {
            model.addAttribute("erro", "A nova senha deve ter entre 8 e 72 caracteres.");
            model.addAttribute("token", token);
            return "redefinir-senha";
        }
        try {
            usuarioService.redefinirSenha(token, senhaNova);
            auditoriaService.registrar(null, RegistroAuditoria.Acao.SENHA_REDEFINIDA,
                    RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
        } catch (IllegalArgumentException e) {
            auditoriaService.registrar(null, RegistroAuditoria.Acao.SENHA_REDEFINIDA,
                    RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("token", token);
            return "redefinir-senha";
        }
        return "redirect:/login?redefinida";
    }
}
