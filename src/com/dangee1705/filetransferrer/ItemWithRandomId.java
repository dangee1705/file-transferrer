package com.dangee1705.filetransferrer;

import java.util.Random;

public class ItemWithRandomId<T> {
	private static Random random = new Random();
	private T item;
	private long id;

	public ItemWithRandomId(T item, long id) {
		this.item = item;
		this.id = id;
	}

	public ItemWithRandomId(T item) {
		this(item, random.nextLong());
	}

	public T getItem() {
		return item;
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ItemWithRandomId) && ((ItemWithRandomId<T>) obj).getId() == id;
	}
}
