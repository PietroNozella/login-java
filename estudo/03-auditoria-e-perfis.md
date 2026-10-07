# Auditoria e perfis

| Ação | USUARIO | ADMINISTRADOR |
| --- | --- | --- |
| Acessar início, aceitar documentos e trocar a própria senha | Sim | Sim |
| Cadastrar uma conta pelo painel administrativo | Não | Sim |
| Consultar auditoria | Não | Sim |

O cadastro público está disponível para visitantes e sempre cria `USUARIO`.
Ocultar um link não protege uma rota: `SecurityConfig` aplica as permissões
no servidor, e o fragmento de navegação reflete essas mesmas regras.

A coleção `auditoria` registra ação, resultado, conta, horário e IP para
login, logout, bloqueio, cadastro, troca/redefinição de senha, solicitação
de recuperação, aceite e acesso negado. Senhas e tokens originais não são
campos do registro de auditoria. O cadastro público identifica o evento
pelo nome de usuário, sem depender de um principal autenticado.
