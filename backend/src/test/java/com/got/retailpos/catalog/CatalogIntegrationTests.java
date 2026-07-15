package com.got.retailpos.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import com.got.retailpos.catalog.infrastructure.CategoryRepository;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.identity.security.RetailUserPrincipal;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductCatalogService catalogService;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	UserAccountService userAccountService;

	@Autowired
	UserAccountRepository userRepository;

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldLetInventoryStaffCreateCategoryAndProduct() throws Exception {
		var staff = principal("stock", Role.INVENTORY_STAFF);
		var categoryResult = mockMvc.perform(post("/api/v1/categories")
				.with(user(staff))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\" เครื่องมือช่าง \"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name", is("เครื่องมือช่าง")))
				.andReturn();
		var categoryId = JsonTestSupport.read(categoryResult, "$.id");

		mockMvc.perform(post("/api/v1/products")
				.with(user(staff))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(productBody(categoryId, "abc-01", "885000000001", "ค้อนหงอน", "199.50", 3)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sku", is("ABC-01")))
				.andExpect(jsonPath("$.salePrice", is(199.5)))
				.andExpect(jsonPath("$.lowStockThreshold", is(3)));
	}

	@Test
	void shouldAllowCashierToReadButNotCreateProduct() throws Exception {
		var cashier = principal("cashier", Role.CASHIER);
		var category = catalogService.createCategory("ทั่วไป");

		mockMvc.perform(get("/api/v1/products").with(user(cashier)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/products")
				.with(user(cashier))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(productBody(category.getId().toString(), "SKU-01", "", "สินค้า", "10.00", 0)))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldRejectPriceWithMoreThanTwoDecimalPlaces() throws Exception {
		var manager = principal("manager", Role.MANAGER);
		var category = catalogService.createCategory("ทั่วไป");

		mockMvc.perform(post("/api/v1/products")
				.with(user(manager))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(productBody(category.getId().toString(), "SKU-01", "", "สินค้า", "10.999", 0)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.salePrice").exists());
	}

	@Test
	void shouldReturnConflictForDuplicateSkuIgnoringCase() throws Exception {
		var owner = principal("owner", Role.OWNER);
		var category = catalogService.createCategory("ทั่วไป");
		catalogService.createProduct(new ProductCatalogService.ProductInput(
				category.getId(), "SKU-01", null, "สินค้าเดิม", new BigDecimal("10.00"), 0));

		mockMvc.perform(post("/api/v1/products")
				.with(user(owner))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(productBody(category.getId().toString(), "sku-01", "", "สินค้าใหม่", "15.00", 0)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", is("SKU ถูกใช้แล้ว: SKU-01")));
	}

	@Test
	void shouldSearchAndPaginateProducts() throws Exception {
		var owner = principal("owner", Role.OWNER);
		var category = catalogService.createCategory("ทั่วไป");
		catalogService.createProduct(input(category.getId().toString(), "HAMMER-01", "ค้อน"));
		catalogService.createProduct(input(category.getId().toString(), "SAW-01", "เลื่อย"));

		mockMvc.perform(get("/api/v1/products")
				.with(user(owner))
				.param("search", "hammer")
				.param("page", "0")
				.param("size", "1")
				.param("sort", "sku,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].sku", is("HAMMER-01")))
				.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	void shouldUpdateProductWithoutChangingItsIdentity() throws Exception {
		var manager = principal("manager", Role.MANAGER);
		var oldCategory = catalogService.createCategory("เดิม");
		var newCategory = catalogService.createCategory("ใหม่");
		var product = catalogService.createProduct(input(oldCategory.getId().toString(), "SKU-01", "สินค้าเดิม"));

		mockMvc.perform(put("/api/v1/products/{id}", product.getId())
				.with(user(manager))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(productBody(newCategory.getId().toString(), "SKU-01", "885000000001", "สินค้าใหม่", "25.00", 4)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(product.getId().toString())))
				.andExpect(jsonPath("$.categoryName", is("ใหม่")))
				.andExpect(jsonPath("$.name", is("สินค้าใหม่")));
	}

	private RetailUserPrincipal principal(String username, Role role) {
		return RetailUserPrincipal.from(
				userAccountService.create(username, "correct-password", username, role));
	}

	private ProductCatalogService.ProductInput input(String categoryId, String sku, String name) {
		return new ProductCatalogService.ProductInput(
				java.util.UUID.fromString(categoryId), sku, null, name, new BigDecimal("10.00"), 0);
	}

	private String productBody(
			String categoryId,
			String sku,
			String barcode,
			String name,
			String salePrice,
			int lowStockThreshold) {
		return """
				{
				  "categoryId": "%s",
				  "sku": "%s",
				  "barcode": "%s",
				  "name": "%s",
				  "salePrice": %s,
				  "lowStockThreshold": %d
				}
				""".formatted(categoryId, sku, barcode, name, salePrice, lowStockThreshold);
	}
}
