package lucas.hjds.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

	private final PaymentServiceOrder paymentService;

	public PaymentController(PaymentServiceOrder paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/process_order_pix")
	public ResponseEntity<String> pix(@RequestBody String json) throws Exception {
		return paymentService.createOrder(json);
	}

	@PostMapping("/process_order_boleto")
	public ResponseEntity<String> boleto(@RequestBody String json) throws Exception {
		return paymentService.createOrder(json);
	}

	@PostMapping("/process_order_card")
	public ResponseEntity<String> card(@RequestBody String json) throws Exception {
		return paymentService.createOrder(json);
	}
}