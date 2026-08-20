package lucas.hjds.payment;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;
import lucas.hjds.mysql.PaymentDAO;
import lucas.hjds.mysql.PaymentData;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentServiceOrder {

	@Value("${mercadopago.access.token}")
	private String accessToken;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	public void init() {
		MercadoPagoConfig.setAccessToken(accessToken);
		System.out.println(">>> Access Token carregado com sucesso!");
	}

	@SuppressWarnings("deprecation")
	public ResponseEntity<String> createOrder(String bodyJson) throws Exception {

		JsonNode requestJson = objectMapper.readTree(bodyJson);

		String payerEmail = requestJson.path("payer").path("email").asText(null);

		String checkoutIdStr = requestJson.path("external_reference").asText(null);
		Long checkoutId = (checkoutIdStr != null && !checkoutIdStr.isEmpty()) ? Long.parseLong(checkoutIdStr) : null;

		String payerFirstName = requestJson.path("payer").path("first_name").asText(null);

		String payerPhone = null;
		JsonNode phoneNode = requestJson.path("payer").path("phone");
		if (!phoneNode.isMissingNode() && !phoneNode.isNull()) {
			String areaCode = phoneNode.path("area_code").asText("");
			String number = phoneNode.path("number").asText("");
			payerPhone = areaCode + number;
		}

		String idempotencyKey = UUID.randomUUID().toString();

		var req = HttpRequest.newBuilder().uri(URI.create("https://api.mercadopago.com/v1/orders"))
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.header("X-Idempotency-Key", idempotencyKey).POST(HttpRequest.BodyPublishers.ofString(bodyJson))
				.build();

		var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());

		String responseBody = res.body();

		if (res.statusCode() < 200 || res.statusCode() >= 300) {
			return ResponseEntity.status(res.statusCode()).body(responseBody);
		}

		JsonNode responseJson = objectMapper.readTree(responseBody);

		String orderId = responseJson.path("id").asText(null);

		if (orderId == null || orderId.isBlank()) {
			return ResponseEntity.status(500).body(responseBody);
		}

		String paymentId = null;

		JsonNode payments = responseJson.path("transactions").path("payments");

		if (payments.isArray() && !payments.isEmpty()) {
			paymentId = payments.get(0).path("id").asText(null);
		}

		String status = responseJson.path("status").asText(null);
		String statusDetail = responseJson.path("status_detail").asText(null);

		BigDecimal totalAmount = BigDecimal.ZERO;

		String totalAmountString = responseJson.path("total_amount").asText(null);

		if (totalAmountString != null && !totalAmountString.isBlank()) {
			totalAmount = new BigDecimal(totalAmountString);
		}

		String paymentMethod = null;
		if (payments.isArray() && !payments.isEmpty()) {
			paymentMethod = payments.get(0).path("payment_method").path("id").asText(null);
		}

		PaymentData data = new PaymentData();
		data.setOrderId(orderId);
		data.setCheckoutId(checkoutId);
		data.setPaymentId(paymentId);
		data.setStatus(status);
		data.setStatusDetail(statusDetail);
		data.setTotalAmount(totalAmount);
		data.setPayerEmail(payerEmail);
		data.setPayerPhone(payerPhone);
		data.setPayerFirstName(payerFirstName);
		data.setPaymentMethod(paymentMethod);
		data.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		data.setApprovedAt(null);

		PaymentDAO paymentDao = new PaymentDAO();
		paymentDao.createPendingOrder(data);

		return ResponseEntity.status(res.statusCode()).body(responseBody);
	}
}