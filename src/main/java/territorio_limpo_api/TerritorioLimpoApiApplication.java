package territorio_limpo_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TerritorioLimpoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerritorioLimpoApiApplication.class, args);
	}

}
