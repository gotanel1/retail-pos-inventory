package com.got.retailpos.identity.security;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
	}
}
