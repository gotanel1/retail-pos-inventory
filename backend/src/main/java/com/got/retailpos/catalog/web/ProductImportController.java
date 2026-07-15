package com.got.retailpos.catalog.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.got.retailpos.catalog.application.ProductImportService;
import com.got.retailpos.identity.security.RetailUserPrincipal;

@RestController
@RequestMapping("/api/v1/product-imports")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
public class ProductImportController {

	private final ProductImportService service;

	public ProductImportController(ProductImportService service) {
		this.service = service;
	}

	@PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ProductImportService.ImportPreview preview(
			@RequestPart("file") MultipartFile file,
			Authentication authentication) {
		return service.preview(file, principal(authentication).id());
	}

	@PostMapping("/{id}/commit")
	public ProductImportService.ImportCommitResult commit(
			@PathVariable UUID id,
			Authentication authentication) {
		return service.commit(id, principal(authentication).id());
	}

	private RetailUserPrincipal principal(Authentication authentication) {
		return (RetailUserPrincipal) authentication.getPrincipal();
	}
}
