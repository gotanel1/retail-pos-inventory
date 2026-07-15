package com.got.retailpos.shared.web;

import java.net.URI;
import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.got.retailpos.identity.application.UsernameAlreadyExistsException;
import com.got.retailpos.identity.security.InvalidCredentialsException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "เข้าสู่ระบบไม่สำเร็จ", exception.getMessage());
	}

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "สร้างผู้ใช้ไม่สำเร็จ", exception.getMessage());
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
