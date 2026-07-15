package com.got.retailpos.identity.application;

public class ManagerPinRateLimitException extends RuntimeException {
	public ManagerPinRateLimitException() { super("Manager PIN ถูกลองผิดหลายครั้ง กรุณารอ 15 นาที"); }
}
