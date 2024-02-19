package de.leipzig.htwk.gitrdf.worker;

import org.apache.jena.shared.impl.JenaParameters;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkerApplication {

	public static void main(String[] args) {

		JenaParameters.enableEagerLiteralValidation = true;
		JenaParameters.enableSilentAcceptanceOfUnknownDatatypes = false;

		SpringApplication.run(WorkerApplication.class, args);
	}

}
