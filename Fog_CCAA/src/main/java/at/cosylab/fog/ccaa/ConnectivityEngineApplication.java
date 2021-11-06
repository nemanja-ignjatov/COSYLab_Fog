package at.cosylab.fog.ccaa;

import at.cosylab.fog.ccaa.utils.ConnectivityEngineGlobals;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConnectivityEngineApplication implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		while(ConnectivityEngineGlobals.isShouldRun()) {
			Thread.sleep(2000);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(ConnectivityEngineApplication.class, args);
	}

}
