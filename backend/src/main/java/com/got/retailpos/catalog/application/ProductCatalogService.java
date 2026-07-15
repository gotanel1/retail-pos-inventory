package com.got.retailpos.catalog.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.got.retailpos.catalog.domain.Category;
import com.got.retailpos.catalog.domain.Product;
import com.got.retailpos.catalog.infrastructure.CategoryRepository;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class ProductCatalogService implements ProductCatalogReader {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	public ProductCatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<Category> findCategories() {
		return categoryRepository.findAll().stream()
				.sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
				.toList();
	}

	@Transactional
	public Category createCategory(String name) {
		var cleanName = name.strip();
		var normalizedName = normalizeCategoryName(cleanName);
		if (categoryRepository.existsByNormalizedName(normalizedName)) {
			throw new CatalogConflictException("มีหมวดหมู่ชื่อนี้แล้ว: " + cleanName);
		}
		return categoryRepository.save(Category.create(cleanName, normalizedName));
	}

	@Transactional(readOnly = true)
	public Page<Product> findProducts(String search, Pageable pageable) {
		return StringUtils.hasText(search)
				? productRepository.search(search.strip(), pageable)
				: productRepository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Map<UUID, CatalogProduct> findActiveProducts(java.util.Collection<UUID> productIds) {
		return productRepository.findAllByIdInAndActiveTrue(productIds).stream()
				.collect(java.util.stream.Collectors.toUnmodifiableMap(
						Product::getId,
						product -> new CatalogProduct(product.getId(), product.getSku(), product.getName(), product.getSalePrice())));
	}

	@Transactional
	public Product createProduct(ProductInput input) {
		validateProductInput(input);
		var category = findCategory(input.categoryId());
		var sku = normalizeSku(input.sku());
		var barcode = normalizeBarcode(input.barcode());
		validateUniqueProduct(sku, barcode, null);

		return productRepository.save(Product.create(
				category,
				sku,
				barcode,
				input.name().strip(),
				normalizePrice(input.salePrice()),
				input.lowStockThreshold()));
	}

	@Transactional
	public Product updateProduct(UUID id, ProductInput input) {
		validateProductInput(input);
		var product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("สินค้า"));
		var category = findCategory(input.categoryId());
		var sku = normalizeSku(input.sku());
		var barcode = normalizeBarcode(input.barcode());
		validateUniqueProduct(sku, barcode, id);

		product.update(
				category,
				sku,
				barcode,
				input.name().strip(),
				normalizePrice(input.salePrice()),
				input.lowStockThreshold());
		return product;
	}

	private Category findCategory(UUID categoryId) {
		return categoryRepository.findById(categoryId)
				.filter(Category::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("หมวดหมู่สินค้า"));
	}

	private void validateUniqueProduct(String sku, String barcode, UUID currentId) {
		var duplicatedSku = currentId == null
				? productRepository.existsBySkuIgnoreCase(sku)
				: productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentId);
		if (duplicatedSku) {
			throw new CatalogConflictException("SKU ถูกใช้แล้ว: " + sku);
		}

		if (barcode == null) {
			return;
		}
		var duplicatedBarcode = currentId == null
				? productRepository.existsByBarcode(barcode)
				: productRepository.existsByBarcodeAndIdNot(barcode, currentId);
		if (duplicatedBarcode) {
			throw new CatalogConflictException("Barcode ถูกใช้แล้ว: " + barcode);
		}
	}

	private String normalizeSku(String sku) {
		return sku.strip().toUpperCase(Locale.ROOT);
	}

	private String normalizeBarcode(String barcode) {
		return StringUtils.hasText(barcode) ? barcode.strip() : null;
	}

	private BigDecimal normalizePrice(BigDecimal price) {
		if (price.signum() < 0 || price.scale() > 2) {
			throw new IllegalArgumentException("ราคาขายต้องไม่ติดลบและมีทศนิยมไม่เกิน 2 ตำแหน่ง");
		}
		return price.setScale(2, RoundingMode.UNNECESSARY);
	}

	private void validateProductInput(ProductInput input) {
		if (input.lowStockThreshold() < 0) {
			throw new IllegalArgumentException("จุดเตือนสต็อกต่ำต้องไม่ติดลบ");
		}
		if (!StringUtils.hasText(input.sku()) || !StringUtils.hasText(input.name())) {
			throw new IllegalArgumentException("SKU และชื่อสินค้าห้ามว่าง");
		}
	}

	public static String normalizeCategoryName(String name) {
		return Normalizer.normalize(name.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
	}

	public record ProductInput(
			UUID categoryId,
			String sku,
			String barcode,
			String name,
			BigDecimal salePrice,
			int lowStockThreshold) {
	}
}
