package com.levelup.http;

import org.json.JSONObject;

import junit.framework.TestCase;

public class InputStreamJSONObjectParserTest extends TestCase {

	public void testBogusData() throws Exception {
		BaseHttpRequest<JSONObject> request = new BaseHttpRequest.Builder<JSONObject>().setUrl("http://android.com/").setStreamParser(InputStreamJSONObjectParser.instance).build();

		try {
			HttpClient.parseRequest(request);
		} catch (HttpException e) {
			if (e.getErrorCode() != HttpException.ERROR_JSON)
				throw e; // forward
			assertNotNull(e.getMessage());
			assertTrue(e.getErrorMessage().startsWith("Bad JSON data"));
		}
	}
}