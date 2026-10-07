package com.example.login.service;

import com.example.login.entity.PasswordResetToken;
import com.example.login.entity.Perfil;
import com.example.login.entity.Usuario;
import com.example.login.repository.PasswordResetTokenRepository;
import com.example.login.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.recovery.token-minutos:60}")
    private long tokenMinutos;

    @Value("${app.login.max-tentativas:5}")
    private int maxTentativas;

    @Value("${app.login.bloqueio-minutos:15}")
    private long bloqueioMinutos;

    public Usuario cadastrarPublico(String nome, String sobrenome, String email,
                                    String username, String senha) {
        return cadastrar(nome, sobrenome, email, username, senha, Perfil.USUARIO);
    }

    public Usuario cadastrar(String nome, String sobrenome, String email,
                             String username, String senhaInicial, Perfil perfil) {
        nome = nome.trim();
        sobrenome = sobrenome.trim();
        username = username.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Nome de usuário já existe.");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setSobrenome(sobrenome);
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setSenha(passwordEncoder.encode(senhaInicial));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setCriadoEm(Instant.now());
        try {
            return usuarioRepository.save(usuario);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Nome de usuário ou e-mail já cadastrado.", e);
        }
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public void trocarSenha(String username, String senhaAtual, String senhaNova) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        usuario.setSenha(passwordEncoder.encode(senhaNova));
        usuarioRepository.save(usuario);
    }

    // Gera token de uso único e o devolve ao controller para envio por e-mail;
    // o banco guarda só o hash SHA-256, nunca o token original.
    public String solicitarRecovery(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return null; // resposta neutra: não revela se o e-mail existe
        }
        String tokenOriginal = UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken reset = new PasswordResetToken();
        reset.setToken(hashToken(tokenOriginal));
        reset.setUsuarioId(usuario.getId());
        reset.setExpiraEm(Instant.now().plus(Duration.ofMinutes(tokenMinutos)));
        reset.setUsado(false);
        tokenRepository.save(reset);
        return tokenOriginal;
    }

    public void redefinirSenha(String token, String senhaNova) {
        PasswordResetToken reset = tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido."));
        if (reset.isUsado() || reset.expirado()) {
            throw new IllegalArgumentException("Link expirado ou já utilizado.");
        }
        Usuario usuario = usuarioRepository.findById(reset.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        usuario.setSenha(passwordEncoder.encode(senhaNova));
        usuario.setFalhasLogin(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);
        reset.setUsado(true);
        tokenRepository.save(reset);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível.", e);
        }
    }

    // 5 tentativas com 15 min de bloqueio: freia força bruta sem travar o usuário
    // legítimo por muito tempo. Retorna true se a conta acabou de ser bloqueada nesta tentativa.
    public boolean registrarFalha(String username) {
        return usuarioRepository.findByUsername(username).map(usuario -> {
            usuario.setFalhasLogin(usuario.getFalhasLogin() + 1);
            boolean bloqueouAgora = false;
            if (usuario.getFalhasLogin() >= maxTentativas) {
                usuario.setBloqueadoAte(Instant.now().plus(Duration.ofMinutes(bloqueioMinutos)));
                usuario.setFalhasLogin(0);
                bloqueouAgora = true;
            }
            usuarioRepository.save(usuario);
            return bloqueouAgora;
        }).orElse(false);
    }

    public void registrarSucesso(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setFalhasLogin(0);
            usuario.setBloqueadoAte(null);
            usuarioRepository.save(usuario);
        });
    }

    public void registrarAceite(String username, String termosVersao, String privacidadeVersao) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        usuario.setAceiteTermosVersao(termosVersao);
        usuario.setAceitePrivacidadeVersao(privacidadeVersao);
        usuario.setAceiteEm(Instant.now());
        usuarioRepository.save(usuario);
    }
}
