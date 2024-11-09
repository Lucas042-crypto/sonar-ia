package br.com.younkoudev.SonarIA;

import br.com.younkoudev.SonarIA.service.SonarService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SonarIaApplication implements CommandLineRunner {

	private final SonarService sonarService;

    public SonarIaApplication(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    public static void main(String[] args) {
		SpringApplication.run(SonarIaApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		sonarService.getRelatorioSonar();
	}
}
