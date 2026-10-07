# Visão geral

O projeto é uma aplicação de autenticação genérica com Java, Spring Boot,
Thymeleaf e MongoDB. Não contém um domínio de negócio acoplado ao login.

## Fluxo de uma conta

1. O visitante abre `/cadastro` e cria uma conta `USUARIO`.
2. Em `/login`, informa nome de usuário e senha.
3. O Spring Security autentica e persiste a sessão no MongoDB.
4. Se o aceite não estiver atualizado, o interceptor encaminha para `/aceite`.
5. A página inicial mostra a sessão ativa e os links disponíveis para o perfil.

`USUARIO` acessa a própria conta. `ADMINISTRADOR` também cadastra usuários,
define o perfil no cadastro administrativo e consulta a auditoria.
O primeiro administrador é criado pelo `AdminSeed` a partir do ambiente.

Controllers recebem os formulários, services aplicam as regras e repositories
acessam o MongoDB. O pacote raiz é `com.example.login`.
