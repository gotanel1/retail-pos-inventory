package com.got.retailpos.identity;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.got.retailpos.TestcontainersConfiguration;
import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.identity.security.RetailUserPrincipal;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserAccountService userAccountService;

	@Autowired
	UserAccountRepository repository;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
	}

	@Test
	void shouldRequireCsrfTokenWhenLoggingIn() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody("owner", "correct-password")))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void shouldKeepAuthenticatedUserInHttpSession() throws Exception {
		userAccountService.create("owner", "correct-password", "เจ้าของร้าน", Role.OWNER);

		var loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody("OWNER", "correct-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is("owner")))
				.andExpect(jsonPath("$.role", is("OWNER")))
				.andReturn();

		var session = (MockHttpSession) loginResult.getRequest().getSession(false);
		mockMvc.perform(get("/api/v1/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName", is("เจ้าของร้าน")));
	}

	@Test
	void shouldReturnProblemDetailsForInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody("unknown", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("เข้าสู่ระบบไม่สำเร็จ")));
	}

	@Test
	void shouldExposeCsrfTokenWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName", is("X-CSRF-TOKEN")))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void shouldRejectUnauthenticatedRequestToCurrentUser() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void shouldAllowManagerToReadUsersButNotCreateThem() throws Exception {
		var manager = userAccountService.create("manager", "correct-password", "ผู้จัดการ", Role.MANAGER);
		var principal = RetailUserPrincipal.from(manager);

		mockMvc.perform(get("/api/v1/users").with(user(principal)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/users")
				.with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(createUserBody("cashier")))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldAllowOnlyOwnerToCreateUser() throws Exception {
		var owner = userAccountService.create("owner", "correct-password", "เจ้าของร้าน", Role.OWNER);

		mockMvc.perform(post("/api/v1/users")
				.with(user(RetailUserPrincipal.from(owner)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(createUserBody("cashier")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("cashier")))
				.andExpect(jsonPath("$.role", is("CASHIER")));
	}

	@Test
	void shouldRejectCashierFromUserManagement() throws Exception {
		var cashier = userAccountService.create("cashier", "correct-password", "แคชเชียร์", Role.CASHIER);

		mockMvc.perform(get("/api/v1/users").with(user(RetailUserPrincipal.from(cashier))))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldRejectInventoryStaffFromUserManagement() throws Exception {
		var staff = userAccountService.create("stock", "correct-password", "พนักงานสต็อก", Role.INVENTORY_STAFF);

		mockMvc.perform(get("/api/v1/users").with(user(RetailUserPrincipal.from(staff))))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
		var owner = userAccountService.create("owner", "correct-password", "เจ้าของร้าน", Role.OWNER);
		userAccountService.create("cashier", "correct-password", "พนักงานเดิม", Role.CASHIER);

		mockMvc.perform(post("/api/v1/users")
				.with(user(RetailUserPrincipal.from(owner)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(createUserBody("CASHIER")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title", is("สร้างผู้ใช้ไม่สำเร็จ")));
	}

	private String loginBody(String username, String password) {
		return """
				{
				  "username": "%s",
				  "password": "%s"
				}
				""".formatted(username, password);
	}

	private String createUserBody(String username) {
		return """
				{
				  "username": "%s",
				  "password": "cashier-password",
				  "displayName": "พนักงานขาย",
				  "role": "CASHIER"
				}
				""".formatted(username);
	}
}
