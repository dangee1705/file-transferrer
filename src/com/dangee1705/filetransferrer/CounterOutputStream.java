package com.dangee1705.filetransferrer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CounterOutputStream extends FilterOutputStream implements Counter {
	private long counter = 0;
	
	public CounterOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int b) throws IOException {
		super.write(b);
		counter++;
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
