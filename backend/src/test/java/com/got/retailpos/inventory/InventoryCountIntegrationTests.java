package com.got.retailpos.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.domain.Product;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.security.RetailUserPrincipal;
import com.got.retailpos.inventory.application.InventoryConflictException;
import com.got.retailpos.inventory.application.InventoryCountService;
import com.got.retailpos.inventory.application.InventoryService;
import com.got.retailpos.inventory.application.SupplierService;
import com.got.retailpos.inventory.domain.MovementType;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository;
import com.got.retailpos.inventory.infrastructure.StockMovementRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryCountIntegrationTests {

	@Autowired MockMvc mockMvc;
	@Autowired ProductCatalogService catalogService;
	@Autowired UserAccountService userService;
	@Autowired InventoryCountService countService;
	@Autowired InventoryService inventoryService;
	@Autowired SupplierService supplierService;
	@Autowired InventoryBalanceRepository balanceRepository;
	@Autowired StockMovementRepository movementRepository;

	private Product product;

	@BeforeEach
	void setUp() {
		var category = catalogService.createCategory("count-" + UUID.randomUUID());
		product = catalogService.createProduct(new ProductCatalogService.ProductInput(category.getId(), "COUNT-" + UUID.randomUUID(), null,
				"สินค้าตรวจนับ", new BigDecimal("10.00"), 0));
	}

	@Test
	void shouldLetStaffSubmitAndManagerApproveAdjustment() throws Exception {
		var staff = principal("staff", Role.INVENTORY_STAFF);
		var manager = principal("manager", Role.MANAGER);

		var result = mockMvc.perform(post("/api/v1/inventory/counts").with(user(staff)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(countBody(5)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", is("SUBMITTED")))
				.andExpect(jsonPath("$.items[0].expectedOnHand", is(0)))
				.andExpect(jsonPath("$.items[0].difference", is(5)))
				.andReturn();
		var id = com.jayway.jsonpath.JsonPath.<String>read(result.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/inventory/counts/{id}/approve", id).with(user(manager)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("APPROVED")))
				.andExpect(jsonPath("$.approvedBy", is(manager.id().toString())));

		assertThat(balanceRepository.findById(product.getId()).orElseThrow().getOnHand()).isEqualTo(5);
		var movement = movementRepository.findAll().getFirst();
		assertThat(movement.getMovementType()).isEqualTo(MovementType.ADJUSTMENT_IN);
		assertThat(movement.getQuantityDelta()).isEqualTo(5);
		assertThat(movement.getReason()).isEqualTo("ตรวจนับประจำเดือน");
		assertThat(movement.getActorUserId()).isEqualTo(manager.id());
	}

	@Test
	void shouldNotLetInventoryStaffApprove() throws Exception {
		var staff = principal("staff-no-approve", Role.INVENTORY_STAFF);
		var count = countService.submit(input(1), staff.id());

		mockMvc.perform(post("/api/v1/inventory/counts/{id}/approve", count.getId()).with(user(staff)).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldRejectStaleCountAfterStockMovement() {
		var staff = principal("staff-stale", Role.INVENTORY_STAFF);
		var manager = principal("manager-stale", Role.MANAGER);
		var count = countService.submit(input(5), staff.id());
		var supplier = supplierService.create(new SupplierService.SupplierInput("supplier-" + UUID.randomUUID(), null, null));
		inventoryService.postReceipt(new InventoryService.ReceiptInput(supplier.getId(), "GR-" + UUID.randomUUID(), Instant.now().minusSeconds(1), null,
				List.of(new InventoryService.ReceiptItemInput(product.getId(), 1, new BigDecimal("2.0000")))), manager.id());

		assertThatThrownBy(() -> countService.approve(count.getId(), manager.id()))
				.isInstanceOf(InventoryConflictException.class)
				.hasMessageContaining("กรุณาตรวจนับใหม่");
		assertThat(balanceRepository.findById(product.getId()).orElseThrow().getOnHand()).isEqualTo(1);
	}

	@Test
	void shouldRejectSecondApproval() {
		var manager = principal("manager-twice", Role.MANAGER);
		var count = countService.submit(input(2), manager.id());
		countService.approve(count.getId(), manager.id());

		assertThatThrownBy(() -> countService.approve(count.getId(), manager.id()))
				.isInstanceOf(InventoryConflictException.class);
	}

	private InventoryCountService.CountInput input(int counted) {
		return new InventoryCountService.CountInput("ตรวจนับประจำเดือน", Instant.now().minusSeconds(1),
				List.of(new InventoryCountService.CountItemInput(product.getId(), counted)));
	}

	private String countBody(int counted) {
		return """
				{"countedAt":"%s","reason":"ตรวจนับประจำเดือน","items":[{"productId":"%s","countedQuantity":%d}]}
				""".formatted(Instant.now().minusSeconds(1), product.getId(), counted);
	}

	private RetailUserPrincipal principal(String prefix, Role role) {
		var username = prefix + "-" + UUID.randomUUID();
		return RetailUserPrincipal.from(userService.create(username, "correct-password", username, role));
	}
}
