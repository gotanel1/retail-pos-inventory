package com.got.retailpos.catalog;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.infrastructure.CategoryRepository;
import com.got.retailpos.catalog.infrastructure.ProductImportRepository;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.identity.security.RetailUserPrincipal;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductImportIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductImportRepository importRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	UserAccountRepository userRepository;

	@Autowired
	UserAccountService userAccountService;

	@Autowired
	ProductCatalogService catalogService;

	@BeforeEach
	void setUp() {
		importRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldPreviewWithoutSavingProductsThenCommitAtomically() throws Exception {
		var owner = principal("owner", Role.OWNER);
		var previewResult = mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile("""
						sku,barcode,name,category,salePrice,lowStockThreshold
						HAMMER-01,885000000001,ค้อนหงอน,เครื่องมือช่าง,199.50,3
						SAW-01,,เลื่อย,เครื่องมือช่าง,120.00,2
						"""))
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.totalRows", is(2)))
				.andExpect(jsonPath("$.validRows", is(2)))
				.andExpect(jsonPath("$.invalidRows", is(0)))
				.andReturn();

		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isZero();
		var importId = UUID.fromString(JsonTestSupport.read(previewResult, "$.importId"));

		mockMvc.perform(post("/api/v1/product-imports/{id}/commit", importId)
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.createdProducts", is(2)));

		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(categoryRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldBlockCommitWhenPreviewContainsDuplicateSku() throws Exception {
		var manager = principal("manager", Role.MANAGER);
		var previewResult = mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile("""
						sku,barcode,name,category,salePrice,lowStockThreshold
						SKU-01,,สินค้าแรก,ทั่วไป,10.00,0
						SKU-01,,สินค้าซ้ำ,ทั่วไป,20.00,0
						"""))
				.with(user(manager))
				.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invalidRows", is(1)))
				.andExpect(jsonPath("$.rows[1].errors", hasItem("SKU ซ้ำภายในไฟล์")))
				.andReturn();
		var importId = UUID.fromString(JsonTestSupport.read(previewResult, "$.importId"));

		mockMvc.perform(post("/api/v1/product-imports/{id}/commit", importId)
				.with(user(manager))
				.with(csrf()))
				.andExpect(status().isConflict());

		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isZero();
	}

	@Test
	void shouldRevalidateAgainstProductsCreatedAfterPreview() throws Exception {
		var staff = principal("stock", Role.INVENTORY_STAFF);
		var previewResult = mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile(validSingleRowCsv()))
				.with(user(staff))
				.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invalidRows", is(0)))
				.andReturn();
		var importId = UUID.fromString(JsonTestSupport.read(previewResult, "$.importId"));

		var category = catalogService.createCategory("ทั่วไป");
		catalogService.createProduct(new ProductCatalogService.ProductInput(
				category.getId(), "SKU-01", null, "สินค้าจากรายการอื่น", new BigDecimal("9.00"), 0));

		mockMvc.perform(post("/api/v1/product-imports/{id}/commit", importId)
				.with(user(staff))
				.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", is("ข้อมูลเปลี่ยนจากตอน preview กรุณาตรวจสอบและอัปโหลดใหม่")));

		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldPreventCommittingTheSamePreviewTwice() throws Exception {
		var owner = principal("owner", Role.OWNER);
		var previewResult = mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile(validSingleRowCsv()))
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isCreated())
				.andReturn();
		var importId = UUID.fromString(JsonTestSupport.read(previewResult, "$.importId"));

		mockMvc.perform(post("/api/v1/product-imports/{id}/commit", importId)
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/product-imports/{id}/commit", importId)
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", is("รายการนำเข้านี้ถูกยืนยันแล้ว")));
	}

	@Test
	void shouldRejectCashierFromPreview() throws Exception {
		var cashier = principal("cashier", Role.CASHIER);

		mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile(validSingleRowCsv()))
				.with(user(cashier))
				.with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldRejectCsvWithMissingHeader() throws Exception {
		var owner = principal("owner", Role.OWNER);

		mockMvc.perform(multipart("/api/v1/product-imports/preview")
				.file(csvFile("sku,name\nSKU-01,สินค้า"))
				.with(user(owner))
				.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title", is("ไฟล์ CSV ไม่ถูกต้อง")));
	}

	private RetailUserPrincipal principal(String username, Role role) {
		return RetailUserPrincipal.from(
				userAccountService.create(username, "correct-password", username, role));
	}

	private MockMultipartFile csvFile(String content) {
		return new MockMultipartFile(
				"file", "products.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
	}

	private String validSingleRowCsv() {
		return """
				sku,barcode,name,category,salePrice,lowStockThreshold
				SKU-01,,สินค้า,ทั่วไป,10.00,0
				""";
	}
}
