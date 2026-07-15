package com.got.retailpos.sales.web;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.got.retailpos.sales.application.SalesService;
import com.got.retailpos.shared.web.PageResponse;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/sales")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','CASHIER')")
public class CustomerSalesController {
	private final SalesService service;
	public CustomerSalesController(SalesService service){this.service=service;}
	@GetMapping public PageResponse<SalesController.SaleResponse> history(@PathVariable UUID customerId,@PageableDefault(size=20,sort="completedAt",direction=org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable){return PageResponse.from(service.findCustomerHistory(customerId,pageable).map(SalesController.SaleResponse::from));}
}
