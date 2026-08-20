package lucas.hjds.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

	private static HikariDataSource dataSource;

	public static void init(String url, String user, String password) {
		if (dataSource == null) {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(url);
			config.setUsername(user);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(3);
			config.setPoolName("Mercado-Pago");
			dataSource = new HikariDataSource(config);
			criarTabelaSeNaoExiste();
		}
	}

	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	private static void criarTabelaSeNaoExiste() {
		String sql = """
				    CREATE TABLE IF NOT EXISTS webhook_pagamentos (
				        order_id VARCHAR(100) PRIMARY KEY,
				        checkout_id BIGINT,
				        payment_id VARCHAR(100),
				        status VARCHAR(50) NOT NULL,
				        status_detail VARCHAR(100),
				        total_amount DECIMAL(10,2),
				        payer_email VARCHAR(150),
				        payer_phone VARCHAR(30),
				        payer_first_name VARCHAR(20),
				        payment_method VARCHAR(50),
				        created_at DATETIME,
				        approved_at DATETIME,
				        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
				    );
				""";

		String sqlCheckouts = """
				    CREATE TABLE IF NOT EXISTS checkouts (
				        id INT AUTO_INCREMENT PRIMARY KEY,
				        title VARCHAR(150) NOT NULL,
				        description TEXT,
				        original_price DECIMAL(10,2),
				        price DECIMAL(10,2) NOT NULL,
				        discount_percent INT,
				        image_url TEXT,
				        redirect_url TEXT,
				        send_email_enabled TINYINT(1) DEFAULT 1,
				        smtp_host VARCHAR(150),
				        smtp_port INT,
				        smtp_user VARCHAR(150),
				        smtp_pass VARCHAR(255),
				        email_subject VARCHAR(255),
				        email_body_html TEXT,
				        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
				    );
				""";

		String sqlAttachments = """
				    CREATE TABLE IF NOT EXISTS checkout_attachments (
				        id INT AUTO_INCREMENT PRIMARY KEY,
				        checkout_id INT NOT NULL,
				        file_name VARCHAR(255) NOT NULL,
				        file_path VARCHAR(500) NOT NULL,
				        file_type VARCHAR(100),
				        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
				        FOREIGN KEY (checkout_id) REFERENCES checkouts(id) ON DELETE CASCADE
				    );
				""";

		try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
			stmt.execute(sqlCheckouts);
			stmt.execute(sqlAttachments);
			System.out.println(
					"Tabelas 'webhook_pagamentos', 'checkouts' e 'checkout_attachments' verificadas/criadas com sucesso!");
		} catch (SQLException e) {
			System.err.println("Erro ao criar tabelas: " + e.getMessage());
		}
	}
}