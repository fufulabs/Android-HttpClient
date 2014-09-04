package com.levelup.http.ion.internal;

import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.ion.builder.Builders;
import com.levelup.http.body.HttpBodyJSON;

/**
 * Created by Steve Lhomme on 15/07/2014.
 */
public class IonHttpBodyJSON extends HttpBodyJSON implements IonBody {

	public IonHttpBodyJSON(HttpBodyJSON sourceBody) {
		super(sourceBody);
	}

	@Override
	public String getContentType() {
		return JSONObjectBody.CONTENT_TYPE;
	}

	@Override
	public void setOutputData(Builders.Any.B requestBuilder) {
		requestBuilder.setJsonObjectBody(jsonObject);
	}
}
