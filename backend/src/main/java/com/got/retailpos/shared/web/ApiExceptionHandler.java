package com.got.retailpos.shared.web;

import java.net.URI;
import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.got.retailpos.identity.application.UsernameAlreadyExistsException;
import com.got.retailpos.identity.security.InvalidCredentialsException;
import com.got.retailpos.catalog.application.CatalogConflictException;
import com.got.retailpos.catalog.application.InvalidCsvException;
import com.got.retailpos.inventory.application.InventoryConflictException;
import com.got.retailpos.customers.application.CustomerConflictException;
import com.got.retailpos.identity.application.InvalidManagerPinException;
import com.got.retailpos.identity.application.ManagerPinRateLimitException;
import com.got.retailpos.sales.application.SaleConflictException;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "เข้าสู่ระบบไม่สำเร็จ", exception.getMessage());
	}

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "สร้างผู้ใช้ไม่สำเร็จ", exception.getMessage());
	}

	@ExceptionHandler(CatalogConflictException.class)
	ProblemDetail handleCatalogConflict(CatalogConflictException exception) {
		return problem(HttpStatus.CONFLICT, "ข้อมูลสินค้าซ้ำ", exception.getMessage());
	}

	@ExceptionHandler(InvalidCsvException.class)
	ProblemDetail handleInvalidCsv(InvalidCsvException exception) {
		return problem(HttpStatus.BAD_REQUEST, "ไฟล์ CSV ไม่ถูกต้อง", exception.getMessage());
	}

	@ExceptionHandler(InventoryConflictException.class)
	ProblemDetail handleInventoryConflict(InventoryConflictException exception) {
		return problem(HttpStatus.CONFLICT, "ข้อมูลสต็อกขัดแย้ง", exception.getMessage());
	}

	@ExceptionHandler(CustomerConflictException.class)
	ProblemDetail handleCustomerConflict(CustomerConflictException exception) {
		return problem(HttpStatus.CONFLICT, "ข้อมูลลูกค้าซ้ำ", exception.getMessage());
	}

	@ExceptionHandler(InvalidManagerPinException.class)
	ProblemDetail handleInvalidManagerPin(InvalidManagerPinException exception) {
		return problem(HttpStatus.FORBIDDEN, "อนุมัติส่วนลดไม่สำเร็จ", exception.getMessage());
	}

	@ExceptionHandler(ManagerPinRateLimitException.class)
	ProblemDetail handleManagerPinRateLimit(ManagerPinRateLimitException exception) {
		return problem(HttpStatus.TOO_MANY_REQUESTS, "Manager PIN ถูกระงับชั่วคราว", exception.getMessage());
	}

	@ExceptionHandler(SaleConflictException.class)
	ProblemDetail handleSaleConflict(SaleConflictException exception) {
		return problem(HttpStatus.CONFLICT, "สถานะบิลขัดแย้ง", exception.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "ไม่พบข้อมูล", exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ถูกต้อง", exception.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return problem(HttpStatus.CONFLICT, "ข้อมูลขัดแย้ง", "ข้อมูลถูกเปลี่ยนโดยรายการอื่น กรุณาตรวจสอบแล้วลองใหม่");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		var errors = new LinkedHashMap<String, String>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		var problem = problem(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ถูกต้อง", "กรุณาตรวจสอบข้อมูลที่ส่งมา");
		problem.setProperty("errors", errors);
		return problem;
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("about:blank"));
		return problem;
	}
}
