package com.got.retailpos.catalog.infrastructure;

import java.util.Collection;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.got.retailpos.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	boolean existsBySkuIgnoreCase(String sku);

	Optional<Product> findBySkuIgnoreCase(String sku);

	boolean existsByBarcode(String barcode);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

	boolean existsByBarcodeAndIdNot(String barcode, UUID id);

	@Query("""
			select product from Product product
			where lower(product.name) like lower(concat('%', :search, '%'))
			   or lower(product.sku) like lower(concat('%', :search, '%'))
			   or lower(coalesce(product.barcode, '')) like lower(concat('%', :search, '%'))
			""")
	Page<Product> search(@Param("search") String search, Pageable pageable);

	@Query("select upper(product.sku) from Product product where upper(product.sku) in :skus")
	List<String> findExistingSkus(@Param("skus") Set<String> skus);

	@Query("select product.barcode from Product product where product.barcode in :barcodes")
	List<String> findExistingBarcodes(@Param("barcodes") Set<String> barcodes);

	List<Product> findAllByIdInAndActiveTrue(Collection<UUID> ids);
}
