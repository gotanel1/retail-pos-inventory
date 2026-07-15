package com.got.retailpos.shared.application;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName) {
		super("ไม่พบข้อมูล " + resourceName);
	}
}
