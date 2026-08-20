package lucas.hjds.email;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lucas.hjds.checkout.CheckoutDAO;

@RestController
@RequestMapping("/api/email")
public class EmailController {

	private final EmailService emailService;

	public EmailController(EmailService emailService) {
		this.emailService = emailService;
	}

	@PostMapping(value = "/send", consumes = { "multipart/form-data" })
	public ResponseEntity<?> sendEmail(@RequestPart("config") EmailConfigRequest config,
			@RequestPart(value = "files", required = false) List<MultipartFile> files) {
		try {
			// Chama a sobrecarga que aceita List<MultipartFile>
			emailService.sendDynamicEmailMultipart(config, files);
			return ResponseEntity.ok().body("{\"message\": \"E-mail enviado com sucesso!\"}");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	@PostMapping("/test-email")
	public ResponseEntity<?> sendTestEmail(@RequestParam("checkoutId") Long checkoutId,
			@RequestParam("targetEmail") String targetEmail) {
		try {
			var checkout = CheckoutDAO.buscarPorId(checkoutId);
			if (checkout == null) {
				return ResponseEntity.badRequest().body("{\"error\": \"Checkout não encontrado.\"}");
			}

			EmailConfigRequest config = new EmailConfigRequest();
			config.setHost(checkout.getSmtpHost());
			config.setPort(checkout.getSmtpPort() != null ? checkout.getSmtpPort() : 587);
			config.setUsername(checkout.getSmtpUser());
			config.setPassword(checkout.getSmtpPass());
			config.setTo(targetEmail);

			String subject = checkout.getEmailSubject() != null ? checkout.getEmailSubject() : "E-mail de Teste";
			subject = subject.replace("{nome}", "Teste");

			String bodyHtml = checkout.getEmailBodyHtml() != null ? checkout.getEmailBodyHtml() : "";
			bodyHtml = bodyHtml.replace("{nome}", "Teste").replace("{link}",
					checkout.getRedirectUrl() != null ? checkout.getRedirectUrl() : "#");

			config.setSubject("[TESTE] " + subject);
			config.setBodyHtml(bodyHtml);

			List<String> anexos = CheckoutDAO.buscarCaminhosAnexos(checkoutId);
			emailService.sendDynamicEmail(config, anexos);

			return ResponseEntity.ok().body("{\"message\": \"E-mail de teste enviado com sucesso!\"}");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

}