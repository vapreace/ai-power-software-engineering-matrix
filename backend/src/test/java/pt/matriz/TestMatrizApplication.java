package pt.matriz;

import org.springframework.boot.SpringApplication;

public class TestMatrizApplication {

	public static void main(String[] args) {
		SpringApplication.from(MatrizApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
