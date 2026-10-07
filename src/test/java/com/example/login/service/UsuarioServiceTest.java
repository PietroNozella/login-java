package com.example.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.login.entity.Perfil;
import com.example.login.entity.PasswordResetToken;
import com.example.login.entity.Usuario;
import com.example.login.repository.PasswordResetTokenRepository;
import com.example.login.repository.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "tokenRepository", tokenRepository);
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", new BCryptPasswordEncoder(4));
        ReflectionTestUtils.setField(usuarioService, "tokenMinutos", 60L);
        ReflectionTestUtils.setField(usuarioService, "maxTentativas", 5);
        ReflectionTestUtils.setField(usuarioService, "bloqueioMinutos", 15L);
    }

    @Test
    void deveCadastrarUsuarioComSenhaCriptografada() {
        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("maria@example.test")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario salvo = usuarioService.cadastrar(
                "Maria", "Silva", "maria@example.test", "maria", "senha-segura", Perfil.USUARIO);

        assertThat(salvo.getSenha()).isNotEqualTo("senha-segura");
        assertThat(new BCryptPasswordEncoder().matches("senha-segura", salvo.getSenha())).isTrue();
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.USUARIO);
    }

    @Test
    void deveRecusarUsernameDuplicado() {
        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> usuarioService.cadastrar(
                "Maria", "Silva", "maria@example.test", "maria", "senha-segura", Perfil.USUARIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome de usuário já existe.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void recoveryNaoDeveCriarTokenParaEmailInexistente() {
        when(usuarioRepository.findByEmail("ausente@example.test")).thenReturn(Optional.empty());

        assertThat(usuarioService.solicitarRecovery("ausente@example.test")).isNull();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void recoveryDeveCriarTokenUnicoComExpiracao() {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");
        when(usuarioRepository.findByEmail("maria@example.test")).thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String tokenOriginal = usuarioService.solicitarRecovery("maria@example.test");

        ArgumentCaptor<PasswordResetToken> tokenSalvo = ArgumentCaptor.forClass(PasswordResetToken.class);
        assertThat(tokenOriginal).hasSize(32);
        verify(tokenRepository).save(tokenSalvo.capture());
        assertThat(tokenSalvo.getValue().getToken()).hasSize(64).isNotEqualTo(tokenOriginal);
        assertThat(tokenSalvo.getValue().getUsuarioId()).isEqualTo("usuario-1");
        assertThat(tokenSalvo.getValue().getExpiraEm()).isNotNull();
    }

    @Test
    void deveBloquearContaNaQuintaFalhaEDestravarNoSucesso() {
        Usuario usuario = new Usuario();
        usuario.setFalhasLogin(4);
        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(usuarioService.registrarFalha("maria")).isTrue();
        assertThat(usuario.getFalhasLogin()).isZero();
        assertThat(usuario.getBloqueadoAte()).isNotNull();
        assertThat(usuario.bloqueado()).isTrue();

        usuarioService.registrarSucesso("maria");

        assertThat(usuario.getFalhasLogin()).isZero();
        assertThat(usuario.getBloqueadoAte()).isNull();
    }

    @Test
    void deveBuscarRecoveryPeloHashEMarcarTokenComoUsado() {
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUsuarioId("usuario-1");
        reset.setExpiraEm(Instant.now().plusSeconds(300));
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");

        when(tokenRepository.findByToken(any(String.class))).thenReturn(Optional.of(reset));
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario));

        usuarioService.redefinirSenha("token-recebido-por-email", "nova-senha-segura");

        verify(tokenRepository).findByToken(org.mockito.ArgumentMatchers.argThat(
                hash -> hash.length() == 64 && !hash.equals("token-recebido-por-email")));
        assertThat(reset.isUsado()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("nova-senha-segura", usuario.getSenha())).isTrue();
    }

    @Test
    void cadastroPublicoDeveCriarSomenteUsuarioComEmailNormalizado() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario salvo = usuarioService.cadastrarPublico(
                " Maria ", " Silva ", " MARIA@EXAMPLE.TEST ", " maria ", " senha-segura ");

        assertThat(salvo.getPerfil()).isEqualTo(Perfil.USUARIO);
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getEmail()).isEqualTo("maria@example.test");
        assertThat(salvo.getUsername()).isEqualTo("maria");
        assertThat(new BCryptPasswordEncoder().matches(" senha-segura ", salvo.getSenha())).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("senha-segura", salvo.getSenha())).isFalse();
    }

    @Test
    void deveRecusarEmailDuplicado() {
        when(usuarioRepository.findByEmail("maria@example.test")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> usuarioService.cadastrarPublico(
                "Maria", "Silva", "maria@example.test", "maria", "senha-segura"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail já cadastrado.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveTratarDuplicidadeConcorrenteNoSalvamento() {
        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("índice único"));

        assertThatThrownBy(() -> usuarioService.cadastrarPublico(
                "Maria", "Silva", "maria@example.test", "maria", "senha-segura"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome de usuário ou e-mail já cadastrado.");
    }

    @Test
    void deveRecusarTokenExpiradoSemAlterarSenha() {
        PasswordResetToken reset = new PasswordResetToken();
        reset.setExpiraEm(Instant.now().minusSeconds(1));
        when(tokenRepository.findByToken(any(String.class))).thenReturn(Optional.of(reset));

        assertThatThrownBy(() -> usuarioService.redefinirSenha("token", "nova-senha-segura"))
                .hasMessage("Link expirado ou já utilizado.");

        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void deveRecusarTokenJaUtilizadoSemAlterarSenha() {
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUsado(true);
        reset.setExpiraEm(Instant.now().plusSeconds(300));
        when(tokenRepository.findByToken(any(String.class))).thenReturn(Optional.of(reset));

        assertThatThrownBy(() -> usuarioService.redefinirSenha("token", "nova-senha-segura"))
                .hasMessage("Link expirado ou já utilizado.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRecusarSenhaAtualIncorreta() {
        Usuario usuario = new Usuario();
        usuario.setSenha(new BCryptPasswordEncoder(4).encode("senha-segura"));
        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.trocarSenha("maria", "senha-errada", "nova-senha"))
                .hasMessage("Senha atual incorreta.");

        verify(usuarioRepository, never()).save(any());
    }
}
