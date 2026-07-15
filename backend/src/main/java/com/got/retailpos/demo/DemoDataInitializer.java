package com.got.retailpos.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.domain.Product;
import com.got.retailpos.catalog.infrastructure.CategoryRepository;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.customers.application.CustomerService;
import com.got.retailpos.customers.domain.Customer;
import com.got.retailpos.customers.infrastructure.CustomerRepository;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.domain.UserAccount;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.inventory.application.InventoryService;
import com.got.retailpos.inventory.application.SupplierService;
import com.got.retailpos.inventory.domain.Supplier;
import com.got.retailpos.inventory.infrastructure.GoodsReceiptRepository;
import com.got.retailpos.inventory.infrastructure.SupplierRepository;
import com.got.retailpos.sales.application.SalesService;
import com.got.retailpos.sales.infrastructure.SaleRepository;

@Component
@Order(20)
@EnableConfigurationProperties(DemoDataProperties.class)
public class DemoDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
	private static final String CATEGORY_NAME = "Demo Tools";
	private static final String SUPPLIER_NAME = "Demo Supplier";

	private final DemoDataProperties properties;
	private final UserAccountService users;
	private final UserAccountRepository userRepository;
	private final ProductCatalogService catalog;
	private final CategoryRepository categories;
	private final ProductRepository products;
	private final SupplierService supplierService;
	private final SupplierRepository suppliers;
	private final GoodsReceiptRepository receipts;
	private final InventoryService inventory;
	private final CustomerService customerService;
	private final CustomerRepository customers;
	private final SalesService sales;
	private final SaleRepository saleRepository;

	public DemoDataInitializer(DemoDataProperties properties, UserAccountService users,
			UserAccountRepository userRepository, ProductCatalogService catalog, CategoryRepository categories,
			ProductRepository products, SupplierService supplierService, SupplierRepository suppliers,
			GoodsReceiptRepository receipts, InventoryService inventory, CustomerService customerService,
			CustomerRepository customers, SalesService sales, SaleRepository saleRepository) {
		this.properties = properties;
		this.users = users;
		this.userRepository = userRepository;
		this.catalog = catalog;
		this.categories = categories;
		this.products = products;
		this.supplierService = supplierService;
		this.suppliers = suppliers;
		this.receipts = receipts;
		this.inventory = inventory;
		this.customerService = customerService;
		this.customers = customers;
		this.sales = sales;
		this.saleRepository = saleRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.enabled()) return;
		if (!StringUtils.hasText(properties.password()) || properties.password().length() < 12) {
			throw new IllegalStateException("APP_DEMO_PASSWORD ต้องยาวอย่างน้อย 12 ตัวอักษรเมื่อเปิด demo data");
		}

		var manager = user("demo-manager", "ผู้จัดการร้านตัวอย่าง", Role.MANAGER);
		var cashier = user("demo-cashier", "แคชเชียร์ร้านตัวอย่าง", Role.CASHIER);
		user("demo-stock", "พนักงานสต็อกร้านตัวอย่าง", Role.INVENTORY_STAFF);
		var category = categories.findByNormalizedName(ProductCatalogService.normalizeCategoryName(CATEGORY_NAME))
				.orElseGet(() -> catalog.createCategory(CATEGORY_NAME));
		var hammer = product(category.getId(), "DEMO-HAMMER", "8850000000001", "ค้อนหงอนตัวอย่าง", "199.00", 3);
		var gloves = product(category.getId(), "DEMO-GLOVES", "8850000000002", "ถุงมือช่างตัวอย่าง", "79.00", 5);
		var tape = product(category.getId(), "DEMO-TAPE", "8850000000003", "เทปพันสายไฟตัวอย่าง", "49.00", 4);
		var supplier = suppliers.findByNormalizedName(SUPPLIER_NAME.toLowerCase(Locale.ROOT))
				.orElseGet(() -> supplierService.create(new SupplierService.SupplierInput(SUPPLIER_NAME, "020000000", "ข้อมูลจำลองเท่านั้น")));
		seedReceipt(supplier, manager, hammer, gloves, tape);
		var customerA = customer("0800000001", "ลูกค้าตัวอย่าง A");
		var customerB = customer("0800000002", "ลูกค้าตัวอย่าง B");
		seedSale("demo-sale-01", customerA, cashier, List.of(new SalesService.SaleItemInput(hammer.getId(), 1)));
		seedSale("demo-sale-02", customerB, cashier, List.of(new SalesService.SaleItemInput(gloves.getId(), 2)));
		seedSale("demo-sale-03", null, cashier, List.of(new SalesService.SaleItemInput(tape.getId(), 2)));
		log.info("เตรียม demo data แบบไม่ใช้ข้อมูลลูกค้าจริงเรียบร้อยแล้ว");
	}

	private UserAccount user(String username, String displayName, Role role) {
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseGet(() -> users.create(username, properties.password(), displayName, role));
	}

	private Product product(UUID categoryId, String sku, String barcode, String name, String price, int threshold) {
		return products.findBySkuIgnoreCase(sku).orElseGet(() -> catalog.createProduct(
				new ProductCatalogService.ProductInput(categoryId, sku, barcode, name, new BigDecimal(price), threshold)));
	}

	private Customer customer(String normalizedPhone, String name) {
		return customers.findByNormalizedPhone(normalizedPhone).orElseGet(() -> customerService.create(
				new CustomerService.CustomerInput(name, normalizedPhone, "ข้อมูลจำลองสำหรับ public demo", false)));
	}

	private void seedReceipt(Supplier supplier, UserAccount manager, Product hammer, Product gloves, Product tape) {
		if (receipts.existsByReferenceNumber("DEMO-GR-001")) return;
		inventory.postReceipt(new InventoryService.ReceiptInput(supplier.getId(), "DEMO-GR-001",
				Instant.now().minusSeconds(60), "ข้อมูลจำลองสำหรับ public demo", List.of(
						new InventoryService.ReceiptItemInput(hammer.getId(), 12, new BigDecimal("120.0000")),
						new InventoryService.ReceiptItemInput(gloves.getId(), 20, new BigDecimal("35.0000")),
						new InventoryService.ReceiptItemInput(tape.getId(), 8, new BigDecimal("18.0000")))), manager.getId());
	}

	private void seedSale(String key, Customer customer, UserAccount cashier, List<SalesService.SaleItemInput> items) {
		if (saleRepository.findByCheckoutIdempotencyKey(key).isPresent()) return;
		var sale = sales.create(new SalesService.CreateSaleInput(customer == null ? null : customer.getId(), items), cashier.getId());
		sales.checkoutCash(sale.getId(), key, sale.getTotal(), cashier.getId());
	}
}
