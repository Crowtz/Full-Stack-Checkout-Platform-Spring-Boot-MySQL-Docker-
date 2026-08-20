package lucas.hjds.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PaymentDAO {

	public boolean updateStatus(PaymentData data) {

		String sql = """
				UPDATE webhook_pagamentos
				SET
				    payment_id = ?,
				    status = ?,
				    status_detail = ?,
				    payment_method = ?,
				    approved_at = ?,
				    updated_at = CURRENT_TIMESTAMP
				WHERE order_id = ?
				""";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, data.getPaymentId());
			stmt.setString(2, data.getStatus());
			stmt.setString(3, data.getStatusDetail());
			stmt.setString(4, data.getPaymentMethod());
			stmt.setTimestamp(5, data.getApprovedAt());
			stmt.setString(6, data.getOrderId());

			int rows = stmt.executeUpdate();

			if (rows > 0) {
				System.out.println("Order " + data.getOrderId() + " atualizada pelo webhook!");
				return true;
			}

			System.err.println("Order " + data.getOrderId() + " não encontrada!");

			return false;

		} catch (SQLException e) {
			System.err.println("Erro no UPDATE: " + e.getMessage());
			return false;
		}
	}

	public void createPendingOrder(PaymentData data) {

		String sql = """
				INSERT INTO webhook_pagamentos (
				    order_id,
				    checkout_id,
				    payment_id,
				    status,
				    status_detail,
				    total_amount,
				    payer_email,
				    payer_phone,
				    payer_first_name,
				    payment_method,
				    created_at,
				    approved_at
				)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

				ON DUPLICATE KEY UPDATE
				    payment_id = VALUES(payment_id),
				    checkout_id = VALUES(checkout_id),
				    status = VALUES(status),
				    status_detail = VALUES(status_detail),
				    total_amount = VALUES(total_amount),
				    payer_email = VALUES(payer_email),
				    payer_phone = VALUES(payer_phone),
				    payer_first_name = VALUES(payer_first_name),
				    payment_method = VALUES(payment_method),
				    created_at = VALUES(created_at),
				    approved_at = VALUES(approved_at)
				""";

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, data.getOrderId());
			if (data.getCheckoutId() != 0) {
				stmt.setLong(2, data.getCheckoutId());
			} else {
				stmt.setNull(2, java.sql.Types.BIGINT);
			}
			stmt.setString(3, data.getPaymentId());
			stmt.setString(4, data.getStatus());
			stmt.setString(5, data.getStatusDetail());
			stmt.setBigDecimal(6, data.getTotalAmount());
			stmt.setString(7, data.getPayerEmail());
			stmt.setString(8, data.getPayerPhone());
			stmt.setString(9, data.getPayerFirstName());
			stmt.setString(10, data.getPaymentMethod());
			stmt.setTimestamp(11, data.getCreatedAt());
			stmt.setTimestamp(12, data.getApprovedAt());

			stmt.executeUpdate();

			System.out.println("Order " + data.getOrderId() + " salva com sucesso!");

		} catch (SQLException e) {
			System.err.println("Erro ao salvar order: " + e.getMessage());
		}
	}

	public Map<String, Object> getOrder(String orderId) {
		String sql = "SELECT * FROM webhook_pagamentos WHERE order_id = ?";
		Map<String, Object> orderData = new HashMap<>();

		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, orderId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					orderData.put("order_id", rs.getString("order_id"));
					orderData.put("checkout_id", rs.getObject("checkout_id"));
					orderData.put("payment_id", rs.getString("payment_id"));
					orderData.put("status", rs.getString("status"));
					orderData.put("status_detail", rs.getString("status_detail"));
					orderData.put("total_amount", rs.getBigDecimal("total_amount"));
					orderData.put("payer_email", rs.getString("payer_email"));
					orderData.put("payer_phone", rs.getString("payer_phone"));
					orderData.put("payer_first_name", rs.getString("payer_first_name"));
					orderData.put("payment_method", rs.getString("payment_method"));
					orderData.put("created_at", rs.getTimestamp("created_at"));
					orderData.put("approved_at", rs.getTimestamp("approved_at"));
					orderData.put("updated_at", rs.getTimestamp("updated_at"));
				}
			}
		} catch (SQLException e) {
			System.err.println("Erro ao buscar order: " + e.getMessage());
		}

		return orderData;
	}

	public boolean isApproved(String orderId) {
		Map<String, Object> data = getOrder(orderId);

		if (!data.containsKey("status") || data.get("status") == null) {
			return false;
		}

		String status = ((String) data.get("status")).toLowerCase();
		String statusDetail = data.get("status_detail") != null ? ((String) data.get("status_detail")).toLowerCase()
				: "";

		boolean isProcessed = "processed".equals(status);
		boolean isDetailOk = statusDetail.isBlank() || "accredited".equals(statusDetail);

		return isProcessed && isDetailOk;
	}

	public boolean removeOrder(String orderId) {
		String sql = "DELETE FROM tb_webhook_pagamentos WHERE order_id = ?";
		try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, orderId);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			System.err.println("Erro ao deletar order: " + e.getMessage());
			return false;
		}
	}
}