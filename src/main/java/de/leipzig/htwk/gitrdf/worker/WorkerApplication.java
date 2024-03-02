package de.leipzig.htwk.gitrdf.worker;

import org.apache.jena.shared.impl.JenaParameters;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"de.leipzig.htwk.gitrdf.worker", "de.leipzig.htwk.gitrdf.database.common"})
@EntityScan(basePackages = "de.leipzig.htwk.gitrdf.database.common.entity")
@EnableJpaRepositories(basePackages = "de.leipzig.htwk.gitrdf.database.common.repository")
@EnableScheduling
public class WorkerApplication {

	public static void main(String[] args) {

		JenaParameters.enableEagerLiteralValidation = true;
		JenaParameters.enableSilentAcceptanceOfUnknownDatatypes = false;

		SpringApplication.run(WorkerApplication.class, args);
	}

}
