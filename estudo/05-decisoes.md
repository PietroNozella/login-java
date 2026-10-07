# Decisões técnicas

- **Aplicação independente:** a base fornece autenticação e autorização,
  sem módulos de negócio. Reutilizar começa pela configuração e pelos templates.
- **Dois perfis:** administrador e usuário atendem ao acesso básico sem uma
  estrutura adicional de permissões dinâmicas.
- **Sessões no MongoDB:** preserva o modelo existente e permite compartilhar
  a persistência da sessão entre instâncias. Alterar senha ou perfil não
  revoga automaticamente todas as sessões existentes nesta versão.
- **Formulários separados:** o cadastro público não recebe um perfil;
  o administrativo reutiliza a validação e acrescenta esse campo.
- **BCrypt e tokens com hash:** senhas e tokens de recuperação não são
  guardados em texto puro no banco.
- **Personalização por ambiente:** `APP_NOME` altera a identificação visual,
  e `APP_TEMA_NOME` seleciona um arquivo CSS adicional.
- **Base nova:** `login_java` é o padrão; dados antigos permanecem intactos
  e não há migração automática.
- **Limites atuais:** não há confirmação de e-mail, MFA ou login social.
  Os textos legais devem ser adaptados ao serviço que usar esta aplicação.
