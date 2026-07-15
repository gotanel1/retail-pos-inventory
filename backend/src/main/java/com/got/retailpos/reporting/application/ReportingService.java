package com.got.retailpos.reporting.application;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

	private final NamedParameterJdbcTemplate jdbc;
	private final ZoneId businessZone;

	public ReportingService(
			NamedParameterJdbcTemplate jdbc,
			@Value("${app.business-zone:Asia/Bangkok}") String businessZone) {
		this.jdbc = jdbc;
		this.businessZone = ZoneId.of(businessZone);
	}

	@Transactional(readOnly = true)
	public DashboardReport dashboard(LocalDate requestedFrom, LocalDate requestedTo) {
		var today = LocalDate.now(businessZone);
		var from = requestedFrom == null ? today : requestedFrom;
		var to = requestedTo == null ? today : requestedTo;
		validateRange(from, to);

		var fromInstant = from.atStartOfDay(businessZone).toInstant();
		var toExclusive = to.plusDays(1).atStartOfDay(businessZone).toInstant();
		var parameters = Map.<String, Object>of(
				"from", Timestamp.from(fromInstant),
				"to", Timestamp.from(toExclusive));

		return new DashboardReport(
				from,
				to,
				summary(parameters),
				paymentBreakdown(parameters),
				lowStock(),
				recentMovements(parameters));
	}

	private Summary summary(Map<String, Object> parameters) {
		var sales = jdbc.queryForMap("""
				WITH filtered_sales AS (
				    SELECT id, total, vat_amount
				    FROM sales
				    WHERE status = 'COMPLETED' AND completed_at >= :from AND completed_at < :to
				), costs AS (
				    SELECT COALESCE(SUM(item.unit_cost_snapshot * item.quantity), 0) AS cost_of_goods_sold
				    FROM sale_items item JOIN filtered_sales sale ON sale.id = item.sale_id
				)
				SELECT COUNT(*) AS sales_count,
				       COALESCE(SUM(total), 0) AS total_sales,
				       COALESCE(SUM(total - vat_amount), 0) AS net_sales_excluding_vat,
				       (SELECT cost_of_goods_sold FROM costs) AS cost_of_goods_sold
				FROM filtered_sales
				""", parameters);
		var inventoryValue = jdbc.queryForObject(
				"SELECT COALESCE(SUM(on_hand * average_cost), 0) FROM inventory_balances",
				Map.of(), BigDecimal.class);
		var netSales = decimal(sales.get("net_sales_excluding_vat"));
		var cost = decimal(sales.get("cost_of_goods_sold"));
		return new Summary(
				((Number) sales.get("sales_count")).longValue(),
				decimal(sales.get("total_sales")),
				netSales,
				cost,
				netSales.subtract(cost),
				inventoryValue == null ? BigDecimal.ZERO : inventoryValue);
	}

	private List<PaymentBreakdown> paymentBreakdown(Map<String, Object> parameters) {
		return jdbc.query("""
				SELECT payment.payment_method, COUNT(*) AS transaction_count, SUM(payment.amount) AS amount
				FROM payments payment
				JOIN sales sale ON sale.id = payment.sale_id
				WHERE payment.status = 'SUCCEEDED' AND sale.status = 'COMPLETED'
				  AND sale.completed_at >= :from AND sale.completed_at < :to
				GROUP BY payment.payment_method
				ORDER BY payment.payment_method
				""", parameters, (result, row) -> new PaymentBreakdown(
				result.getString("payment_method"),
				result.getLong("transaction_count"),
				result.getBigDecimal("amount")));
	}

	private List<LowStockItem> lowStock() {
		return jdbc.query("""
				SELECT product.id, product.sku, product.name, balance.on_hand, balance.reserved,
				       balance.on_hand - balance.reserved AS available, product.low_stock_threshold
				FROM products product
				JOIN inventory_balances balance ON balance.product_id = product.id
				WHERE product.active = TRUE
				  AND balance.on_hand - balance.reserved <= product.low_stock_threshold
				ORDER BY available, product.sku
				LIMIT 20
				""", Map.of(), (result, row) -> new LowStockItem(
				result.getObject("id", UUID.class),
				result.getString("sku"),
				result.getString("name"),
				result.getInt("on_hand"),
				result.getInt("reserved"),
				result.getInt("available"),
				result.getInt("low_stock_threshold")));
	}

	private List<Movement> recentMovements(Map<String, Object> parameters) {
		return jdbc.query("""
				SELECT movement.id, movement.product_id, product.sku, product.name, movement.movement_type,
				       movement.quantity_delta, movement.on_hand_after, movement.reserved_after,
				       movement.reference_type, movement.reference_id, movement.reason,
				       movement.actor_user_id, movement.occurred_at
				FROM stock_movements movement
				JOIN products product ON product.id = movement.product_id
				WHERE movement.occurred_at >= :from AND movement.occurred_at < :to
				ORDER BY movement.occurred_at DESC, movement.id DESC
				LIMIT 20
				""", parameters, (result, row) -> new Movement(
				result.getObject("id", UUID.class),
				result.getObject("product_id", UUID.class),
				result.getString("sku"),
				result.getString("name"),
				result.getString("movement_type"),
				result.getInt("quantity_delta"),
				result.getInt("on_hand_after"),
				result.getInt("reserved_after"),
				result.getString("reference_type"),
				result.getObject("reference_id", UUID.class),
				result.getString("reason"),
				result.getObject("actor_user_id", UUID.class),
				result.getTimestamp("occurred_at").toInstant()));
	}

	private void validateRange(LocalDate from, LocalDate to) {
		if (to.isBefore(from)) throw new IllegalArgumentException("วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่มต้น");
		if (ChronoUnit.DAYS.between(from, to) > 366) throw new IllegalArgumentException("ช่วงรายงานต้องไม่เกิน 366 วัน");
	}

	private BigDecimal decimal(Object value) {
		return value == null ? BigDecimal.ZERO : (BigDecimal) value;
	}

	public record DashboardReport(LocalDate from, LocalDate to, Summary summary,
			List<PaymentBreakdown> payments, List<LowStockItem> lowStock, List<Movement> movements) {}
	public record Summary(long salesCount, BigDecimal totalSales, BigDecimal netSalesExcludingVat,
			BigDecimal costOfGoodsSold, BigDecimal grossProfit, BigDecimal inventoryValue) {}
	public record PaymentBreakdown(String method, long transactionCount, BigDecimal amount) {}
	public record LowStockItem(UUID productId, String sku, String name, int onHand, int reserved,
			int available, int lowStockThreshold) {}
	public record Movement(UUID id, UUID productId, String sku, String productName, String movementType,
			int quantityDelta, int onHandAfter, int reservedAfter, String referenceType, UUID referenceId,
			String reason, UUID actorUserId, Instant occurredAt) {}
}
