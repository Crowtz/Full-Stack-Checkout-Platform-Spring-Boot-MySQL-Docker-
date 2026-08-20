<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout de Pagamento</title>
    <script src="https://sdk.mercadopago.com/js/v2"></script>
    <link rel="stylesheet" href="style.css">
</head>

<body>

    <div id="toast">Copiado com sucesso!</div>

    <div id="modalOverlay" class="modal-overlay">
        <div class="modal-box">
            <h3 id="modalTitle">Aviso</h3>
            <p id="modalMessage">Mensagem do popup.</p>
            <button id="modalBtnClose" class="btn-modal-close" onclick="closeModal()">Ok</button>
        </div>
    </div>

    <div class="checkout-container">

        <!-- Resumo do Produto Configurável -->
        <div class="product-summary">
            <img id="display_prod_img" src="" alt="Produto">
            <div class="product-info">
                <h3 id="display_prod_title" class="product-title"></h3>
                <p id="display_prod_desc" class="product-desc"></p>
                <div class="price-container">
                    <span id="display_prod_original" class="product-price-original" style="display: none;"></span>
                    <span id="display_prod_price" class="product-price"></span>
                    <span id="display_prod_discount" class="product-discount-badge" style="display: none;"></span>
                </div>
            </div>
        </div>

        <div class="form-group">
            <label class="required">Nome completo / Razão Social</label>
            <input
                type="text"
                id="cust_name"
                placeholder="Seu nome e sobrenome"
                value=""
                required
                minlength="6"
                pattern="^[A-Za-zÀ-ÿ0-9]+(\s+[A-Za-zÀ-ÿ0-9]+)+$"
                title="Digite ao menos o nome e o sobrenome separados por espaço.">
        </div>

        <div class="form-row">
            <div class="form-group">
                <label class="required">E-mail</label>
                <input type="email" id="cust_email" placeholder="exemplo@email.com" value="">
                <span class="subtext">O e-mail usado para receber a compra.</span>
            </div>
            <div class="form-group">
                <label>Celular (Opcional)</label>
                <input type="tel" id="cust_phone" placeholder="(99) 99999-9999" maxlength="15" value="">
            </div>
        </div>

        <div class="payment-header">
            <label style="margin: 0;">Forma de pagamento</label>
            <div class="secure-badge">
                <img src="https://http2.mlstatic.com/frontend-assets/ui-navigation/5.19.1/mercadopago/logo__small.png"
                    alt="Mercado Pago">
                Seguro
            </div>
        </div>

        <div class="payment-methods">
            <div class="pm-option active" onclick="selectPayment('card', event)">Cartão</div>
            <div class="pm-option" onclick="selectPayment('pix', event)">Pix</div>
        </div>

        <!-- Cartão -->
        <div id="area-card">
            <div id="cardPaymentBrick_container"></div>
        </div>

        <!-- Pix -->
        <div id="area-pix" style="display:none">
            <button id="btnPix" class="btn-submit">Gerar Pix</button>
            <div id="pixResult" class="result-box" style="display:none">
                <img id="pixQrImg" alt="QR Code Pix" /><br>
                <label>Clique abaixo para copiar o Código Pix:</label>
                <input id="pixCopiaCola" class="copy-input" readonly onclick="copyToClipboard(this.value)" />
            </div>
        </div>

    </div>

    <script>
        // O PHP imprime as variáveis como strings válidas para o JavaScript
        const urlParams = new URLSearchParams(window.location.search);
        const checkoutId = urlParams.get('id');

        const BASE_API_URL = <?php echo json_encode(getenv('BASE_URL_SPRING') ?: ($_ENV['BASE_URL_SPRING'] ?? '')); ?>;
        const MP_PUBLIC_KEY = <?php echo json_encode(getenv('MP_PUBLIC_KEY') ?: ($_ENV['MP_PUBLIC_KEY'] ?? '')); ?>;

        const CONFIG = {
            baseUrlSpring: BASE_API_URL,
            mpPublicKey: MP_PUBLIC_KEY,
            product: {
                title: "Carregando...",
                description: "",
                price: 0,
                imageUrl: ""
            }
        };


        let mp;
        let bricksBuilder;

        try {
            mp = new MercadoPago(CONFIG.mpPublicKey, {
                locale: 'pt-BR'
            });
            bricksBuilder = mp.bricks();
        } catch (error) {
            console.error("Erro ao inicializar o Mercado Pago:", error);
        }
        // Cartão
        const renderCardBrick = async () => {
            if (!CONFIG.product || !CONFIG.product.price || Number(CONFIG.product.price) <= 0) {
                return;
            }

            const getEmail = () => document.getElementById("cust_email").value.trim() || "";

            const settings = {
                initialization: {
                    amount: CONFIG.product.price,
                    payer: {
                        email: getEmail()
                    }
                },
                customization: {
                    paymentMethods: {
                        maxInstallments: 12
                    }
                },
                callbacks: {
                    onReady: () => {},
                    onSubmit: async (cardFormData) => {
                        if (!validarFormulario()) return;

                        const rawName = formatarNome(document.getElementById("cust_name").value);
                        const nameParts = rawName.split(" ").filter(Boolean);
                        const firstName = nameParts[0] || "Cliente";
                        const lastName = nameParts.slice(1).join(" ") || "";

                        const cust_email = document.getElementById("cust_email").value.trim();
                        const phoneData = extrairTelefone(document.getElementById("cust_phone").value);

                        const payload = {
                            type: "online",
                            processing_mode: "automatic",
                            external_reference: "",
                            total_amount: Number(cardFormData.transaction_amount).toFixed(2),
                            payer: {
                                email: cust_email,
                                first_name: firstName,
                                last_name: lastName,
                                phone: {
                                    area_code: phoneData.area_code,
                                    number: phoneData.number
                                },
                            },
                            transactions: {
                                payments: [{
                                    amount: Number(cardFormData.transaction_amount).toFixed(2),
                                    payment_method: {
                                        id: cardFormData.payment_method_id,
                                        type: "credit_card",
                                        token: cardFormData.token,
                                        installments: Number(cardFormData.installments)
                                    }
                                }]
                            }
                        };

                        try {
                            const response = await fetch(`${CONFIG.baseUrlSpring}/process_order_card`, {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/json",
                                    "ngrok-skip-browser-warning": "true"
                                },
                                body: JSON.stringify(payload)
                            });
                            const order = await response.json();

                            if (order.errors) {
                                showModal("Erro no Pagamento", order.errors[0]?.message);
                                return;
                            }
                            monitorarPagamento(order.id);
                        } catch (err) {
                            showModal("Erro", "Erro ao conectar com o servidor.");
                        }
                    },
                    onError: (error) => console.error(error),
                },
            };

            if (window.cardBrickController && typeof window.cardBrickController.unmount === 'function') {
                try {
                    await window.cardBrickController.unmount();
                } catch (e) {}
            }

            window.cardBrickController = await bricksBuilder.create('cardPayment', 'cardPaymentBrick_container', settings);
        };


        async function carregarDadosCheckout() {
            if (!checkoutId) {
                renderCardBrick();
                return;
            }

            try {
                const res = await fetch(`${CONFIG.baseUrlSpring}/api/checkouts/${checkoutId}`, {
                    method: "GET",
                    headers: {
                        "ngrok-skip-browser-warning": "true"
                    }
                });

                if (!res.ok) {
                    console.error("Checkout não encontrado no banco.");
                    return;
                }

                const data = await res.json();

                data.price = Number(data.price);
                CONFIG.product = data;

                const titleEl = document.getElementById("display_prod_title");
                if (titleEl) titleEl.innerText = data.title;

                const descEl = document.getElementById("display_prod_desc");
                if (descEl) descEl.innerText = data.description || '';

                const imgEl = document.getElementById("display_prod_img");
                if (imgEl) imgEl.src = data.imageUrl || 'https://via.placeholder.com/150';

                const priceEl = document.getElementById("display_prod_price");
                if (priceEl) priceEl.innerText = `R$ ${data.price.toFixed(2).replace('.', ',')}`;

                // Trata Preço Original (riscado)
                const origEl = document.getElementById("display_prod_original");
                if (origEl) {
                    if (data.originalPrice) {
                        origEl.innerText = `R$ ${Number(data.originalPrice).toFixed(2).replace('.', ',')}`;
                        origEl.style.display = "inline";
                    } else {
                        origEl.style.display = "none";
                    }
                }

                // Trata % Desconto
                const discEl = document.getElementById("display_prod_discount");
                if (discEl) {
                    if (data.discountPercent) {
                        discEl.innerText = `${data.discountPercent}% OFF`;
                        discEl.style.display = "inline";
                    } else {
                        discEl.style.display = "none";
                    }
                }

                renderCardBrick();

            } catch (err) {
                console.error("Erro ao carregar dados do checkout:", err);
            }
        }

        carregarDadosCheckout();

        function formatarNome(nome) {
            return nome.replace(/[^a-zA-ZÀ-ÿ\s]/g, "").trim();
        }

        function aplicarMascaraTelefone(value) {
            let nums = value.replace(/\D/g, "");
            if (nums.length > 11) nums = nums.substring(0, 11);

            if (nums.length === 0) return "";
            if (nums.length <= 2) return `(${nums}`;
            if (nums.length <= 6) return `(${nums.substring(0, 2)}) ${nums.substring(2)}`;
            if (nums.length <= 10) return `(${nums.substring(0, 2)}) ${nums.substring(2, 6)}-${nums.substring(6)}`;

            return `(${nums.substring(0, 2)}) ${nums.substring(2, 7)}-${nums.substring(7)}`;
        }

        function extrairTelefone(telefoneRaw) {
            const apenasNumeros = telefoneRaw.replace(/\D/g, "");
            if (apenasNumeros.length >= 10) {
                return {
                    area_code: apenasNumeros.substring(0, 2),
                    number: apenasNumeros.substring(2)
                };
            }
            return {
                area_code: "",
                number: apenasNumeros
            };
        }

        document.getElementById("cust_name").addEventListener("blur", (e) => {
            e.target.value = formatarNome(e.target.value);
        });

        const phoneInput = document.getElementById("cust_phone");

        phoneInput.addEventListener("keydown", (e) => {
            if (e.key === "Backspace") {
                const val = e.target.value;
                const cursorPos = e.target.selectionStart;

                if (cursorPos > 0 && /\D/.test(val[cursorPos - 1])) {
                    e.preventDefault();
                    let nums = val.replace(/\D/g, "");
                    nums = nums.substring(0, nums.length - 1);
                    e.target.value = aplicarMascaraTelefone(nums);
                }
            }
        });

        phoneInput.addEventListener("input", (e) => {
            e.target.value = aplicarMascaraTelefone(e.target.value);
        });

        if (phoneInput.value) {
            phoneInput.value = aplicarMascaraTelefone(phoneInput.value);
        }

        function showModal(title, msg, showButton = true) {
            document.getElementById("modalTitle").innerText = title;
            document.getElementById("modalMessage").innerText = msg;
            document.getElementById("modalBtnClose").style.display = showButton ? "inline-block" : "none";
            document.getElementById("modalOverlay").style.display = "flex";
        }

        function closeModal() {
            document.getElementById("modalOverlay").style.display = "none";
        }

        function showToast(text = "Copiado para a área de transferência!") {
            const toast = document.getElementById("toast");
            toast.innerText = text;
            toast.classList.add("show");
            setTimeout(() => toast.classList.remove("show"), 2500);
        }

        function copyToClipboard(value) {
            if (!value) return;
            navigator.clipboard.writeText(value).then(() => showToast()).catch(() => showModal("Erro", "Não foi possível copiar."));
        }

        function validarEmail(email) {
            return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        }

        function validarNomeSobrenome() {
            const inputNome = document.getElementById('cust_name');
            const valor = inputNome.value.trim();

            const partesNome = valor.split(/\s+/);

            if (partesNome.length < 2 || partesNome.some(p => p.length < 2)) {
                inputNome.focus();
                return false;
            }

            return true;
        }

        function validarFormulario(isBoleto = false) {
            const name = formatarNome(document.getElementById("cust_name").value);
            const email = document.getElementById("cust_email").value.trim();

            if (!name) {
                showModal("Campo Obrigatório", "Por favor, preencha o seu nome completo antes de prosseguir.");
                return false;
            }

            if (!validarNomeSobrenome()) {
                showModal("Campo Obrigatório", "Por favor, preencha o seu nome e sobrenome antes de prosseguir.");
                return false;
            }

            if (!email) {
                showModal("Campo Obrigatório", "Por favor, preencha o seu e-mail antes de prosseguir.");
                return false;
            }

            if (!validarEmail(email)) {
                showModal("E-mail Inválido", "Por favor, insira um e-mail válido (ex: exemplo@email.com).");
                return false;
            }
            return true;
        }

        function selectPayment(type, event) {
            document.querySelectorAll('.pm-option').forEach(el => el.classList.remove('active'));
            if (event) event.currentTarget.classList.add('active');

            document.getElementById("area-card").style.display = type === "card" ? "block" : "none";
            document.getElementById("area-pix").style.display = type === "pix" ? "block" : "none";
        }

        function redirecionarComContagem(orderId) {
            const redirectUrl = CONFIG.product?.redirectUrl;

            // Caso não haja URL de redirecionamento configurada
            if (!redirectUrl) {
                showModal(
                    "Pagamento Aprovado!",
                    "\nVerifique seu e-mail para acessar o produto.",
                    true
                );
                return;
            }

            // Caso exista URL de redirecionamento
            let segundos = 5;

            const atualizarMensagem = () => {
                showModal(
                    "Pagamento Aprovado!",
                    `\nRedirecionando em ${segundos} segundo${segundos > 1 ? 's' : ''}...`,
                    false
                );
            };

            atualizarMensagem();

            const timer = setInterval(() => {
                segundos--;
                if (segundos > 0) {
                    atualizarMensagem();
                } else {
                    clearInterval(timer);
                    window.location.href = redirectUrl;
                }
            }, 1000);
        }

        let paymentPollingInterval = null;

        function monitorarPagamento(orderId) {
            if (paymentPollingInterval) {
                clearInterval(paymentPollingInterval);
            }

            paymentPollingInterval = setInterval(async () => {
                try {
                    const response = await fetch(
                        `${CONFIG.baseUrlSpring}/api/payments/status/${encodeURIComponent(orderId)}`, {
                            method: "GET",
                            headers: {
                                "ngrok-skip-browser-warning": "true"
                            }
                        }
                    );

                    if (!response.ok) return;

                    const data = await response.json();

                    if (data.approved === true) {
                        clearInterval(paymentPollingInterval);
                        paymentPollingInterval = null;
                        redirecionarComContagem(orderId);
                        return;
                    }

                } catch (error) {
                    console.error("Erro consultando status:", error);
                }
            }, 6000);
        }

        // Pix
        document.getElementById("btnPix").addEventListener("click", async () => {
            if (!validarFormulario(false)) return;

            const rawName = formatarNome(document.getElementById("cust_name").value);
            const nameParts = rawName.split(" ").filter(Boolean);
            const firstName = nameParts[0] || "Cliente";
            const lastName = nameParts.slice(1).join(" ") || "";

            const phoneData = extrairTelefone(document.getElementById("cust_phone").value);

            const payload = {
                type: "online",
                processing_mode: "automatic",
                external_reference: "" + checkoutId,
                total_amount: CONFIG.product.price.toFixed(2),
                payer: {
                    email: document.getElementById("cust_email").value.trim(),
                    first_name: firstName,
                    last_name: lastName,
                    phone: {
                        area_code: phoneData.area_code,
                        number: phoneData.number
                    },
                },
                transactions: {
                    payments: [{
                        amount: CONFIG.product.price.toFixed(2),
                        payment_method: {
                            id: "pix",
                            type: "bank_transfer"
                        }
                    }]
                }
            };

            try {
                const r = await fetch(`${CONFIG.baseUrlSpring}/process_order_pix`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "ngrok-skip-browser-warning": "true"
                    },
                    body: JSON.stringify(payload)
                });

                const order = await r.json();
                const payment = order?.transactions?.payments?.[0];
                const pm = payment?.payment_method;

                if (pm?.qr_code_base64) {
                    document.getElementById("pixResult").style.display = "block";
                    document.getElementById("pixQrImg").src = `data:image/png;base64,${pm.qr_code_base64}`;
                    document.getElementById("pixCopiaCola").value = pm.qr_code || "";

                    monitorarPagamento(order.id);
                }
            } catch (err) {
                showModal("Erro", "Falha ao gerar Pix. Verifique a conexão.");
            }
        });

        document.getElementById("cust_email").addEventListener("blur", () => {
            renderCardBrick();
        });
    </script>
</body>

</html>