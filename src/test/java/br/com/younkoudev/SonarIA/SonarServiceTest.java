package br.com.younkoudev.SonarIA;

import br.com.younkoudev.SonarIA.service.SonarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SonarServiceTest {

    @Autowired
    private SonarService sonarService;

    @Test
    public void getRelatorioSonar(){
        sonarService.getRelatorioSonar();
    }
}
