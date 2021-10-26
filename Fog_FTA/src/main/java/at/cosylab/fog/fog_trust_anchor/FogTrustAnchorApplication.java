package at.cosylab.fog.fog_trust_anchor;

import at.cosylab.fog.fog_trust_anchor.utils.FTAGlobals;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FogTrustAnchorApplication implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		while(FTAGlobals.isShouldRun()) {
			Thread.sleep(2000);
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(FogTrustAnchorApplication.class, args);
	}

}
