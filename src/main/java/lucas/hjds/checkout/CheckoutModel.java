package lucas.hjds.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CheckoutModel {
	private Long id;
	private String title;
	private String description;
	private BigDecimal originalPrice;
	private BigDecimal price;
	private Integer discountPercent;
	private String imageUrl;
	private String redirectUrl;

	// Configurações SMTP / E-mail Pós-Venda
	private Boolean sendEmailEnabled = true;
	private String smtpHost;
	private Integer smtpPort;
	private String smtpUser;
	private String smtpPass;
	private String emailSubject;
	private String emailBodyHtml;

	private List<Map<String, Object>> attachments;

	public CheckoutModel() {
	}

	public CheckoutModel(Long id, String title, String description, BigDecimal originalPrice, BigDecimal price,
			Integer discountPercent, String imageUrl, String redirectUrl, Boolean sendEmailEnabled, String smtpHost,
			Integer smtpPort, String smtpUser, String smtpPass, String emailSubject, String emailBodyHtml) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.originalPrice = originalPrice;
		this.price = price;
		this.discountPercent = discountPercent;
		this.imageUrl = imageUrl;
		this.redirectUrl = redirectUrl;
		this.sendEmailEnabled = sendEmailEnabled != null ? sendEmailEnabled : false;
		this.smtpHost = smtpHost;
		this.smtpPort = smtpPort;
		this.smtpUser = smtpUser;
		this.smtpPass = smtpPass;
		this.emailSubject = emailSubject;
		this.emailBodyHtml = emailBodyHtml;
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getDiscountPercent() {
		return discountPercent;
	}

	public void setDiscountPercent(Integer discountPercent) {
		this.discountPercent = discountPercent;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public Boolean getSendEmailEnabled() {
		return sendEmailEnabled != null ? sendEmailEnabled : false;
	}

	public void setSendEmailEnabled(Boolean sendEmailEnabled) {
		this.sendEmailEnabled = sendEmailEnabled;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public Integer getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(Integer smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getSmtpUser() {
		return smtpUser;
	}

	public void setSmtpUser(String smtpUser) {
		this.smtpUser = smtpUser;
	}

	public String getSmtpPass() {
		return smtpPass;
	}

	public void setSmtpPass(String smtpPass) {
		this.smtpPass = smtpPass;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public String getEmailBodyHtml() {
		return emailBodyHtml;
	}

	public void setEmailBodyHtml(String emailBodyHtml) {
		this.emailBodyHtml = emailBodyHtml;
	}

	public List<Map<String, Object>> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Map<String, Object>> attachments) {
		this.attachments = attachments;
	}
}