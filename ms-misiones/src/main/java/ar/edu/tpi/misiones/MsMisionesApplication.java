package ar.edu.tpi.misiones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsMisionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsMisionesApplication.class, args);
    }
}
