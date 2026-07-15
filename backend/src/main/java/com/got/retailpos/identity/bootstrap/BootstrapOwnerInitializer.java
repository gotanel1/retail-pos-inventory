package com.got.retailpos.identity.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;

@Component
@EnableConfigurationProperties(BootstrapOwnerProperties.class)
public class BootstrapOwnerInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapOwnerInitializer.class);

	private final UserAccountService userAccountService;
	private final BootstrapOwnerProperties properties;

	public BootstrapOwnerInitializer(UserAccountService userAccountService, BootstrapOwnerProperties properties) {
		this.userAccountService = userAccountService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userAccountService.hasAnyUser()) {
			return;
		}

		if (!StringUtils.hasText(properties.password())) {
			log.warn("ยังไม่มีผู้ใช้: กำหนด APP_BOOTSTRAP_OWNER_PASSWORD เพื่อสร้างบัญชี OWNER ครั้งแรก");
			return;
		}

		userAccountService.create(
				properties.username(), properties.password(), properties.displayName(), Role.OWNER);
		log.info("สร้างบัญชี OWNER เริ่มต้นชื่อ {} แล้ว", properties.username());
	}
}
