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
