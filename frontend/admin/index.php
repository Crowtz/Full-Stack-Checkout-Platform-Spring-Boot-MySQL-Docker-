<?php
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

if (!isset($_SESSION['usuario'])) {
    header("Location: login.php");
    exit();
}
?>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Painel - Gerenciador de Checkouts</title>
    <link rel="stylesheet" href="style.css">
</head>

<body>

    <div style="position: fixed; bottom: 20px; left: 20px; z-index: 1000;">
        <a href="logout.php" style="
        display: inline-block;
        padding: 10px 18px;
        background-color: #3483FA;
        color: #ffffff;
        text-decoration: none;
        border-radius: 6px;
        font-size: 14px;
        font-weight: bold;
        font-family: Arial, sans-serif;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
    ">Sair</a>
    </div>

    <div id="customModal" class="modal-overlay">
        <div class="modal-box">
            <h3 id="modalTitle">Aviso</h3>
            <p id="modalMsg"></p>
            <div id="modalInputContainer" style="display: none; margin-bottom: 12px;">
                <label style="font-size: 12px;">E-mail do Destinatário:</label>
                <input type="email" id="modalInput" placeholder="seuemail@exemplo.com">
            </div>
            <div class="modal-actions" id="modalActions">
                <button class="btn-modal btn-modal-primary" onclick="fecharModal()">OK</button>
            </div>
        </div>
    </div>

    <div class="dashboard">

        <div class="card">
            <h2 id="formTitle">Criar Novo Checkout</h2>
            <form id="checkoutForm">
                <input type="hidden" id="checkout_id">

                <div class="form-group">
                    <label>Título do Produto *</label>
                    <input type="text" id="title" placeholder="Ex: Curso Completo Java" required>
                </div>

                <div class="form-group">
                    <label>Descrição</label>
                    <textarea id="description" rows="2" placeholder="Breve descrição do item"></textarea>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Preço Original (R$)</label>
                        <input type="number" step="0.01" id="originalPrice" placeholder="49.90">
                    </div>
                    <div class="form-group">
                        <label>Preço Venda (R$) *</label>
                        <input type="number" step="0.01" id="price" placeholder="26.90" required>
                    </div>
                    <div class="form-group">
                        <label>% Desconto</label>
                        <input type="number" id="discountPercent" placeholder="46" readonly
                            style="background-color: #f1f5f9; cursor: not-allowed;">
                    </div>
                </div>

                <div class="form-group">
                    <label>Link Pós-Pagamento (Página de Obrigado / Entrega)</label>
                    <input type="url" id="redirectUrl" placeholder="https://seusite.com/obrigado">
                </div>

                <div class="form-group">
                    <label>Imagem do Produto</label>
                    <div class="image-toggle">
                        <button type="button" id="btnTypeUrl" class="active" onclick="switchImgType('url')">URL
                            Externa</button>
                        <button type="button" id="btnTypeFile" onclick="switchImgType('file')">Upload do PC</button>
                    </div>

                    <div id="boxUrl">
                        <input type="url" id="imageUrl" placeholder="https://exemplo.com/imagem.jpg">
                    </div>

                    <div id="boxFile" style="display:none;">
                        <input type="file" id="imageFile" accept="image/*">
                    </div>
                </div>

                <h3 style="font-size: 15px; color: var(--text-main); margin-top: 24px;">Configuração de E-mail Pós-Venda
                </h3>

                <label class="checkbox-container">
                    <input type="checkbox" id="sendEmailEnabled" checked onchange="toggleEmailFields()">
                    Enviar e-mail automático pós-venda
                </label>

                <div id="emailFieldsGroup">
                    <div class="form-row">
                        <div class="form-group">
                            <label>Servidor SMTP (Host)</label>
                            <input type="text" id="smtpHost" placeholder="smtp.gmail.com">
                        </div>
                        <div class="form-group">
                            <label>Porta SMTP</label>
                            <input type="number" id="smtpPort" placeholder="587">
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label>E-mail do Remetente (Login)</label>
                            <input type="email" id="smtpUser" placeholder="seuemail@gmail.com">
                        </div>
                        <div class="form-group">
                            <label>Senha / Token de App</label>
                            <input type="password" id="smtpPass" placeholder="••••••••">
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Assunto do E-mail</label>
                        <input type="text" id="emailSubject" placeholder="Seu acesso chegou! {nome}">
                    </div>

                    <div class="form-group">
                        <label>Corpo do E-mail (HTML permitido)</label>
                        <textarea id="emailBodyHtml" rows="5"
                            placeholder="<h1>Obrigado pela compra!</h1><p>Acesse aqui: {link}</p>"></textarea>
                    </div>

                    <!-- Lista de anexos existentes (Modo Edição) -->
                    <div class="form-group" id="existingAttachmentsGroup" style="display: none;">
                        <label>Anexos Cadastrados:</label>
                        <ul class="attachment-list" id="existingAttachmentsList"></ul>
                    </div>

                    <div class="form-group">
                        <label>Adicionar Anexos (PDF, ZIP, etc.)</label>
                        <input type="file" id="emailAttachments" multiple>
                    </div>
                </div>

                <button type="submit" class="btn-submit" id="btnSave">Gerar Link de Checkout</button>
                <button type="button" class="btn-cancel" id="btnCancel" onclick="resetForm()">Cancelar Edição</button>
            </form>
        </div>

        <div class="card">
            <h2>Checkouts Cadastrados</h2>
            <div id="checkoutList">Carregando...</div>
        </div>

    </div>

    <script>
        const BASE_API_URL = <?php echo json_encode(getenv('BASE_URL_SPRING') ?: $_ENV['BASE_URL_SPRING'] ?? 'http://localhost:8080'); ?>;

        function initConfig() {
            try {
                console.log('API inicializados com sucesso:', BASE_API_URL);
            } catch (error) {
                console.error('Erro ao inicializar API', error);
            }
        }

        initConfig();

        let baseUrlSpring = BASE_API_URL;


        let imageType = "url";
        let onConfirmAction = null;

        function toggleEmailFields() {
            const isChecked = document.getElementById("sendEmailEnabled").checked;
            document.getElementById("emailFieldsGroup").style.opacity = isChecked ? "1" : "0.5";
            document.getElementById("emailFieldsGroup").style.pointerEvents = isChecked ? "auto" : "none";
        }

        function abrirModal(titulo, mensagem, showInput = false, confirmCallback = null) {
            document.getElementById("modalTitle").innerText = titulo;
            document.getElementById("modalMsg").innerText = mensagem;
            const inputContainer = document.getElementById("modalInputContainer");
            const actionsDiv = document.getElementById("modalActions");

            inputContainer.style.display = showInput ? "block" : "none";
            document.getElementById("modalInput").value = "";

            if (confirmCallback) {
                onConfirmAction = confirmCallback;
                actionsDiv.innerHTML = `
                    <button class="btn-modal btn-modal-secondary" onclick="fecharModal()">Cancelar</button>
                    <button class="btn-modal btn-modal-primary" onclick="executarConfirmacao()">Confirmar</button>
                `;
            } else {
                actionsDiv.innerHTML = `<button class="btn-modal btn-modal-primary" onclick="fecharModal()">OK</button>`;
            }

            document.getElementById("customModal").style.display = "flex";
        }

        function fecharModal() {
            document.getElementById("customModal").style.display = "none";
            onConfirmAction = null;
        }

        function executarConfirmacao() {
            const inputValue = document.getElementById("modalInput").value;
            if (onConfirmAction) onConfirmAction(inputValue);
            fecharModal();
        }

        function calcularDesconto() {
            const orig = parseFloat(document.getElementById("originalPrice").value);
            const atual = parseFloat(document.getElementById("price").value);

            if (orig && atual && orig > atual) {
                document.getElementById("discountPercent").value = Math.round(((orig - atual) / orig) * 100);
            } else {
                document.getElementById("discountPercent").value = "";
            }
        }

        document.getElementById("originalPrice").addEventListener("input", calcularDesconto);
        document.getElementById("price").addEventListener("input", calcularDesconto);

        function switchImgType(type) {
            imageType = type;
            document.getElementById("btnTypeUrl").classList.toggle("active", type === 'url');
            document.getElementById("btnTypeFile").classList.toggle("active", type === 'file');
            document.getElementById("boxUrl").style.display = type === 'url' ? 'block' : 'none';
            document.getElementById("boxFile").style.display = type === 'file' ? 'block' : 'none';
        }

        async function carregarCheckouts() {
            try {
                const res = await fetch(`${baseUrlSpring}/api/checkouts`, {
                    headers: {
                        "ngrok-skip-browser-warning": "true"
                    }
                });
                const lista = await res.json();
                const container = document.getElementById("checkoutList");

                if (lista.length === 0) {
                    container.innerHTML = "<p style='color:var(--text-muted); font-size:14px;'>Nenhum checkout criado ainda.</p>";
                    return;
                }

                container.innerHTML = lista.map(item => `
                    <div class="checkout-item">
                        <img src="${item.imageUrl || 'https://via.placeholder.com/60'}" alt="Thumb">
                        <div class="checkout-details">
                            <div class="checkout-title">${item.title}</div>
                            <div class="checkout-price">R$ ${Number(item.price).toFixed(2)}</div>
                        </div>
                        <div class="checkout-actions">
                            <button class="btn-action btn-open" onclick="abrirLink(${item.id})">Abrir</button>
                            <button class="btn-action btn-copy" onclick="copiarLink(${item.id})">Copiar</button>
                            <button class="btn-action btn-edit" onclick="editarCheckout(${item.id})">Editar</button>
                            <button class="btn-action btn-delete" onclick="deletarCheckout(${item.id})">Remover</button>
                            <button class="btn-action btn-test" onclick="solicitarEmailTeste(${item.id})">✉ Enviar E-mail de Teste</button>
                        </div>
                    </div>
                `).join('');
            } catch (err) {
                document.getElementById("checkoutList").innerText = "Erro ao carregar checkouts.";
            }
        }

        function solicitarEmailTeste(checkoutId) {
            abrirModal("Testar Envio de E-mail", "Digite o e-mail que receberá a mensagem de teste:", true, async (targetEmail) => {
                if (!targetEmail || !targetEmail.includes("@")) {
                    alert("Insira um e-mail válido.");
                    return;
                }
                try {
                    const res = await fetch(`${baseUrlSpring}/api/email/test-email?checkoutId=${checkoutId}&targetEmail=${encodeURIComponent(targetEmail)}`, {
                        method: "POST"
                    });
                    if (res.ok) {
                        abrirModal("Sucesso", "E-mail de teste enviado com sucesso para: " + targetEmail);
                    } else {
                        const errData = await res.json();
                        abrirModal("Erro", "Falha no envio: " + (errData.error || "Erro desconhecido"));
                    }
                } catch (e) {
                    abrirModal("Erro", "Erro ao comunicar com o servidor.");
                }
            });
        }

        async function editarCheckout(id) {
            try {
                const res = await fetch(`${baseUrlSpring}/api/checkouts/${id}`, {
                    headers: {
                        "ngrok-skip-browser-warning": "true"
                    }
                });
                const data = await res.json();

                document.getElementById("checkout_id").value = data.id;
                document.getElementById("title").value = data.title;
                document.getElementById("description").value = data.description || '';
                document.getElementById("originalPrice").value = data.originalPrice || '';
                document.getElementById("price").value = data.price;
                document.getElementById("discountPercent").value = data.discountPercent || '';
                document.getElementById("redirectUrl").value = data.redirectUrl || '';

                switchImgType('url');
                document.getElementById("imageUrl").value = data.imageUrl || '';

                document.getElementById("sendEmailEnabled").checked = data.sendEmailEnabled !== false;
                document.getElementById("smtpHost").value = data.smtpHost || '';
                document.getElementById("smtpPort").value = data.smtpPort || '';
                document.getElementById("smtpUser").value = data.smtpUser || '';
                document.getElementById("smtpPass").value = data.smtpPass || '';
                document.getElementById("emailSubject").value = data.emailSubject || '';
                document.getElementById("emailBodyHtml").value = data.emailBodyHtml || '';

                toggleEmailFields();
                carregarAnexosExistentes(data.attachments || []);

                document.getElementById("formTitle").innerText = "Editar Checkout #" + id;
                document.getElementById("btnSave").innerText = "Atualizar Checkout";
                document.getElementById("btnCancel").style.display = "block";
            } catch (err) {
                abrirModal("Erro", "Erro ao carregar dados do checkout.");
            }
        }

        function carregarAnexosExistentes(attachments) {
            const group = document.getElementById("existingAttachmentsGroup");
            const list = document.getElementById("existingAttachmentsList");
            list.innerHTML = "";

            if (!attachments || attachments.length === 0) {
                group.style.display = "none";
                return;
            }

            group.style.display = "block";
            attachments.forEach(att => {
                const li = document.createElement("li");
                li.className = "attachment-item";
                li.innerHTML = `
                    <span>📄 ${att.fileName || att.filePath.split('/').pop()}</span>
                    <button type="button" class="btn-remove-file" onclick="removerAnexo(${att.id})">Excluir</button>
                `;
                list.appendChild(li);
            });
        }

        async function removerAnexo(attachmentId) {
            abrirModal("Confirmar exclusão", "Deseja excluir este anexo?", false, async () => {
                try {
                    const res = await fetch(`${baseUrlSpring}/api/checkouts/attachments/${attachmentId}`, {
                        method: "DELETE"
                    });
                    if (res.ok) {
                        const checkoutId = document.getElementById("checkout_id").value;
                        editarCheckout(checkoutId);
                    }
                } catch (e) {
                    abrirModal("Erro", "Não foi possível excluir o anexo.");
                }
            });
        }

        async function deletarCheckout(id) {
            abrirModal("Confirmação", "Deseja realmente remover este checkout?", false, async () => {
                try {
                    await fetch(`${baseUrlSpring}/api/checkouts/${id}`, {
                        method: "DELETE"
                    });
                    carregarCheckouts();
                } catch (err) {
                    abrirModal("Erro", "Erro ao deletar checkout.");
                }
            });
        }

        function abrirLink(id) {
            window.open(`${window.location.origin}/index.php?id=${id}`, '_blank');
        }

        function copiarLink(id) {
            const link = `${window.location.origin}/index.php?id=${id}`;
            navigator.clipboard.writeText(link);
            abrirModal("Copiado!", "Link do checkout copiado para a área de transferência:\n\n" + link);
        }

        function resetForm() {
            document.getElementById("checkoutForm").reset();
            document.getElementById("checkout_id").value = "";
            document.getElementById("formTitle").innerText = "Criar Novo Checkout";
            document.getElementById("btnSave").innerText = "Gerar Link de Checkout";
            document.getElementById("btnCancel").style.display = "none";
            document.getElementById("existingAttachmentsGroup").style.display = "none";
            document.getElementById("sendEmailEnabled").checked = true;
            toggleEmailFields();
            switchImgType('url');
        }

        document.getElementById("checkoutForm").addEventListener("submit", async (e) => {
            e.preventDefault();

            let imgFinal = document.getElementById("imageUrl").value;
            if (imageType === "file") {
                const fileInput = document.getElementById("imageFile");
                if (fileInput.files.length > 0) {
                    imgFinal = await new Promise((res) => {
                        const reader = new FileReader();
                        reader.onload = () => res(reader.result);
                        reader.readAsDataURL(fileInput.files[0]);
                    });
                }
            }

            const formData = new FormData();
            const id = document.getElementById("checkout_id").value;

            const checkoutData = {
                title: document.getElementById("title").value,
                description: document.getElementById("description").value,
                originalPrice: parseFloat(document.getElementById("originalPrice").value) || null,
                price: parseFloat(document.getElementById("price").value),
                discountPercent: parseInt(document.getElementById("discountPercent").value) || null,
                redirectUrl: document.getElementById("redirectUrl").value || null,
                imageUrl: imgFinal,
                sendEmailEnabled: document.getElementById("sendEmailEnabled").checked,
                smtpHost: document.getElementById("smtpHost").value,
                smtpPort: parseInt(document.getElementById("smtpPort").value) || null,
                smtpUser: document.getElementById("smtpUser").value,
                smtpPass: document.getElementById("smtpPass").value,
                emailSubject: document.getElementById("emailSubject").value,
                emailBodyHtml: document.getElementById("emailBodyHtml").value
            };

            formData.append("checkout", new Blob([JSON.stringify(checkoutData)], {
                type: "application/json"
            }));

            const attachmentInput = document.getElementById("emailAttachments");
            for (let i = 0; i < attachmentInput.files.length; i++) {
                formData.append("attachments", attachmentInput.files[i]);
            }

            const url = id ? `${baseUrlSpring}/api/checkouts/${id}` : `${baseUrlSpring}/api/checkouts`;
            const method = id ? "PUT" : "POST";

            try {
                const response = await fetch(url, {
                    method,
                    body: formData
                });
                if (response.ok) {
                    abrirModal("Sucesso", id ? "Checkout atualizado com sucesso!" : "Checkout criado com sucesso!");
                    resetForm();
                    carregarCheckouts();
                } else {
                    abrirModal("Erro", "Erro ao salvar checkout no servidor.");
                }
            } catch (err) {
                abrirModal("Erro", "Erro ao conectar com o servidor.");
            }
        });

        carregarCheckouts();
    </script>
</body>

</html>