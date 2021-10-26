package at.cosylab.fog.fog_trust_provider;

import at.cosylab.fog.fog_trust_provider.utils.FTPGlobals;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FogTrustProviderApplication implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		while(FTPGlobals.isShouldRun()) {
			Thread.sleep(2000);
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(FogTrustProviderApplication.class, args);
	}

}