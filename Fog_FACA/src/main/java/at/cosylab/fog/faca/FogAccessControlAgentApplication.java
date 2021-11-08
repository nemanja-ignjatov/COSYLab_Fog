package at.cosylab.fog.faca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class FogAccessControlAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(FogAccessControlAgentApplication.class, args);
	}
}
