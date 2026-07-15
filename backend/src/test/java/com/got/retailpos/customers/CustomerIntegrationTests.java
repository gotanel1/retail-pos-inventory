package com.got.retailpos.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.customers.application.CustomerService;
import com.got.retailpos.customers.infrastructure.CustomerRepository;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.security.RetailUserPrincipal;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerIntegrationTests {

	@Autowired MockMvc mockMvc;
	@Autowired UserAccountService userService;
	@Autowired CustomerService customerService;
	@Autowired CustomerRepository customerRepository;

	@Test
	void shouldLetCashierCreateAndSearchCustomer() throws Exception {
		var cashier = principal("cashier", Role.CASHIER);
		mockMvc.perform(post("/api/v1/customers").with(user(cashier)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"สมชาย ใจดี","phone":"081-234-5678","note":"ลูกค้าประจำ","marketingConsent":true}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name", is("สมชาย ใจดี")))
				.andExpect(jsonPath("$.marketingConsent", is(true)));

		mockMvc.perform(get("/api/v1/customers").with(user(cashier)).param("search", "081234"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	void shouldRejectDuplicateNormalizedPhone() throws Exception {
		var manager = principal("manager", Role.MANAGER);
		customerService.create(new CustomerService.CustomerInput("คนแรก", "081-234-5678", null, false));

		mockMvc.perform(post("/api/v1/customers").with(user(manager)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"คนที่สอง","phone":"0812345678","marketingConsent":false}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldForbidInventoryStaffFromCustomerData() throws Exception {
		var staff = principal("stock", Role.INVENTORY_STAFF);
		mockMvc.perform(get("/api/v1/customers").with(user(staff))).andExpect(status().isForbidden());
	}

	@Test
	void shouldAnonymizeInsteadOfDeletingCustomer() throws Exception {
		var owner = principal("owner", Role.OWNER);
		var customer = customerService.create(new CustomerService.CustomerInput("ลูกค้าทดสอบ", "0899999999", "ข้อมูลส่วนตัว", true));

		mockMvc.perform(delete("/api/v1/customers/{id}", customer.getId()).with(user(owner)).with(csrf()))
				.andExpect(status().isNoContent());

		var stored = customerRepository.findById(customer.getId()).orElseThrow();
		assertThat(stored.isActive()).isFalse();
		assertThat(stored.getPhone()).isNull();
		assertThat(stored.getNote()).isNull();
		assertThat(stored.isMarketingConsent()).isFalse();
		assertThat(stored.getAnonymizedAt()).isNotNull();
	}

	private RetailUserPrincipal principal(String prefix, Role role) {
		var username = prefix + "-" + UUID.randomUUID();
		return RetailUserPrincipal.from(userService.create(username, "correct-password", username, role));
	}
}
