package io.triagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TriagentApplication {

	public static void main(String[] args) {
		SpringApplication.run(TriagentApplication.class, args);
	}

}
