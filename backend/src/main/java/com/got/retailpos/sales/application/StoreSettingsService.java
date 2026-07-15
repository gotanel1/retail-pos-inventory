package com.got.retailpos.sales.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.got.retailpos.sales.domain.StoreSettings;
import com.got.retailpos.sales.infrastructure.StoreSettingsRepository;

@Service
public class StoreSettingsService {
	private final StoreSettingsRepository repository;
	public StoreSettingsService(StoreSettingsRepository repository) { this.repository = repository; }
	@Transactional(readOnly = true)
	public StoreSettings get() { return repository.findById((short) 1).orElseThrow(); }
	@Transactional
	public StoreSettings update(SettingsInput input) {
		if (!StringUtils.hasText(input.storeName())) throw new IllegalArgumentException("ชื่อร้านห้ามว่าง");
		if (input.vatRate() == null || input.vatRate().signum() < 0 || input.vatRate().compareTo(new BigDecimal("100")) > 0 || input.vatRate().scale() > 2) throw new IllegalArgumentException("อัตรา VAT ต้องอยู่ระหว่าง 0-100 และมีทศนิยมไม่เกิน 2 ตำแหน่ง");
		var settings = get(); settings.update(input.storeName().strip(), input.vatEnabled(), input.vatRate().setScale(2, RoundingMode.UNNECESSARY), StringUtils.hasText(input.receiptFooter()) ? input.receiptFooter().strip() : null); return settings;
	}
	public record SettingsInput(String storeName, boolean vatEnabled, BigDecimal vatRate, String receiptFooter) {}
}
