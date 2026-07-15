package com.got.retailpos.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.got.retailpos.inventory.infrastructure.GoodsReceiptRepository;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository;
import com.got.retailpos.inventory.infrastructure.StockMovementRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductCatalogService catalogService;

	@Autowired
	SupplierService supplierService;

	@Autowired
	InventoryService inventoryService;

	@Autowired
	UserAccountService userAccountService;

	@Autowired
	InventoryBalanceRepository balanceRepository;

	@Autowired
	GoodsReceiptRepository receiptRepository;

	@Autowired
	StockMovementRepository movementRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Product product;
	private UUID supplierId;

	@BeforeEach
	void setUp() {
		var category = catalogService.createCategory("เครื่องมือ-" + UUID.randomUUID());
		product = catalogService.createProduct(new ProductCatalogService.ProductInput(
				category.getId(),
				"SKU-" + UUID.randomUUID(),
				null,
				"ค้อนหงอน",
				new BigDecimal("250.00"),
				3));
		supplierId = supplierService.create(new SupplierService.SupplierInput(
				"Supplier-" + UUID.randomUUID(), "021234567", null)).getId();
	}

	@Test
	void shouldPostReceiptAndAppendMovement() throws Exception {
		var staff = principal("stock", Role.INVENTORY_STAFF);

		mockMvc.perform(post("/api/v1/goods-receipts")
				.with(user(staff))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(receiptBody(supplierId, "GR-001", product.getId(), 10, "100.0000")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", is("POSTED")))
				.andExpect(jsonPath("$.items[0].quantity", is(10)))
				.andExpect(jsonPath("$.totalCost", is(1000.0)));

		mockMvc.perform(get("/api/v1/inventory/balances")
				.with(user(staff))
				.param("search", product.getSku()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].onHand", is(10)))
				.andExpect(jsonPath("$.content[0].reserved", is(0)))
				.andExpect(jsonPath("$.content[0].available", is(10)))
				.andExpect(jsonPath("$.content[0].averageCost", is(100.0)));

		mockMvc.perform(get("/api/v1/inventory/movements")
				.with(user(staff))
				.param("productId", product.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].movementType", is("RECEIVE")))
				.andExpect(jsonPath("$.content[0].quantityDelta", is(10)))
				.andExpect(jsonPath("$.content[0].onHandAfter", is(10)));
	}

	@Test
	void shouldCalculateMovingAverageAcrossReceipts() {
		var actorId = userAccountService.create("manager-a", "correct-password", "Manager", Role.MANAGER).getId();
		inventoryService.postReceipt(input("GR-A", 10, "100.0000"), actorId);
		inventoryService.postReceipt(input("GR-B", 10, "120.0000"), actorId);

		var balance = balanceRepository.findById(product.getId()).orElseThrow();
		assertThat(balance.getOnHand()).isEqualTo(20);
		assertThat(balance.getAvailable()).isEqualTo(20);
		assertThat(balance.getAverageCost()).isEqualByComparingTo("110.0000");
		assertThat(movementRepository.count()).isEqualTo(2);
	}

	@Test
	void shouldRejectCashierPostingReceipt() throws Exception {
		var cashier = principal("cashier", Role.CASHIER);

		mockMvc.perform(post("/api/v1/goods-receipts")
				.with(user(cashier))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(receiptBody(supplierId, "GR-CASHIER", product.getId(), 1, "10.0000")))
				.andExpect(status().isForbidden());

		assertThat(receiptRepository.count()).isZero();
		assertThat(movementRepository.count()).isZero();
	}

	@Test
	void shouldRollbackWholeReceiptWhenAnyProductDoesNotExist() {
		var actorId = userAccountService.create("manager-b", "correct-password", "Manager", Role.MANAGER).getId();
		var input = new InventoryService.ReceiptInput(
				supplierId,
				"GR-INVALID",
				Instant.now().minusSeconds(1),
				null,
				java.util.List.of(
						new InventoryService.ReceiptItemInput(product.getId(), 2, new BigDecimal("10.0000")),
						new InventoryService.ReceiptItemInput(UUID.randomUUID(), 3, new BigDecimal("20.0000"))));

		assertThatThrownBy(() -> inventoryService.postReceipt(input, actorId))
				.hasMessageContaining("สินค้าบางรายการ");
		assertThat(receiptRepository.count()).isZero();
		assertThat(movementRepository.count()).isZero();
		assertThat(balanceRepository.findById(product.getId())).isEmpty();
	}

	@Test
	void shouldRejectDuplicateReferenceNumber() throws Exception {
		var manager = principal("manager-c", Role.MANAGER);
		inventoryService.postReceipt(input("GR-DUPLICATE", 1, "10.0000"), manager.id());

		mockMvc.perform(post("/api/v1/goods-receipts")
				.with(user(manager))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(receiptBody(supplierId, "GR-DUPLICATE", product.getId(), 1, "11.0000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ถูกใช้แล้ว")));
	}

	@Test
	void shouldLetDatabaseRejectMovementUpdateAndDelete() {
		var actorId = userAccountService.create("owner-a", "correct-password", "Owner", Role.OWNER).getId();
		var receipt = inventoryService.postReceipt(input("GR-IMMUTABLE", 1, "10.0000"), actorId);
		var movementId = movementRepository.findAll().getFirst().getId();
		movementRepository.flush();

		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE stock_movements SET reason = 'changed' WHERE id = ?",
				movementId))
				.hasMessageContaining("immutable");
		assertThat(receipt.getId()).isNotNull();
	}

	private RetailUserPrincipal principal(String prefix, Role role) {
		var username = prefix + "-" + UUID.randomUUID();
		return RetailUserPrincipal.from(
				userAccountService.create(username, "correct-password", username, role));
	}

	private InventoryService.ReceiptInput input(String reference, int quantity, String unitCost) {
		return new InventoryService.ReceiptInput(
				supplierId,
				reference,
				Instant.now().minusSeconds(1),
				null,
				java.util.List.of(new InventoryService.ReceiptItemInput(
						product.getId(), quantity, new BigDecimal(unitCost))));
	}

	private String receiptBody(UUID supplier, String reference, UUID productId, int quantity, String unitCost) {
		return """
				{
				  "supplierId": "%s",
				  "referenceNumber": "%s",
				  "receivedAt": "%s",
				  "note": "รับสินค้าตามใบส่งของ",
				  "items": [{
				    "productId": "%s",
				    "quantity": %d,
				    "unitCost": %s
				  }]
				}
				""".formatted(supplier, reference, Instant.now().minusSeconds(1), productId, quantity, unitCost);
	}
}
