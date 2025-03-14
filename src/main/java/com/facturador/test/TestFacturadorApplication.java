package com.facturador.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@ComponentScan("io.github.project.openubl.spring.xsender.runtime")
@SpringBootApplication
public class TestFacturadorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestFacturadorApplication.class, args);
	}

}
