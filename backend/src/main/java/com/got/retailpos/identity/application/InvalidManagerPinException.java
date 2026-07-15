package com.got.retailpos.identity.application;

public class InvalidManagerPinException extends RuntimeException {
	public InvalidManagerPinException() { super("ชื่อผู้อนุมัติหรือ Manager PIN ไม่ถูกต้อง"); }
}
