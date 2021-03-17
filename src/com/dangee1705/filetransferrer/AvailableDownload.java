package com.dangee1705.filetransferrer;

import com.dangee1705.filetransferrer.Client.ServerHandler;

public class AvailableDownload {
	private ServerHandler serverHandler;
	private String name;
	private long id;

	public AvailableDownload(ServerHandler serverHandler, String name, long id) {
		this.serverHandler = serverHandler;
		this.name = name;
		this.id = id;
	}

	public ServerHandler getServerHandler() {
		return serverHandler;
	}

	public String getName() {
		return name;
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return serverHandler.getInetAddress().getHostAddress() + " " + name;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof AvailableDownload)
			return ((AvailableDownload) obj).getId() == id;
		return false;
	}
}
