package com.levelup.http.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.koushikdutta.async.http.body.StreamPart;

public class InputStreamPart extends StreamPart {

	private final InputStream inputStream;

	public InputStreamPart(String streamName, InputStream value) {
		super(streamName, -1, new ArrayList<NameValuePair>() {
			{
				add(new BasicNameValuePair("filename", "rawstream"));
			}
		});
		this.inputStream = value;
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return inputStream;
	}
}