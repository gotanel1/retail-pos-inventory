package com.got.retailpos.catalog.application;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.got.retailpos.catalog.domain.Category;
import com.got.retailpos.catalog.domain.Product;
import com.got.retailpos.catalog.domain.ProductImport;
import com.got.retailpos.catalog.domain.ProductImportStatus;
import com.got.retailpos.catalog.infrastructure.CategoryRepository;
import com.got.retailpos.catalog.infrastructure.ProductImportRepository;
import com.got.retailpos.catalog.infrastructure.ProductRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class ProductImportService {

	private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
	private static final int MAX_ROWS = 1_000;
	private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9._-]+$");
	private static final Pattern BARCODE_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{4,64}$");
	private static final Set<String> REQUIRED_HEADERS = Set.of(
			"sku", "barcode", "name", "category", "saleprice", "lowstockthreshold");

	private final ProductImportRepository importRepository;
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public ProductImportService(
			ProductImportRepository importRepository,
			ProductRepository productRepository,
			CategoryRepository categoryRepository) {
		this.importRepository = importRepository;
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public ImportPreview preview(MultipartFile file, UUID userId) {
		var rawContent = readUtf8(file);
		var parsed = parseAndValidate(rawContent);
		var validRows = (int) parsed.rows().stream().filter(ParsedRow::isValid).count();
		var originalFilename = StringUtils.hasText(file.getOriginalFilename())
				? StringUtils.cleanPath(file.getOriginalFilename())
				: "products.csv";

		var productImport = importRepository.save(ProductImport.preview(
				originalFilename,
				rawContent,
				parsed.rows().size(),
				validRows,
				parsed.rows().size() - validRows,
				userId));

		return ImportPreview.from(productImport, parsed.rows());
	}

	@Transactional
	public ImportCommitResult commit(UUID importId, UUID userId) {
		var productImport = importRepository.findByIdForUpdate(importId)
				.filter(item -> item.getCreatedBy().equals(userId))
				.orElseThrow(() -> new ResourceNotFoundException("รายการนำเข้าสินค้า"));
		if (productImport.getStatus() != ProductImportStatus.PREVIEWED) {
			throw new CatalogConflictException("รายการนำเข้านี้ถูกยืนยันแล้ว");
		}
		if (productImport.isExpired()) {
			throw new CatalogConflictException("รายการ preview หมดอายุ กรุณาอัปโหลดไฟล์ใหม่");
		}

		var parsed = parseAndValidate(productImport.getRawContent());
		if (parsed.rows().stream().anyMatch(row -> !row.isValid())) {
			throw new CatalogConflictException("ข้อมูลเปลี่ยนจากตอน preview กรุณาตรวจสอบและอัปโหลดใหม่");
		}

		var categories = resolveCategories(parsed.rows());
		var products = parsed.rows().stream()
				.map(row -> Product.create(
						categories.get(ProductCatalogService.normalizeCategoryName(row.category)),
						row.sku,
						row.barcode,
						row.name,
						row.salePrice,
						row.lowStockThreshold))
				.toList();
		productRepository.saveAll(products);
		productImport.complete();
		return new ImportCommitResult(productImport.getId(), products.size());
	}

	private Map<String, Category> resolveCategories(List<ParsedRow> rows) {
		var normalizedNames = rows.stream()
				.map(row -> ProductCatalogService.normalizeCategoryName(row.category))
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		var categories = new HashMap<String, Category>();
		categoryRepository.findAllByNormalizedNameIn(normalizedNames)
				.forEach(category -> categories.put(category.getNormalizedName(), category));

		for (var row : rows) {
			var normalizedName = ProductCatalogService.normalizeCategoryName(row.category);
			categories.computeIfAbsent(normalizedName, ignored ->
					categoryRepository.save(Category.create(row.category, normalizedName)));
		}
		return categories;
	}

	private ParsedFile parseAndValidate(String rawContent) {
		try (var parser = CSVFormat.RFC4180.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.setIgnoreHeaderCase(true)
				.setIgnoreSurroundingSpaces(true)
				.setTrim(true)
				.setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
				.get()
				.parse(new StringReader(stripBom(rawContent)))) {
			validateHeaders(parser.getHeaderMap().keySet());
			var rows = new ArrayList<ParsedRow>();
			for (var record : parser) {
				if (rows.size() >= MAX_ROWS) {
					throw new InvalidCsvException("ไฟล์ CSV มีได้ไม่เกิน " + MAX_ROWS + " แถว");
				}
				rows.add(parseRow(record));
			}
			if (rows.isEmpty()) {
				throw new InvalidCsvException("ไฟล์ CSV ไม่มีข้อมูลสินค้า");
			}
			addDuplicateAndDatabaseErrors(rows);
			return new ParsedFile(rows);
		}
		catch (IOException | IllegalArgumentException exception) {
			throw new InvalidCsvException("ไม่สามารถอ่านรูปแบบไฟล์ CSV ได้", exception);
		}
	}

	private ParsedRow parseRow(CSVRecord record) {
		var row = new ParsedRow(record.getRecordNumber() + 1);
		row.sku = record.get("sku").strip().toUpperCase(Locale.ROOT);
		row.barcode = nullable(record.get("barcode"));
		row.name = record.get("name").strip();
		row.category = record.get("category").strip();

		if (!SKU_PATTERN.matcher(row.sku).matches() || row.sku.length() > 80) {
			row.errors.add("SKU ต้องเป็น A-Z, 0-9, จุด, ขีดกลาง หรือขีดล่าง และยาวไม่เกิน 80 ตัว");
		}
		if (row.barcode != null && !BARCODE_PATTERN.matcher(row.barcode).matches()) {
			row.errors.add("Barcode ต้องยาว 4-64 ตัวและมีเฉพาะตัวอักษร ตัวเลข จุด ขีดกลาง หรือขีดล่าง");
		}
		if (!StringUtils.hasText(row.name) || row.name.length() > 180) {
			row.errors.add("ชื่อสินค้าห้ามว่างและยาวไม่เกิน 180 ตัว");
		}
		if (!StringUtils.hasText(row.category) || row.category.length() > 120) {
			row.errors.add("หมวดหมู่ห้ามว่างและยาวไม่เกิน 120 ตัว");
		}
		parsePrice(record.get("salePrice"), row);
		parseThreshold(record.get("lowStockThreshold"), row);
		return row;
	}

	private void parsePrice(String value, ParsedRow row) {
		try {
			var price = new BigDecimal(value.strip());
			if (price.signum() < 0 || price.scale() > 2 || price.precision() - price.scale() > 17) {
				throw new NumberFormatException();
			}
			row.salePrice = price.setScale(2, RoundingMode.UNNECESSARY);
		}
		catch (NumberFormatException exception) {
			row.errors.add("ราคาขายต้องเป็นเลขไม่ติดลบและมีทศนิยมไม่เกิน 2 ตำแหน่ง");
		}
	}

	private void parseThreshold(String value, ParsedRow row) {
		try {
			row.lowStockThreshold = Integer.parseInt(value.strip());
			if (row.lowStockThreshold < 0) {
				throw new NumberFormatException();
			}
		}
		catch (NumberFormatException exception) {
			row.errors.add("จุดเตือนสต็อกต่ำต้องเป็นจำนวนเต็มตั้งแต่ 0 ขึ้นไป");
		}
	}

	private void addDuplicateAndDatabaseErrors(List<ParsedRow> rows) {
		var seenSkus = new HashSet<String>();
		var seenBarcodes = new HashSet<String>();
		for (var row : rows) {
			if (!seenSkus.add(row.sku)) {
				row.errors.add("SKU ซ้ำภายในไฟล์");
			}
			if (row.barcode != null && !seenBarcodes.add(row.barcode)) {
				row.errors.add("Barcode ซ้ำภายในไฟล์");
			}
		}

		var existingSkus = new HashSet<>(productRepository.findExistingSkus(seenSkus));
		var existingBarcodes = seenBarcodes.isEmpty()
				? Set.<String>of()
				: new HashSet<>(productRepository.findExistingBarcodes(seenBarcodes));
		for (var row : rows) {
			if (existingSkus.contains(row.sku)) {
				row.errors.add("SKU มีอยู่ในระบบแล้ว");
			}
			if (row.barcode != null && existingBarcodes.contains(row.barcode)) {
				row.errors.add("Barcode มีอยู่ในระบบแล้ว");
			}
		}
	}

	private void validateHeaders(Set<String> headers) {
		var normalizedHeaders = headers.stream()
				.map(header -> stripBom(header).strip().toLowerCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.toSet());
		if (!normalizedHeaders.containsAll(REQUIRED_HEADERS)) {
			throw new InvalidCsvException("CSV ต้องมี header: sku, barcode, name, category, salePrice, lowStockThreshold");
		}
	}

	private String readUtf8(MultipartFile file) {
		if (file.isEmpty()) {
			throw new InvalidCsvException("กรุณาเลือกไฟล์ CSV");
		}
		if (file.getSize() > MAX_FILE_BYTES) {
			throw new InvalidCsvException("ไฟล์ CSV ต้องมีขนาดไม่เกิน 2 MB");
		}
		try {
			var content = new String(file.getBytes(), StandardCharsets.UTF_8);
			if (content.indexOf('\uFFFD') >= 0) {
				throw new InvalidCsvException("ไฟล์ต้องเข้ารหัสแบบ UTF-8");
			}
			return content;
		}
		catch (IOException exception) {
			throw new InvalidCsvException("ไม่สามารถอ่านไฟล์ CSV ได้", exception);
		}
	}

	private String stripBom(String value) {
		return value.startsWith("\uFEFF") ? value.substring(1) : value;
	}

	private String nullable(String value) {
		return StringUtils.hasText(value) ? value.strip() : null;
	}

	private record ParsedFile(List<ParsedRow> rows) {
	}

	private static final class ParsedRow {
		private final long rowNumber;
		private String sku;
		private String barcode;
		private String name;
		private String category;
		private BigDecimal salePrice;
		private int lowStockThreshold;
		private final List<String> errors = new ArrayList<>();

		private ParsedRow(long rowNumber) {
			this.rowNumber = rowNumber;
		}

		private boolean isValid() {
			return errors.isEmpty();
		}
	}

	public record ImportRowPreview(
			long rowNumber,
			String sku,
			String barcode,
			String name,
			String category,
			BigDecimal salePrice,
			Integer lowStockThreshold,
			List<String> errors) {

		private static ImportRowPreview from(ParsedRow row) {
			return new ImportRowPreview(
					row.rowNumber,
					row.sku,
					row.barcode,
					row.name,
					row.category,
					row.salePrice,
					row.errors.stream().anyMatch(error -> error.startsWith("จุดเตือน"))
							? null : row.lowStockThreshold,
					List.copyOf(row.errors));
		}
	}

	public record ImportPreview(
			UUID importId,
			String filename,
			int totalRows,
			int validRows,
			int invalidRows,
			java.time.Instant expiresAt,
			List<ImportRowPreview> rows) {

		private static ImportPreview from(ProductImport productImport, List<ParsedRow> rows) {
			return new ImportPreview(
					productImport.getId(),
					productImport.getOriginalFilename(),
					productImport.getTotalRows(),
					productImport.getValidRows(),
					productImport.getInvalidRows(),
					productImport.getExpiresAt(),
					rows.stream().map(ImportRowPreview::from).toList());
		}
	}

	public record ImportCommitResult(UUID importId, int createdProducts) {
	}
}
