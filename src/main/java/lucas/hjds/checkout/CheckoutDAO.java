package lucas.hjds.checkout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lucas.hjds.mysql.Database;

public class CheckoutDAO {

	public static CheckoutModel salvar(CheckoutModel c) throws SQLException {
		String sql = "INSERT INTO checkouts (title, description, original_price, price, discount_percent, image_url, redirect_url, "
				+ "send_email_enabled, smtp_host, smtp_port, smtp_user, smtp_pass, email_subject, email_body_html) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection conn = Database.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, c.getTitle());
			stmt.setString(2, c.getDescription());
			stmt.setObject(3, c.getOriginalPrice());
			stmt.setBigDecimal(4, c.getPrice());
			stmt.setObject(5, c.getDiscountPercent());
			stmt.setString(6, c.getImageUrl());
			stmt.setString(7, c.getRedirectUrl());
			stmt.setBoolean(8, c.getSendEmailEnabled());
			stmt.setString(9, c.getSmtpHost());
			stmt.setObject(10, c.getSmtpPort());
			stmt.setString(11, c.getSmtpUser());
			stmt.setString(12, c.getSmtpPass());
			stmt.setString(13, c.getEmailSubject());
			stmt.setString(14, c.getEmailBodyHtml());

			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next())
					c.setId(rs.getLong(1));
			}
		}
		return c;
	}

	public static List<CheckoutModel> listarTodos() throws SQLException {
		List<CheckoutModel> lista = new ArrayList<>();
		String sql = "SELECT * FROM checkouts ORDER BY id DESC";
		try (Connection conn = Database.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				lista.add(mapResultSet(rs));
			}
		}
		return lista;
	}

	public static CheckoutModel buscarPorId(Long id) throws SQLException {
		String sql = "SELECT * FROM checkouts WHERE id = ?";
		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, id);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapResultSet(rs);
				}
			}
		}
		return null;
	}

	public static void atualizar(CheckoutModel c) throws SQLException {
		String sql = "UPDATE checkouts SET title = ?, description = ?, original_price = ?, price = ?, discount_percent = ?, "
				+ "image_url = ?, redirect_url = ?, send_email_enabled = ?, smtp_host = ?, smtp_port = ?, smtp_user = ?, "
				+ "smtp_pass = ?, email_subject = ?, email_body_html = ? WHERE id = ?";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, c.getTitle());
			stmt.setString(2, c.getDescription());
			stmt.setObject(3, c.getOriginalPrice());
			stmt.setBigDecimal(4, c.getPrice());
			stmt.setObject(5, c.getDiscountPercent());
			stmt.setString(6, c.getImageUrl());
			stmt.setString(7, c.getRedirectUrl());
			stmt.setBoolean(8, c.getSendEmailEnabled());
			stmt.setString(9, c.getSmtpHost());
			stmt.setObject(10, c.getSmtpPort());
			stmt.setString(11, c.getSmtpUser());
			stmt.setString(12, c.getSmtpPass());
			stmt.setString(13, c.getEmailSubject());
			stmt.setString(14, c.getEmailBodyHtml());
			stmt.setLong(15, c.getId());

			stmt.executeUpdate();
		}
	}

	public static void deletar(Long id) throws SQLException {
		String sql = "DELETE FROM checkouts WHERE id = ?";
		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, id);
			stmt.executeUpdate();
		}
	}

	private static CheckoutModel mapResultSet(ResultSet rs) throws SQLException {
		return new CheckoutModel(rs.getLong("id"), rs.getString("title"), rs.getString("description"),
				rs.getBigDecimal("original_price"), rs.getBigDecimal("price"),
				rs.getObject("discount_percent") != null ? rs.getInt("discount_percent") : null,
				rs.getString("image_url"), rs.getString("redirect_url"), rs.getBoolean("send_email_enabled"),
				rs.getString("smtp_host"), rs.getObject("smtp_port") != null ? rs.getInt("smtp_port") : null,
				rs.getString("smtp_user"), rs.getString("smtp_pass"), rs.getString("email_subject"),
				rs.getString("email_body_html"));
	}

	public static void salvarAnexo(Long checkoutId, String fileName, String filePath, String fileType)
			throws SQLException {
		String sql = "INSERT INTO checkout_attachments (checkout_id, file_name, file_path, file_type) VALUES (?, ?, ?, ?)";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, checkoutId);
			stmt.setString(2, fileName);
			stmt.setString(3, filePath);
			stmt.setString(4, fileType);

			stmt.executeUpdate();
		}
	}

	public static List<String> buscarCaminhosAnexos(Long checkoutId) throws SQLException {
		List<String> caminhos = new ArrayList<>();
		String sql = "SELECT file_path FROM checkout_attachments WHERE checkout_id = ?";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, checkoutId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					caminhos.add(rs.getString("file_path"));
				}
			}
		}
		return caminhos;
	}

	public static void removerAnexoPorId(Long attachmentId) {
		String selectSql = "SELECT file_path FROM checkout_attachments WHERE id = ?";
		String deleteSql = "DELETE FROM checkout_attachments WHERE id = ?";
		String filePath = null;

		try (Connection conn = Database.getConnection()) {
			try (PreparedStatement stmtSelect = conn.prepareStatement(selectSql)) {
				stmtSelect.setLong(1, attachmentId);
				try (ResultSet rs = stmtSelect.executeQuery()) {
					if (rs.next()) {
						filePath = rs.getString("file_path");
					}
				}
			}

			if (filePath == null) {
				System.out.println("Anexo não encontrado para o ID: " + attachmentId);
				return;
			}

			try (PreparedStatement stmtDelete = conn.prepareStatement(deleteSql)) {
				stmtDelete.setLong(1, attachmentId);
				stmtDelete.executeUpdate();
			}

			apagarArquivoDoDisco(filePath);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void apagarArquivoDoDisco(String filePath) {
		try {
			Path path = Paths.get(filePath);
			boolean deletado = Files.deleteIfExists(path);
			if (deletado) {
				System.out.println("Arquivo apagado do disco com sucesso: " + filePath);
			} else {
				System.out.println("Arquivo não encontrado no disco: " + filePath);
			}
		} catch (IOException e) {
			System.err.println("Erro ao deletar o arquivo físico: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static List<Map<String, Object>> buscarAnexosPorCheckoutId(Long checkoutId) {
		List<Map<String, Object>> lista = new ArrayList<>();
		String sql = "SELECT id, file_name, file_path FROM checkout_attachments WHERE checkout_id = ?";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, checkoutId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> map = new HashMap<>();
					map.put("id", rs.getLong("id"));
					map.put("fileName", rs.getString("file_name"));
					map.put("filePath", rs.getString("file_path"));
					lista.add(map);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return lista;
	}

}