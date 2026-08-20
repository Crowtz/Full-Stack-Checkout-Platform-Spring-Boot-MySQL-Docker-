package lucas.hjds.payment.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lucas.hjds.checkout.CheckoutDAO;
import lucas.hjds.checkout.CheckoutModel;
import lucas.hjds.email.EmailConfigRequest;
import lucas.hjds.email.EmailService;
import lucas.hjds.mysql.PaymentDAO;

@RestController
@RequestMapping("/api/payments")
public class PaymentStatusController {

	private final PaymentDAO paymentDao = new PaymentDAO();

	@GetMapping("/status/{orderId}")
	public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable("orderId") String orderId) {
		Map<String, Object> response = new HashMap<>();
		Map<String, Object> orderData = paymentDao.getOrder(orderId);

		if (orderData == null || orderData.isEmpty()) {
			response.put("approved", false);
			response.put("status", "not_found");
			return ResponseEntity.ok(response);
		}

		boolean approved = paymentDao.isApproved(orderId);

		response.put("approved", approved);
		response.put("status", orderData.get("status"));
		response.put("status_detail", orderData.get("status_detail"));

		if (approved) {
			Object checkoutIdObj = orderData.get("checkout_id");

			System.out.println("");
			System.out.println("[EMAIL LOG] Pedido Aprovado: " + orderId);
			System.out.println("[EMAIL LOG] checkout_id retornado do banco: " + checkoutIdObj);

			if (checkoutIdObj != null) {
				try {
					Long checkoutId = Long.parseLong(checkoutIdObj.toString());
					CheckoutModel checkout = CheckoutDAO.buscarPorId(checkoutId);

					if (checkout == null) {
						System.err
								.println("[EMAIL LOG] Erro: NENHUM checkout encontrado no banco com ID: " + checkoutId);
					} else {
						if (Boolean.TRUE.equals(checkout.getSendEmailEnabled()) && checkout.getSmtpHost() != null
								&& !checkout.getSmtpHost().trim().isEmpty()) {

							EmailConfigRequest emailConfig = new EmailConfigRequest();
							emailConfig.setHost(checkout.getSmtpHost());
							emailConfig.setPort(checkout.getSmtpPort() != null ? checkout.getSmtpPort() : 587);
							emailConfig.setUsername(checkout.getSmtpUser());
							emailConfig.setPassword(checkout.getSmtpPass());

							String recipientEmail = (String) orderData.get("payer_email");
							emailConfig.setTo(recipientEmail);

							System.out.println("[EMAIL LOG] Enviando e-mail para: " + recipientEmail);

							String name = (String) orderData.get("payer_first_name");
							if (name == null || name.trim().isEmpty()) {
								name = "Cliente";
							}

							String subject = checkout.getEmailSubject() != null ? checkout.getEmailSubject()
									: "Status do seu pedido";
							subject = subject.replace("{nome}", name);

							String bodyHtml = checkout.getEmailBodyHtml() != null ? checkout.getEmailBodyHtml() : "";
							bodyHtml = bodyHtml.replace("{nome}", name).replace("{link}",
									checkout.getRedirectUrl() != null ? checkout.getRedirectUrl() : "");

							emailConfig.setSubject(subject);
							emailConfig.setBodyHtml(bodyHtml);

							List<String> anexos = CheckoutDAO.buscarCaminhosAnexos(checkoutId);

							EmailService emailService = new EmailService();
							emailService.sendDynamicEmail(emailConfig, anexos);
							System.out.println("[EMAIL LOG] E-mail enviado com sucesso!");

						} else {
							System.err.println("[EMAIL LOG] Envio cancelado: Envio desabilitado ou host SMTP ausente.");
						}
					}
				} catch (Exception e) {
					System.err.println("[EMAIL LOG] Exceção durante o processamento de e-mail:");
					e.printStackTrace();
				}
			} else {
				System.err.println("[EMAIL LOG] 'checkout_id' veio NULO na consulta do pedido " + orderId);
			}
		}
		System.out.println("");

		return ResponseEntity.ok(response);
	}
}