package com.got.retailpos.catalog.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ProductCatalogReader {

	Map<UUID, CatalogProduct> findActiveProducts(Collection<UUID> productIds);

	record CatalogProduct(UUID id, String sku, String name, java.math.BigDecimal salePrice) {
	}
}
