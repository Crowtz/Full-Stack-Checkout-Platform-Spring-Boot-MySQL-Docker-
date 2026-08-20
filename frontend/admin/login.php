<?php
session_start();

if (isset($_SESSION['usuario'])) {
    header("Location: index.php");
    exit();
}

$erro = false;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $usuario_valido = getenv('APP_USER') ?: $_ENV['APP_USER'] ?? 'admin';
    $senha_pura = getenv('APP_PASS') ?: $_ENV['APP_PASS'] ?? '123';

    $senha_hash_valida = password_hash($senha_pura, PASSWORD_DEFAULT);

    $usuario_digitado = $_POST['usuario'] ?? '';
    $senha_digitada = $_POST['senha'] ?? '';

    if ($usuario_digitado === $usuario_valido && password_verify($senha_digitada, $senha_hash_valida)) {
        session_regenerate_id(true);
        $_SESSION['usuario'] = $usuario_digitado;

        header("Location: index.php");
        exit();
    } else {
        $erro = true;
    }
}
?>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acesso</title>
    <link rel="stylesheet" href="style.css">

    <style>
        body {
            margin: 0;
            display: flex;
            justify-content: center;
            /* Centraliza na horizontal */
            align-items: center;
            /* Centraliza na vertical */
            min-height: 100vh;
            /* Garante a altura total da janela */
            background-color: #f5f7fa;
            /* Cor de fundo opcional */
            font-family: Arial, sans-serif;
        }

        .container-login {
            max-width: 500px;
            width: 100%;
            background: #ffffff;
            padding: 32px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
            border: 1px solid #e1e8ed;
        }
    </style>
</head>

<body>
    <div class="container-login">
        <h2>Área de Acesso</h2>

        <?php if ($erro): ?>
            <p style="color: red;">Usuário ou senha incorretos!</p>
        <?php endif; ?>

        <!-- action vazio faz o formulário enviar os dados para este mesmo arquivo -->
        <form action="" method="POST">
            <label for="usuario">Usuário:</label><br>
            <input type="text" id="usuario" name="usuario" required><br><br>

            <label for="senha">Senha:</label><br>
            <input type="password" id="senha" name="senha" required><br><br>

            <button type="submit" class="btn-submit" id="btnSave">Entrar</button>
        </form>
    </div>
</body>

</html>