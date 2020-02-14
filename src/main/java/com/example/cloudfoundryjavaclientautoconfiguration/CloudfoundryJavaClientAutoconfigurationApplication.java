package com.example.cloudfoundryjavaclientautoconfiguration;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.cloudfoundry.CloudFoundryProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class CloudfoundryJavaClientAutoconfigurationApplication {

	@SneakyThrows
	public static void main(String[] args) {
		SpringApplication.run(CloudfoundryJavaClientAutoconfigurationApplication.class, args);
		System.in.read();
	}

}


@Component
@Log4j2
class Restager {

	private final String buildpack;
	private final CloudFoundryOperations cloudFoundryOperations;
	private final CloudFoundryClient client;

	Restager(CloudFoundryOperations cf, CloudFoundryClient client, @Value("${restager.buildpack:python}") String buildpack) {
		this.buildpack = buildpack;
		this.cloudFoundryOperations = cf;
		this.client = client;
		log.info("going to restage all applications with the " + this.buildpack + " buildpack.");
	}

	@SneakyThrows
	@EventListener(ApplicationReadyEvent.class)
	public void restage() {
		this.cloudFoundryOperations
			.applications()
			.list()
			.filter(applicationSummary -> applicationSummary.getRunningInstances() > 0)
			.flatMap(applicationSummary -> client.applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(applicationSummary.getId()).build()))
			.filter(summaryApplicationResponse -> buildpack(summaryApplicationResponse.getBuildpack(), summaryApplicationResponse.getDetectedBuildpack()).contains(this.buildpack))
			.doOnNext(summaryApplicationResponse -> log.info("going to restage " + summaryApplicationResponse.getName()))
			.flatMap(summaryApplicationResponse -> this.cloudFoundryOperations.applications().restage(RestageApplicationRequest.builder().name(summaryApplicationResponse.getName()).build()))
			.doFinally(signalType -> log.info(signalType.toString()))
			.subscribe()
		;
	}

	private String buildpack(String bp, String dbp) {
		return StringUtils.hasText(bp) ? bp : StringUtils.hasText(dbp) ? dbp : "";
	}
}