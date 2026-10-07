# Login Java

Aplicação de autenticação reutilizável em Java 21, Spring Boot 3.4, Spring Security,
Thymeleaf e MongoDB. A interface está em português brasileiro.

## Funcionalidades

- Cadastro público em `/cadastro`, sempre com perfil `USUARIO`.
- Cadastro administrativo em `/usuarios/novo`, com seleção de `USUARIO` ou `ADMINISTRADOR`.
- Login por nome de usuário, logout por POST e sessões persistidas no MongoDB.
- Senhas com BCrypt e proteção CSRF nos formulários.
- Bloqueio da conta por 15 minutos após cinco tentativas inválidas.
- Troca de senha e recuperação por e-mail com token de uso único, válido por 60 minutos.
- Aceite versionado dos termos e da privacidade após o login.
- Auditoria de autenticação e ações de conta, acessível somente ao administrador.

Não há confirmação de e-mail, MFA ou login social. Os documentos legais são modelos
que precisam ser adaptados pelo responsável pela implantação.

## Executar com Docker

Pré-requisito: Docker com Compose em execução.

```powershell
Copy-Item .env.example .env
# Substitua APP_ADMIN_PASSWORD por uma senha forte antes de iniciar.
docker compose up --build
```

- Aplicação: `http://localhost:8080/login`
- Caixa de e-mail local do Mailpit: `http://localhost:8025`
- MongoDB: `localhost:27018`, banco padrão `login_java`

O Dockerfile compila o projeto e gera o JAR. A primeira conta administrativa é
criada com as variáveis `APP_ADMIN_*`; reinícios não sobrescrevem uma conta existente.
O nome do administrador inicial fica reservado para essa conta.

## Executar localmente

Pré-requisitos: Java 21 e MongoDB acessível. Build e testes são validados com Java 21.
Para usar o MongoDB e o Mailpit do Compose com a aplicação fora do Docker:

```powershell
docker compose up -d mongo mailpit
$env:MONGODB_URI="mongodb://localhost:27018/login_java"
$env:MONGODB_DATABASE="login_java"
$env:APP_ADMIN_PASSWORD="defina-uma-senha-forte"
$env:SMTP_HOST="localhost"
$env:SMTP_PORT="1025"
$env:APP_BASE_URL="http://localhost:8080"
.\mvnw.cmd spring-boot:run
```

O Maven Wrapper fornece o Maven. Na execução local, `.env` não é carregado
automaticamente: use as variáveis do processo como no exemplo.

## Configuração e personalização

| Variável | Uso | Padrão |
| --- | --- | --- |
| `APP_NOME` | Nome exibido nos títulos e na navegação | `Sistema de Login` |
| `APP_TEMA_NOME` | Arquivo de tema visual | `padrao` |
| `APP_BASE_URL` | Endereço público usado nos links de recuperação | `http://localhost:8080` |
| `MONGODB_URI` | Conexão do MongoDB | `mongodb://localhost:27017/login_java` |
| `MONGODB_DATABASE` | Nome do banco | `login_java` |
| `APP_ADMIN_USERNAME` | Usuário do administrador inicial | `admin` |
| `APP_ADMIN_PASSWORD` | Senha do administrador inicial | Obrigatória |
| `APP_ADMIN_EMAIL` | E-mail do administrador inicial | `admin@local` |
| `APP_ADMIN_NOME` | Nome do administrador inicial | `Administrador` |
| `SESSION_TIMEOUT` | Tempo de inatividade da sessão | `30m` |

Para um novo tema, copie `static/css/tema-padrao.css` para `tema-<nome>.css`,
altere as variáveis CSS e configure `APP_TEMA_NOME=<nome>`. O tema padrão é
carregado primeiro e o novo arquivo sobrescreve suas variáveis.

O banco contém `usuarios`, `password_reset_tokens`, `auditoria` e `sessions`.
A aplicação não migra nem exclui bancos ou volumes de versões anteriores.

Para MongoDB Atlas, configure `MONGODB_URI` com a URI do cluster e
`MONGODB_DATABASE` com o banco escolhido. Autorize a conexão no cluster e
codifique caracteres especiais das credenciais na URI. Não versione senhas.

## E-mail de recuperação

O Compose inclui Mailpit para testes locais. Para usar um servidor SMTP:

```env
SMTP_HOST=smtp.exemplo.com
SMTP_PORT=587
SMTP_USERNAME=usuario
SMTP_PASSWORD=senha
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=no-reply@exemplo.com
APP_BASE_URL=https://sistema.exemplo.com
```

A resposta de recuperação é a mesma para e-mails existentes e inexistentes.
O token original aparece apenas no link enviado por e-mail; o banco guarda seu hash.

## Estrutura

O código está em `src/main/java/com/example/login`: configurações na raiz e
subpacotes `controller`, `dto`, `entity`, `repository`, `security` e `service`.
Templates e arquivos estáticos ficam em `src/main/resources`. A pasta `estudo`
explica os fluxos e as decisões técnicas.

## Testes e build

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

O JAR gerado é `target/login-java-1.0.0.jar`. A suíte cobre validação, cadastro
público sem privilégios, duplicidade, hash de senha, bloqueio e tokens de recuperação.
Para verificar o fluxo real, inicie MongoDB/Mailpit e percorra cadastro, login,
aceite, troca de senha e logout; confira a recuperação na caixa de e-mail local.
