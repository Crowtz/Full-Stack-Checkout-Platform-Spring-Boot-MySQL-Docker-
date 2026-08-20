# Full-Stack Checkout Platform

Plataforma completa de checkout e pós-venda voltada para processamento de pagamentos, notificações automáticas por e-mail e gerenciamento de anexos. Projeto 100% containerizado com Docker.

**Desenvolvido por:** [Lucas Henrique Julionel Da Silva](https://github.com/Crowtz)  
**LinkedIn:** [Perfil do LinkedIn](https://www.linkedin.com/in/lucas-julionel-7a489542b/)

---

## Demonstração em Vídeo

[![Demonstração do Projeto](https://img.youtube.com/vi/SEU_VIDEO_ID/maxresdefault.jpg)](https://www.youtube.com/watch?v=SEU_VIDEO_ID)

---

## 📸 Capturas de Tela

<div align="center">
  <img src="./docs/screenshots/checkout.png" alt="Tela de Checkout" width="45%">
  <img src="./docs/screenshots/dashboard.png" alt="Painel Administrativo" width="45%">
</div>

---

## Tecnologias Utilizadas

* **Backend:** Java 17, Spring Boot, HikariCP, JDBC.
* **Banco de Dados:** MySQL 8.0.
* **Frontend:** HTML5, CSS3, JavaScript (ES6+), Nginx.
* **Infraestrutura:** Docker, Docker Compose.
* **Integrações:** Mercado Pago API (Webhooks & Checkout), JavaMail API (SMTP).

---

## Funcionalidades Principais

- [x] Processamento de transações via Mercado Pago com escuta via Webhook.
- [x] Envio automático de e-mails com anexos/comprovantes dinâmicos.
- [x] Gerenciamento e persistência de arquivos locais no servidor.
- [x] Criação e verificação automática de banco de dados e tabelas no startup da aplicação.
- [x] Deploy unificado com comando único via Docker Compose.

---

## Como Rodar o Projeto

### Pré-requisitos
* [Docker Windows](https://docs.docker.com/desktop/setup/install/windows-install/) instalado e em execução.
* [Docker Linux](https://docs.docker.com/engine/install/ubuntu/) instalado e em execução.

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/Crowtz/Full-Stack-Checkout-Platform-Spring-Boot-MySQL-Docker-
   cd Full-Stack-Checkout-Platform-Spring-Boot-MySQL-Docker-

2. **Configuração do Ambiente (.env)**
   ```bash
   Localize o arquivo .env (ou crie-o a partir do .env.example).
   Abra o arquivo com um editor (ex: nano ou vim): nano .env
   Preencha as variáveis de ambiente com os dados corretos (IP da VPS, senha de admin e tokens do Mercado Pago).
   
3. **Ajuste de Permissões**
   ```bash
   Para garantir que o Docker consiga ler e escrever nos volumes de dados do projeto:
   sudo chmod -R 775 .

4. **Inicializando a Aplicação**
   ```bash
   Suba o ambiente em modo de segundo plano (background):
   docker compose up -d --build

5. **Manutenção e Reinicialização**
   ```bash
   Sempre que fizer alterações no arquivo .env ou atualizar o código, reinicie os containers:
   Parar e limpar volumes: docker compose down -v
   Subir novamente: docker compose up -d --build
