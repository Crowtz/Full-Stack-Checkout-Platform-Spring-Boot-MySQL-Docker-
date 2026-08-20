package lucas.hjds.checkout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/checkouts")
public class CheckoutController {

	private static final String UPLOAD_DIR = "uploads/attachments/";

	@Value("${app.base-url}")
	private String baseUrl;

	@Value("${mercadopago.public-key}")
	private String mpPublicKey;

	@GetMapping("/pubkey")
	public ResponseEntity<Map<String, String>> getConfig() {
		Map<String, String> config = new HashMap<>();
		config.put("baseUrl", baseUrl);
		config.put("mpPublicKey", mpPublicKey);
		return ResponseEntity.ok(config);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> criar(@RequestPart("checkout") String checkoutJson,
			@RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CheckoutModel model = mapper.readValue(checkoutJson, CheckoutModel.class);

			CheckoutModel salvo = CheckoutDAO.salvar(model);

			if (attachments != null && attachments.length > 0) {
				salvarAnexos(salvo.getId(), attachments);
			}

			return ResponseEntity.ok(salvo);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Erro ao salvar checkout: " + e.getMessage());
		}
	}

	@GetMapping
	public ResponseEntity<List<CheckoutModel>> listar() {
		try {
			return ResponseEntity.ok(CheckoutDAO.listarTodos());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
		try {
			CheckoutModel item = CheckoutDAO.buscarPorId(id);
			if (item == null) {
				return ResponseEntity.notFound().build();
			}

			// Carrega os anexos vinculados e insere no objeto do checkout
			item.setAttachments(CheckoutDAO.buscarAnexosPorCheckoutId(id));

			return ResponseEntity.ok(item);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Erro: " + e.getMessage());
		}
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestPart("checkout") String checkoutJson,
			@RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CheckoutModel model = mapper.readValue(checkoutJson, CheckoutModel.class);

			model.setId(id);
			CheckoutDAO.atualizar(model);

			if (attachments != null && attachments.length > 0) {
				salvarAnexos(id, attachments);
			}

			return ResponseEntity.ok(model);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Erro ao atualizar: " + e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deletar(@PathVariable Long id) {
		try {
			CheckoutDAO.deletar(id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Erro ao deletar: " + e.getMessage());
		}
	}

	private void salvarAnexos(Long checkoutId, MultipartFile[] files) throws IOException {
		// Define o caminho absoluto para salvar os arquivos
		Path pastaDestino = Paths.get(UPLOAD_DIR, checkoutId.toString()).toAbsolutePath().normalize();

		// Garante a criação de todos os diretórios necessários
		if (!Files.exists(pastaDestino)) {
			Files.createDirectories(pastaDestino);
		}

		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				Path arquivoDestino = pastaDestino.resolve(file.getOriginalFilename());

				Files.copy(file.getInputStream(), arquivoDestino, StandardCopyOption.REPLACE_EXISTING);

				try {
					CheckoutDAO.salvarAnexo(checkoutId, file.getOriginalFilename(), arquivoDestino.toString(),
							file.getContentType());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@DeleteMapping("/attachments/{attachmentId}")
	public ResponseEntity<?> removeAttachment(@PathVariable("attachmentId") Long attachmentId) {
		try {
			CheckoutDAO.removerAnexoPorId(attachmentId);
			return ResponseEntity.ok().body("{\"message\": \"Anexo removido com sucesso.\"}");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

}