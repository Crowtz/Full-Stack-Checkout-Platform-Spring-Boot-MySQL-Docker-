package lucas.hjds;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import lucas.hjds.mysql.Database;

@SpringBootApplication
public class MainMP {

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password}")
	private String password;

	public static void main(String[] args) {
		SpringApplication.run(MainMP.class, args);
	}

	@PostConstruct
	public void initDatabase() {
		Database.init(url, username, password);
	}
}