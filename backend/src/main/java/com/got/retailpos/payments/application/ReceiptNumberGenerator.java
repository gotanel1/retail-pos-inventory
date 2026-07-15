package com.got.retailpos.payments.application;
import java.time.*; import java.time.format.DateTimeFormatter; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Component;
@Component public class ReceiptNumberGenerator {
	private final JdbcTemplate jdbc; public ReceiptNumberGenerator(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public String next(){var seq=jdbc.queryForObject("SELECT nextval('receipt_number_seq')",Long.class);return "R-"+LocalDate.now(ZoneId.of("Asia/Bangkok")).format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+String.format("%06d",seq);}
}
