package com.got.retailpos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class DatasourceConfigurationTests {

	@Test
	void convertsRenderConnectionStringWithoutLeakingCredentialsIntoJdbcUrl() {
		var environment = environmentWith(Map.of(
				"DATABASE_URL", "postgresql://retail:p%40ssword@render-db:5432/retail_pos?sslmode=require"));

		new RenderDatabaseEnvironmentPostProcessor()
				.postProcessEnvironment(environment, new SpringApplication(Object.class));

		assertThat(environment.getRequiredProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://render-db:5432/retail_pos?sslmode=require")
				.doesNotContain("@", "p%40ssword");
	}

	@Test
	void keepsExplicitSpringDatasourceUrlForCompose() {
		var environment = environmentWith(Map.of(
				"DATABASE_URL", "postgresql://render-db:5432/retail_pos",
				"SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/retail_pos"));

		new RenderDatabaseEnvironmentPostProcessor()
				.postProcessEnvironment(environment, new SpringApplication(Object.class));

		assertThat(environment.getPropertySources().contains(RenderDatabaseEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
				.isFalse();
	}

	@Test
	void registersProcessorForSpringBootStartup() throws IOException {
		var factories = PropertiesLoaderUtils.loadProperties(new ClassPathResource("META-INF/spring.factories"));

		assertThat(factories.getProperty(EnvironmentPostProcessor.class.getName()))
				.isEqualTo(RenderDatabaseEnvironmentPostProcessor.class.getName());
	}

	private StandardEnvironment environmentWith(Map<String, Object> deploymentProperties) {
		var environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("deployment", deploymentProperties));
		return environment;
	}
}
