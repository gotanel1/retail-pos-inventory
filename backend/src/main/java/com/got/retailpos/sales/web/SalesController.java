package com.got.retailpos.sales.web;

import java.math.BigDecimal; import java.time.Instant; import java.util.*;
import org.springframework.data.domain.Pageable; import org.springframework.data.web.PageableDefault; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import com.got.retailpos.identity.security.RetailUserPrincipal; import com.got.retailpos.sales.application.SalesService; import com.got.retailpos.sales.domain.*; import com.got.retailpos.shared.web.PageResponse;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;

@RestController @RequestMapping("/api/v1/sales")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','CASHIER')")
public class SalesController {
	private final SalesService service; public SalesController(SalesService service){this.service=service;}
	@GetMapping public PageResponse<SaleResponse> findAll(@PageableDefault(size=20,sort="createdAt",direction=org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable){return PageResponse.from(service.findAll(pageable).map(SaleResponse::from));}
	@GetMapping("/{id}") public SaleResponse findById(@PathVariable UUID id){return SaleResponse.from(service.findById(id));}
	@PostMapping @ResponseStatus(HttpStatus.CREATED) public SaleResponse create(@Valid @RequestBody CreateSaleRequest body,Authentication authentication){return SaleResponse.from(service.create(body.toInput(),principal(authentication).id()));}
	@PostMapping("/{id}/discount") public SaleResponse discount(@PathVariable UUID id,@Valid @RequestBody DiscountRequest body){return SaleResponse.from(service.applyDiscount(id,body.toInput()));}
	@PostMapping("/{id}/checkout/cash") public SaleResponse checkoutCash(@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CashRequest body,Authentication authentication){return SaleResponse.from(service.checkoutCash(id,key,body.cashReceived(),principal(authentication).id()));}
	private RetailUserPrincipal principal(Authentication auth){return (RetailUserPrincipal)auth.getPrincipal();}
	public record CreateSaleRequest(UUID customerId,@NotEmpty @Size(max=100) List<@Valid SaleItemRequest> items){SalesService.CreateSaleInput toInput(){return new SalesService.CreateSaleInput(customerId,items.stream().map(SaleItemRequest::toInput).toList());}}
	public record SaleItemRequest(@NotNull UUID productId,@Positive int quantity){SalesService.SaleItemInput toInput(){return new SalesService.SaleItemInput(productId,quantity);}}
	public record DiscountRequest(@NotNull DiscountType type,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal value,@NotBlank String managerUsername,@Pattern(regexp="^[0-9]{4,6}$") String managerPin){SalesService.DiscountInput toInput(){return new SalesService.DiscountInput(type,value,managerUsername,managerPin);}}
	public record CashRequest(@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal cashReceived){}
	public record SaleResponse(UUID id,String receiptNumber,SaleStatus status,UUID customerId,BigDecimal subtotal,DiscountType discountType,BigDecimal discountValue,BigDecimal discountAmount,boolean vatEnabled,BigDecimal vatRate,BigDecimal vatAmount,BigDecimal total,BigDecimal cashReceived,BigDecimal changeAmount,UUID discountApprovedBy,UUID createdBy,UUID completedBy,Instant createdAt,Instant completedAt,List<ItemResponse> items){static SaleResponse from(Sale s){return new SaleResponse(s.getId(),s.getReceiptNumber(),s.getStatus(),s.getCustomerId(),s.getSubtotal(),s.getDiscountType(),s.getDiscountValue(),s.getDiscountAmount(),s.isVatEnabled(),s.getVatRate(),s.getVatAmount(),s.getTotal(),s.getCashReceived(),s.getChangeAmount(),s.getDiscountApprovedBy(),s.getCreatedBy(),s.getCompletedBy(),s.getCreatedAt(),s.getCompletedAt(),s.getItems().stream().map(ItemResponse::from).toList());}}
	public record ItemResponse(UUID productId,String sku,String name,int quantity,BigDecimal unitPrice,BigDecimal unitCostSnapshot,BigDecimal lineTotal){static ItemResponse from(SaleItem i){return new ItemResponse(i.getProductId(),i.getSkuSnapshot(),i.getNameSnapshot(),i.getQuantity(),i.getUnitPrice(),i.getUnitCostSnapshot(),i.getLineTotal());}}
}
