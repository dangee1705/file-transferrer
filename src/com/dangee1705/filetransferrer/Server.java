package com.dangee1705.filetransferrer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {
	private Thread thread;
	private ServerSocket serverSocket;
	private ArrayList<ItemWithRandomId<File>> fileWithRandomIds;
	private ArrayList<ClientHandler> clientHandlers;

	public Server() {
		thread = new Thread(this, "Server");
		thread.start();
	}

	private class ClientHandler implements Runnable {
		private Socket socket;
		private DataInputStream dataInputStream;
		private DataOutputStream dataOutputStream;

		public ClientHandler(Socket socket) {
			this.socket = socket;
			try {
				dataInputStream = new DataInputStream(socket.getInputStream());
				dataOutputStream = new DataOutputStream(socket.getOutputStream());
			} catch(IOException e) {
				cleanup();
				return;
			}
			
		}

		private void cleanup() {
			clientHandlers.remove(this);
		}

		@Override
		public void run() {
			while(true) {
				try {
					int messageType = dataInputStream.readInt();
					System.out.println(messageType);
				} catch(IOException e) {
					break;
				}
			}
			cleanup();
		}

		public void sendFileList() throws IOException {
			dataOutputStream.writeInt(0);
			dataOutputStream.writeInt(fileWithRandomIds.size());
			for(ItemWithRandomId<File> fileWithRandomId : fileWithRandomIds) {
				dataOutputStream.writeLong(fileWithRandomId.getId());
				byte[] nameBytes = fileWithRandomId.getItem().getName().getBytes();
				dataOutputStream.writeInt(nameBytes.length);
				dataOutputStream.write(nameBytes);
			}
			dataOutputStream.flush();
		}
	}

	@Override
	public void run() {
		fileWithRandomIds = new ArrayList<>();

		try {
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(FileTransferrer.PORT));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		clientHandlers = new ArrayList<>();
		while(true) {
			try {
				clientHandlers.add(new ClientHandler(serverSocket.accept()));
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void addFile(File file) {
		fileWithRandomIds.add(new ItemWithRandomId<>(file));
		for(ClientHandler clientHandler : clientHandlers) {
			try {
				clientHandler.sendFileList();
			} catch (IOException e) {
				
			}
		}
	}
}
