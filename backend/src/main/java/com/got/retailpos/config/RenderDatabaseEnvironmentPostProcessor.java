package com.got.retailpos.config;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public final class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String PROPERTY_SOURCE_NAME = "renderDatabase";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_URL"))) return;
		var databaseUrl = environment.getProperty("DATABASE_URL");
		if (!StringUtils.hasText(databaseUrl)) return;

		environment.getPropertySources().addFirst(new MapPropertySource(
				PROPERTY_SOURCE_NAME, Map.of("spring.datasource.url", toJdbcUrl(databaseUrl))));
	}

	static String toJdbcUrl(String databaseUrl) {
		var uri = URI.create(databaseUrl);
		if (!"postgresql".equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("DATABASE_URL ต้องใช้ scheme postgresql");
		}
		var authority = uri.getRawAuthority();
		var path = uri.getRawPath();
		if (!StringUtils.hasText(authority) || !StringUtils.hasText(path) || "/".equals(path)) {
			throw new IllegalArgumentException("DATABASE_URL ต้องมี host และชื่อฐานข้อมูล");
		}
		var userInfoSeparator = authority.lastIndexOf('@');
		var hostAndPort = userInfoSeparator >= 0 ? authority.substring(userInfoSeparator + 1) : authority;
		if (!StringUtils.hasText(hostAndPort)) {
			throw new IllegalArgumentException("DATABASE_URL ต้องมี host");
		}
		var query = StringUtils.hasText(uri.getRawQuery()) ? "?" + uri.getRawQuery() : "";
		return "jdbc:postgresql://" + hostAndPort + path + query;
	}
}
