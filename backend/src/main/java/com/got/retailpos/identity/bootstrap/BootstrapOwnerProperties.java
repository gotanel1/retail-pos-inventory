package com.got.retailpos.identity.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.owner")
public record BootstrapOwnerProperties(String username, String password, String displayName) {
}
