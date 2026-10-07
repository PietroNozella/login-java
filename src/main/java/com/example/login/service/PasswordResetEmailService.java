package com.example.login.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String remetente;
    private final long tokenMinutos;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.mail.from}") String remetente,
            @Value("${app.recovery.token-minutos}") long tokenMinutos) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.remetente = remetente;
        this.tokenMinutos = tokenMinutos;
    }

    public void enviar(String destinatario, String token) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Redefinição de senha");
        mensagem.setText("Use o link abaixo para redefinir sua senha. Ele expira em "
                + tokenMinutos + " minutos e só pode ser utilizado uma vez:\n\n"
                + baseUrl + "/redefinir-senha/" + token);
        mailSender.send(mensagem);
    }
}
