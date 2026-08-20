package lucas.hjds;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOriginPatterns("*") // Libera qualquer origem, IP ou subdomínio HTTP/HTTPS
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD").allowedHeaders("*")
				.exposedHeaders("*").allowCredentials(true).maxAge(3600);
	}
}