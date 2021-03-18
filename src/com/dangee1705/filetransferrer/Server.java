package com.dangee1705.filetransferrer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

	public class ClientHandler implements Runnable {
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

			Thread thread = new Thread(this, "Client-Handler-" + socket.getInetAddress().getHostAddress());
			thread.start();
		}

		private void cleanup() {
			clientHandlers.remove(this);
		}

		@Override
		public void run() {
			try {
				sendFileList();
			} catch (IOException e1) {
				
			}
			while(true) {
				try {
					int messageType = dataInputStream.readInt();
					if(messageType == 0) {
						long fileId = dataInputStream.readLong();

						synchronized(dataOutputStream) {
							dataOutputStream.writeInt(2);
							dataOutputStream.writeLong(fileId);
							ItemWithRandomId<File> fileWithRandomId = null;
							for(ItemWithRandomId<File> item : fileWithRandomIds) {
								if(item.getId() == fileId) {
									fileWithRandomId = item;
									break;
								}
							}
							if(fileWithRandomId == null) {
								dataOutputStream.writeBoolean(false);
							} else {
								dataOutputStream.writeBoolean(true);
								sendFile(fileWithRandomId.getItem(), fileWithRandomId.getItem());
							}
						}
					}
				} catch(IOException e) {
					break;
				}
			}
			cleanup();
		}

		private void sendString(String string) throws IOException {
			byte[] stringBytes = string.getBytes();
			dataOutputStream.writeInt(stringBytes.length);
			dataOutputStream.write(stringBytes);
		}

		public void sendFileList() throws IOException {
			dataOutputStream.writeInt(0);
			dataOutputStream.writeInt(fileWithRandomIds.size());
			for(ItemWithRandomId<File> fileWithRandomId : fileWithRandomIds) {
				dataOutputStream.writeLong(fileWithRandomId.getId());
				sendString(fileWithRandomId.getItem().getName());
			}
			dataOutputStream.flush();
		}

		private void sendFile(File root, File file) throws IOException {
			if(file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
				String name = file.getAbsolutePath().substring(root.getAbsolutePath().length() - root.getName().length());
				sendString(name);
				dataOutputStream.writeBoolean(file.isDirectory());
				if(file.isDirectory()) {
					dataOutputStream.writeInt(file.listFiles().length);
					for(File subfile : file.listFiles()) {
						sendFile(root, subfile);
					}
				} else {
					dataOutputStream.writeLong(file.length());
					FileInputStream fileInputStream = new FileInputStream(file);
					byte[] buffer = new byte[8192];
					int count = 0;
					while((count = fileInputStream.read(buffer, 0, 8192)) != -1) {
						dataOutputStream.write(buffer, 0, count);
					}
					fileInputStream.close();
				}
			}
		}

		public Socket getSocket() {
			return socket;
		}
	}

	private ArrayList<Listener> onClientConnectedListeners = new ArrayList<>();

	public void addOnClientConnectListener(Listener listener) {
		onClientConnectedListeners.add(listener);
	}

	public ArrayList<ClientHandler> getClientHandlers() {
		return clientHandlers;
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
				onClientConnectedListeners.forEach(listener -> listener.on());
				System.out.println("got " + clientHandlers.size() + " connection(s)");
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
