package com.got.retailpos.sales;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal; import java.time.Instant; import java.util.*; import java.util.concurrent.*;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.context.annotation.Import;
import com.got.retailpos.TestcontainersConfiguration; import com.got.retailpos.catalog.application.ProductCatalogService; import com.got.retailpos.identity.application.UserAccountService; import com.got.retailpos.identity.domain.Role; import com.got.retailpos.inventory.application.*; import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository; import com.got.retailpos.sales.application.SalesService; import com.got.retailpos.sales.domain.SaleStatus;

@Import(TestcontainersConfiguration.class) @SpringBootTest
class SalesConcurrencyIntegrationTests {
	@Autowired ProductCatalogService catalog; @Autowired UserAccountService users; @Autowired SupplierService suppliers; @Autowired InventoryService inventory; @Autowired SalesService sales; @Autowired InventoryBalanceRepository balances;
	@Test void shouldAllowOnlyOneCheckoutForLastItem() throws Exception {
		var category=catalog.createCategory("concurrency-"+UUID.randomUUID());var product=catalog.createProduct(new ProductCatalogService.ProductInput(category.getId(),"LAST-"+UUID.randomUUID(),null,"ชิ้นสุดท้าย",new BigDecimal("50.00"),0));var actor=users.create("cashier-"+UUID.randomUUID(),"correct-password","Cashier",Role.CASHIER);var manager=users.create("manager-"+UUID.randomUUID(),"correct-password","Manager",Role.MANAGER);var supplier=suppliers.create(new SupplierService.SupplierInput("supplier-"+UUID.randomUUID(),null,null));inventory.postReceipt(new InventoryService.ReceiptInput(supplier.getId(),"GR-"+UUID.randomUUID(),Instant.now().minusSeconds(1),null,List.of(new InventoryService.ReceiptItemInput(product.getId(),1,new BigDecimal("30.0000")))),manager.getId());
		var input=new SalesService.CreateSaleInput(null,List.of(new SalesService.SaleItemInput(product.getId(),1)));var first=sales.create(input,actor.getId());var second=sales.create(input,actor.getId());var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
		Callable<Boolean> checkoutFirst=()->checkout(first.getId(),"race-1",actor.getId(),ready,start);Callable<Boolean> checkoutSecond=()->checkout(second.getId(),"race-2",actor.getId(),ready,start);var a=executor.submit(checkoutFirst);var b=executor.submit(checkoutSecond);ready.await(5,TimeUnit.SECONDS);start.countDown();var results=List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));executor.shutdownNow();
		assertThat(results).containsExactlyInAnyOrder(true,false);assertThat(balances.findById(product.getId()).orElseThrow().getOnHand()).isZero();assertThat(List.of(sales.findById(first.getId()).getStatus(),sales.findById(second.getId()).getStatus()).stream().filter(status->status==SaleStatus.COMPLETED)).hasSize(1);
	}
	private boolean checkout(UUID sale,String key,UUID actor,CountDownLatch ready,CountDownLatch start){try{ready.countDown();start.await();sales.checkoutCash(sale,key,new BigDecimal("50.00"),actor);return true;}catch(Exception exception){return false;}}
}
