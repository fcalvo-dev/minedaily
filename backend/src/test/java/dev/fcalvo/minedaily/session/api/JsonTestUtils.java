package dev.fcalvo.minedaily.session.api;

import com.jayway.jsonpath.JsonPath;

import org.springframework.test.web.servlet.MvcResult;

final class JsonTestUtils {

	private JsonTestUtils() {
	}

	static String readString(MvcResult mvcResult, String path) throws Exception {
		return JsonPath.read(mvcResult.getResponse().getContentAsString(), path);
	}

}
