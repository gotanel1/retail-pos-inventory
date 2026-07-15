package com.got.retailpos.catalog;

import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

final class JsonTestSupport {

	private JsonTestSupport() {
	}

	static String read(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), path);
	}
}
