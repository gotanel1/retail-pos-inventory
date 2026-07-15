package com.got.retailpos.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public record DemoDataProperties(boolean enabled, String password) {
}
