<?php
// 1. Inicializa a sessão existente para poder manipulá-la
session_start();

// 2. Limpa todas as variáveis da sessão ($_SESSION)
session_unset();

// 3. Destrói a sessão no servidor
session_destroy();

// 4. Se houver cookie de sessão, apaga do navegador por segurança
if (ini_get("session.use_cookies")) {
    $params = session_get_cookie_params();
    setcookie(
        session_name(),
        '',
        time() - 42000,
        $params["path"],
        $params["domain"],
        $params["secure"],
        $params["httponly"]
    );
}

// 5. Redireciona para a página de login
header("Location: login.php");
exit();
?>