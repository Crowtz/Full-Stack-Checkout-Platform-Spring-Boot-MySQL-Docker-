package lucas.hjds.email;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	// Método para anexos vindo do banco de dados (Caminhos em String)
	public void sendDynamicEmail(EmailConfigRequest config, List<String> attachmentPaths) throws Exception {
		JavaMailSenderImpl mailSender = criarMailSender(config);

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(config.getUsername());
		helper.setTo(config.getTo());
		helper.setSubject(config.getSubject());
		helper.setText(config.getBodyHtml(), true);

		if (attachmentPaths != null) {
			for (String path : attachmentPaths) {
				if (path != null && !path.trim().isEmpty()) {
					File file = new File(path);
					if (file.exists()) {
						helper.addAttachment(file.getName(), file);
					} else {
						System.err.println("[EMAIL LOG] Anexo não encontrado no caminho: " + path);
					}
				}
			}
		}

		mailSender.send(message);
	}

	// Sobrecarga para MultipartFile (caso use no upload direto)
	public void sendDynamicEmailMultipart(EmailConfigRequest config, List<MultipartFile> attachments) throws Exception {
		JavaMailSenderImpl mailSender = criarMailSender(config);

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(config.getUsername());
		helper.setTo(config.getTo());
		helper.setSubject(config.getSubject());
		helper.setText(config.getBodyHtml(), true);

		if (attachments != null) {
			for (MultipartFile file : attachments) {
				if (file != null && !file.isEmpty()) {
					helper.addAttachment(file.getOriginalFilename(), file);
				}
			}
		}

		mailSender.send(message);
	}

	// Método utilitário para reaproveitar a configuração do SMTP
	private JavaMailSenderImpl criarMailSender(EmailConfigRequest config) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(config.getHost());
		mailSender.setPort(config.getPort());
		mailSender.setUsername(config.getUsername());
		mailSender.setPassword(config.getPassword());

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");

		if (config.getPort() != null && config.getPort() == 465) {
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.ssl.enable", "true");
		} else {
			props.put("mail.smtp.starttls.enable", "true");
		}

		props.put("mail.debug", "false");
		return mailSender;
	}
}