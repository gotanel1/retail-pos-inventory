package com.got.retailpos.reporting;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.domain.Product;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.security.RetailUserPrincipal;
import com.got.retailpos.inventory.application.InventoryService;
import com.got.retailpos.inventory.application.SupplierService;
import com.got.retailpos.sales.application.SalesService;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportingIntegrationTests {

	@Autowired MockMvc mockMvc;
	@Autowired ProductCatalogService catalog;
	@Autowired UserAccountService users;
	@Autowired SupplierService suppliers;
	@Autowired InventoryService inventory;
	@Autowired SalesService sales;
	@Autowired EntityManager entityManager;

	Product product;
	RetailUserPrincipal manager;
	RetailUserPrincipal cashier;

	@BeforeEach
	void setup() {
		var category = catalog.createCategory("report-" + UUID.randomUUID());
		product = catalog.createProduct(new ProductCatalogService.ProductInput(
				category.getId(), "REPORT-" + UUID.randomUUID(), null, "สินค้ารายงาน", new BigDecimal("107.00"), 1));
		manager = principal("manager", Role.MANAGER);
		cashier = principal("cashier", Role.CASHIER);
		var supplier = suppliers.create(new SupplierService.SupplierInput("supplier-" + UUID.randomUUID(), null, null));
		inventory.postReceipt(new InventoryService.ReceiptInput(
				supplier.getId(), "GR-" + UUID.randomUUID(), Instant.now(), null,
				List.of(new InventoryService.ReceiptItemInput(product.getId(), 2, new BigDecimal("60.0000")))), manager.id());
		var sale = sales.create(new SalesService.CreateSaleInput(
				null, List.of(new SalesService.SaleItemInput(product.getId(), 1))), cashier.id());
		sales.checkoutCash(sale.getId(), "report-" + UUID.randomUUID(), new BigDecimal("107.00"), cashier.id());
		entityManager.flush();
	}

	@Test
	void shouldCalculateDashboardFromImmutableSnapshots() throws Exception {
		var today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
		mockMvc.perform(get("/api/v1/reports/dashboard")
				.param("from", today.toString()).param("to", today.toString()).with(user(manager)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.salesCount", is(1)))
				.andExpect(jsonPath("$.summary.totalSales", is(107.0)))
				.andExpect(jsonPath("$.summary.netSalesExcludingVat", is(100.0)))
				.andExpect(jsonPath("$.summary.costOfGoodsSold", is(60.0)))
				.andExpect(jsonPath("$.summary.grossProfit", is(40.0)))
				.andExpect(jsonPath("$.summary.inventoryValue", is(60.0)))
				.andExpect(jsonPath("$.payments[0].method", is("CASH")))
				.andExpect(jsonPath("$.lowStock[*].sku", hasItem(product.getSku())))
				.andExpect(jsonPath("$.movements[0].movementType", is("SALE")));
	}

	@Test
	void shouldRestrictReportsAndValidateDateRange() throws Exception {
		mockMvc.perform(get("/api/v1/reports/dashboard").with(user(cashier)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/reports/dashboard")
				.param("from", "2026-07-16").param("to", "2026-07-15").with(user(manager)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title", is("ข้อมูลไม่ถูกต้อง")));
	}

	private RetailUserPrincipal principal(String prefix, Role role) {
		var username = prefix + "-" + UUID.randomUUID();
		return RetailUserPrincipal.from(users.create(username, "correct-password", username, role));
	}
}
