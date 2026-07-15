package com.got.retailpos.identity.application;

public class UsernameAlreadyExistsException extends RuntimeException {

	public UsernameAlreadyExistsException(String username) {
		super("ชื่อผู้ใช้นี้ถูกใช้แล้ว: " + username);
	}
}
