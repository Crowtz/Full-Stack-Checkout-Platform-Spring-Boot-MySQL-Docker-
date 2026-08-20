package lucas.hjds.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lucas.hjds.mysql.PaymentDAO;
import lucas.hjds.mysql.PaymentData;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings("deprecation")
@RestController
@RequestMapping("/webhooks")
public class MercadoPagoWebhookController {

	@Value("${mercadopago.webhook.secret:}")
	private String webhookSecret;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping
	public ResponseEntity<String> validate() {
		return ResponseEntity.ok("Webhook ativo!");
	}

	@PostMapping
	public ResponseEntity<Void> receive(@RequestHeader(value = "x-signature", required = false) String xSignature,
			@RequestHeader(value = "x-request-id", required = false) String xRequestId,
			@RequestBody(required = false) String bodyJson, HttpServletRequest request) {

		System.out.println();
		System.out.println("----------------- NOVO WEBHOOK RECEBIDO -----------------");

		// ========================================================
		// 1. PEGAR DATA.ID DIRETAMENTE DA QUERY STRING
		// ========================================================

		String dataId = request.getParameter("data.id");

		String notificationId = request.getParameter("id");

		String type = request.getParameter("type");

		System.out.println("- data.id: " + dataId + " id: " + notificationId + " type: " + type);
		System.out.println("- URI da Requisição: " + request.getRequestURI()
				+ (request.getQueryString() != null ? "?" + request.getQueryString() : ""));

		JsonNode rootNode = null;
		if (bodyJson != null && !bodyJson.isBlank()) {
			try {
				rootNode = objectMapper.readTree(bodyJson);
			} catch (Exception e) {
				System.err.println("Erro ao ler JSON do webhook: " + e.getMessage());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}
		}

		// ========================================================
		// 3. VALIDAR ASSINATURA
		// ========================================================

		if (webhookSecret == null || webhookSecret.isBlank()) {
			System.err.println("WEBHOOK SECRET NÃO CONFIGURADA!");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		if (xSignature == null || xSignature.isBlank()) {
			System.err.println("Header x-signature ausente!");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (xRequestId == null || xRequestId.isBlank()) {
			System.err.println("Header x-request-id ausente!");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (dataId == null || dataId.isBlank()) {
			System.err.println("Parâmetro data.id ausente!");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		boolean valid = validateSignature(xSignature, xRequestId, dataId);

		System.out.println("- Validação da Assinatura: " + (valid ? "AUTÊNTICA (OK)" : "INVÁLIDA / FORJADA"));

		if (!valid) {
			System.err.println("Requisição rejeitada: Assinatura inválida.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (rootNode == null) {
			System.out.println("Corpo JSON vazio.");
			return ResponseEntity.ok().build();
		}

		try {
			JsonNode dataNode = rootNode.path("data");
			String orderId = dataNode.path("id").asText(null);

			if (orderId == null || orderId.isBlank()) {
				orderId = dataId;
			}

			String status = dataNode.path("status").asText(null);

			if (status == null) {
				status = "null";
			}

			String statusDetail = dataNode.path("status_detail").asText(null);

			String paymentId = null;
			String paymentMethod = null;

			JsonNode paymentsArray = dataNode.path("transactions").path("payments");

			if (paymentsArray.isArray() && !paymentsArray.isEmpty()) {

				JsonNode firstPayment = paymentsArray.get(0);

				if (firstPayment.has("id")) {
					paymentId = firstPayment.get("id").asText();
				}

				if (firstPayment.has("status")) {
					status = firstPayment.get("status").asText();
				}

				if (firstPayment.has("status_detail")) {
					statusDetail = firstPayment.get("status_detail").asText();
				}

				JsonNode paymentMethodNode = firstPayment.path("payment_method");

				if (paymentMethodNode.has("id")) {
					paymentMethod = paymentMethodNode.get("id").asText();
				}

			}

			Timestamp approvedAt = null;
			if ("processed".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(status)) {
				approvedAt = new Timestamp(System.currentTimeMillis());
			}

			// ====================================================
			// BANCO
			// ====================================================

			if (orderId != null && !orderId.isBlank()) {

				PaymentData data = new PaymentData();
				data.setOrderId(orderId);
				data.setPaymentId(paymentId);
				data.setStatus(status);
				data.setStatusDetail(statusDetail);
				data.setPaymentMethod(paymentMethod);
				data.setApprovedAt(approvedAt);

				PaymentDAO paymentDao = new PaymentDAO();
				paymentDao.updateStatus(data);

				System.out.println(">>> [DB] Order Update " + orderId + " (Status: " + status + " | Detail: "
						+ statusDetail + ") gravada!");
			}

		} catch (Exception e) {
			System.err.println(">>> Erro ao processar JSON do Webhook: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		return ResponseEntity.ok().build();
	}

	private boolean validateSignature(String xSignature, String xRequestId, String dataId) {
		try {
			if (xSignature == null || xSignature.isBlank()) {
				System.err.println("!!! x-signature ausente!");
				return false;
			}

			if (xRequestId == null || xRequestId.isBlank()) {
				System.err.println("!!! x-request-id ausente!");
				return false;
			}

			if (dataId == null || dataId.isBlank()) {
				System.err.println("!!! data.id ausente!");
				return false;
			}

			String ts = null;
			String receivedHash = null;

			for (String part : xSignature.split(",")) {

				String[] keyValue = part.trim().split("=", 2);

				if (keyValue.length != 2) {
					continue;
				}

				String key = keyValue[0].trim();
				String value = keyValue[1].trim();

				if ("ts".equalsIgnoreCase(key)) {
					ts = value;
				}

				if ("v1".equalsIgnoreCase(key)) {
					receivedHash = value;
				}
			}

			if (ts == null || ts.isBlank()) {
				System.err.println("!!! ts não encontrado!");
				return false;
			}

			if (receivedHash == null || receivedHash.isBlank()) {
				System.err.println("!!! v1 não encontrado!");
				return false;
			}

			String normalizedDataId = dataId.toLowerCase(java.util.Locale.ROOT);

			String manifest = "id:" + normalizedDataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";

			System.out.println(">>> data.id original: " + dataId);

			System.out.println(">>> data.id usado no HMAC: " + normalizedDataId);

			System.out.println(">>> Manifesto calculado: " + manifest);

			Mac mac = Mac.getInstance("HmacSHA256");

			SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.trim().getBytes(StandardCharsets.UTF_8),
					"HmacSHA256");

			mac.init(secretKey);

			byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

			String calculatedHash = bytesToHex(hash);

			System.out.println(">>> HMAC recebido:  " + receivedHash);

			System.out.println(">>> HMAC calculado: " + calculatedHash);

			boolean valid = MessageDigest.isEqual(
					calculatedHash.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8),

					receivedHash.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));

			if (valid) {
				System.out.println("- HMAC Válido encontrado!");
			} else {
				System.err.println("- HMAC NÃO corresponde!");
			}

			return valid;

		} catch (Exception e) {
			System.err.println("Erro ao validar HMAC: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length * 2);

		for (byte b : bytes) {
			result.append(String.format("%02x", b & 0xff));
		}

		return result.toString();
	}

}