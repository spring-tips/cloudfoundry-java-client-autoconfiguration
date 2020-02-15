package com.example.cfac;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class CfacApplication {

	@SneakyThrows
	public static void main(String[] args) {
		SpringApplication.run(CfacApplication.class, args);
		System.in.read();
	}
}

@Component
@Log4j2
class Restager {

	private final String buildpack = "python";

	private boolean isValidBuildpack(String one, String two) {
		return (StringUtils.hasText(one) ? one : StringUtils.hasText(two) ? two : "").contains(this.buildpack);
	}

	Restager(CloudFoundryOperations ops, CloudFoundryClient client) {
		ops
			.applications()
			.list()
			.filter(as -> as.getRunningInstances() > 0)
			.flatMap(as -> client.applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(as.getId()).build()))
			.filter(as -> isValidBuildpack(as.getBuildpack(), as.getDetectedBuildpack()))
			.doOnNext(as -> log.info("restaging " + as.getName() + '.'))
			.flatMap(as -> ops.applications().restage(RestageApplicationRequest.builder().name(as.getName()).build()))
			.subscribe();
	}
}