package com.dangee1705.filetransferrer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CounterInputStream extends FilterInputStream implements Counter {
	private long counter = 0;

	public CounterInputStream(InputStream in) {
		super(in);
	}
	
	@Override
	public int read() throws IOException {
		counter++;
		return super.read();
	}

	@Override
	public long getCounter() {
		return counter;
	}

	@Override
	public void resetCounter() {
		counter = 0;
	}
}
