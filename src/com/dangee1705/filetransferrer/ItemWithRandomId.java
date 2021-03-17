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
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if(obj instanceof ItemWithRandomId<?>)
			return ((ItemWithRandomId<T>) obj).getId() == id;
		return false;
	}

	@Override
	public String toString() {
		return item.toString();
	}
}
