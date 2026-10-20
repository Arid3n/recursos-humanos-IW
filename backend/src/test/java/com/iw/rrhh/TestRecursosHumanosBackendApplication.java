package com.iw.rrhh;

import org.springframework.boot.SpringApplication;

public class TestRecursosHumanosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(RecursosHumanosBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
