# Autenticação passo a passo

O formulário envia `username`, `password` e o token CSRF para `/login`.
O Spring Security consulta `MongoUserDetailsService`, que busca a conta,
informa seu perfil e sinaliza se ela está ativa ou temporariamente bloqueada.
O `PasswordEncoder` compara a senha recebida com o hash BCrypt.

`AutenticacaoHandlers` registra sucesso ou falha. Na quinta falha, a conta
fica bloqueada por 15 minutos. No sucesso, o contador é limpo e o navegador
é redirecionado para `/`. Mensagens de credencial inválida não distinguem
usuário inexistente de senha incorreta.

O Spring Session mantém as sessões na coleção `sessions`. O navegador
transporta o identificador de sessão, não a senha. O logout é um POST
protegido por CSRF e encerra a sessão atual.

`AceiteInterceptor` verifica as versões aceitas antes de permitir a navegação
autenticada. Termos, privacidade, login, cadastro, recuperação e logout
ficam fora desse redirecionamento.
