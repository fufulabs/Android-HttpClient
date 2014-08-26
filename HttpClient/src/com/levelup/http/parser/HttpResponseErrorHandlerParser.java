package com.levelup.http.parser;

import java.io.IOException;

import com.levelup.http.BaseHttpResponseErrorHandler;
import com.levelup.http.DataErrorException;
import com.levelup.http.HttpResponse;
import com.levelup.http.HttpResponseErrorHandler;
import com.levelup.http.ImmutableHttpRequest;
import com.levelup.http.parser.XferTransform;

/**
 * Created by robUx4 on 26/08/2014.
 */
public class HttpResponseErrorHandlerParser implements HttpResponseErrorHandler {

	public final XferTransform<HttpResponse, ?> errorDataParser;

	public HttpResponseErrorHandlerParser(XferTransform<HttpResponse,?> errorDataParser) {
		this.errorDataParser = errorDataParser;
	}

	@Override
	public DataErrorException handleError(HttpResponse httpResponse, ImmutableHttpRequest request, Exception cause) {
		try {
			Object errorData = errorDataParser.transformData(httpResponse, request);
			return new DataErrorException(errorData, cause);
		} catch (IOException e) {
			return BaseHttpResponseErrorHandler.INSTANCE.handleError(httpResponse, request, cause);
		}
	}
}
