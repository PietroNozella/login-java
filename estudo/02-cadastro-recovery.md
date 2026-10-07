# Cadastro e recuperação de senha

## Cadastro

`CadastroPublicoForm` contém nome, sobrenome, e-mail, usuário e senha.
`CadastroUsuarioForm` reutiliza esses campos e acrescenta o perfil, obrigatório
somente no cadastro administrativo.

O cadastro público chama `UsuarioService.cadastrarPublico`, que fixa o perfil
`USUARIO`. Um parâmetro `perfil=ADMINISTRADOR` enviado pelo visitante não
concede privilégios. O cadastro administrativo é protegido pelo Spring Security.

Nome, sobrenome e usuário são aparados; e-mail também é convertido para
minúsculas. A senha não é aparada. O banco mantém índices únicos de usuário
e e-mail. Tanto a consulta prévia quanto um conflito no salvamento resultam
em mensagem de duplicidade, sem expor detalhes do banco.

## Recuperação

`/esqueci-senha` sempre exibe a mesma resposta. Para uma conta existente,
o serviço gera um token, armazena seu hash SHA-256 com validade de 60 minutos
e envia o link pelo SMTP. O Mailpit permite conferir esse envio localmente.

Em `/redefinir-senha/{token}`, o serviço verifica o hash, o prazo e se o token
já foi utilizado. A nova senha é salva com BCrypt e o token é marcado como
usado. A redefinição também limpa o bloqueio por tentativas inválidas.
