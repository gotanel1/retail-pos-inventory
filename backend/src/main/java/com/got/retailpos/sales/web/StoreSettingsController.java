package com.got.retailpos.sales.web;

import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.got.retailpos.sales.application.StoreSettingsService;
import com.got.retailpos.sales.domain.StoreSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/v1/store-settings")
public class StoreSettingsController {
	private final StoreSettingsService service;
	public StoreSettingsController(StoreSettingsService service) { this.service = service; }
	@GetMapping public SettingsResponse get() { return SettingsResponse.from(service.get()); }
	@PutMapping @PreAuthorize("hasRole('OWNER')") public SettingsResponse update(@Valid @RequestBody SettingsRequest body) { return SettingsResponse.from(service.update(body.toInput())); }
	public record SettingsRequest(@NotBlank @Size(max=180) String storeName, boolean vatEnabled, @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal vatRate, @Size(max=500) String receiptFooter) { StoreSettingsService.SettingsInput toInput() { return new StoreSettingsService.SettingsInput(storeName, vatEnabled, vatRate, receiptFooter); } }
	public record SettingsResponse(String storeName, boolean vatEnabled, BigDecimal vatRate, String receiptFooter) { static SettingsResponse from(StoreSettings value) { return new SettingsResponse(value.getStoreName(), value.isVatEnabled(), value.getVatRate(), value.getReceiptFooter()); } }
}
