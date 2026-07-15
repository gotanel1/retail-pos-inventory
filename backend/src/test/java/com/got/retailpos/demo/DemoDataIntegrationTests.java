package com.got.retailpos.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.customers.infrastructure.CustomerRepository;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.inventory.infrastructure.GoodsReceiptRepository;
import com.got.retailpos.sales.domain.SaleStatus;
import com.got.retailpos.sales.infrastructure.SaleRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.bootstrap.owner.password=owner-password-for-tests",
		"app.demo.enabled=true",
		"app.demo.password=demo-password-for-tests"
})
class DemoDataIntegrationTests {

	@Autowired UserAccountRepository users;
	@Autowired ProductRepository products;
	@Autowired CustomerRepository customers;
	@Autowired GoodsReceiptRepository receipts;
	@Autowired SaleRepository sales;

	@Test
	void shouldSeedOnlyFakePortfolioData() {
		assertThat(users.findByUsernameIgnoreCase("demo-manager")).isPresent();
		assertThat(users.findByUsernameIgnoreCase("demo-cashier")).isPresent();
		assertThat(users.findByUsernameIgnoreCase("demo-stock")).isPresent();
		assertThat(products.findBySkuIgnoreCase("DEMO-HAMMER")).isPresent();
		assertThat(customers.count()).isEqualTo(2);
		assertThat(receipts.existsByReferenceNumber("DEMO-GR-001")).isTrue();
		assertThat(sales.findAll()).filteredOn(sale -> sale.getStatus() == SaleStatus.COMPLETED).hasSize(3);
	}
}
