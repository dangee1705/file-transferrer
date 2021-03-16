package com.dangee1705.filetransferrer;

import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;

public class Client {
	private ArrayList<ServerHandler> serverHandlers;
	private ArrayList<ItemWithRandomId<String>> availableDownloads = new ArrayList<>();

	public Client() {
		serverHandlers = new ArrayList<>();
		
		try {
			Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
			while(enumeration.hasMoreElements()) {
				NetworkInterface networkInterface = enumeration.nextElement();
				for(InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					if(networkInterface.isUp() && !networkInterface.isLoopback() && !interfaceAddress.getAddress().isLinkLocalAddress()) {
						byte[] addr = interfaceAddress.getAddress().getAddress();
						for(int i = 0; i < 256; i++) {
							addr[addr.length - 1] = (byte) i;
							InetAddress newInetAddress = InetAddress.getByAddress(addr);
							synchronized(serverHandlers) {
								serverHandlers.add(new ServerHandler(newInetAddress));
							}
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public class ServerHandler implements Runnable {
		private InetAddress inetAddress;
		private Thread thread;

		public ServerHandler(InetAddress inetAddress) {
			this.inetAddress = inetAddress;
			thread = new Thread(this, "Server-Handler-" + inetAddress);
			thread.start();
		}

		@Override
		public void run() {
			try {
				Socket socket = new Socket(inetAddress, FileTransferrer.PORT);
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				while(true) {
					int messageType = dataInputStream.readInt();
					if(messageType == 0) {
						int numFiles = dataInputStream.readInt();
						for(int i = 0; i < numFiles; i++) {
							long fileId = dataInputStream.readLong();
							int nameBytesLength = dataInputStream.readInt();
							byte[] nameBytes = dataInputStream.readNBytes(nameBytesLength);
							String name = new String(nameBytes);
							synchronized(availableDownloads) {
								ItemWithRandomId<String> download = new ItemWithRandomId<String>(inetAddress.getHostAddress() + " > " + name, fileId);
								if(!availableDownloads.contains(download))
									availableDownloads.add(download);
							}
						}

						for(Listener listener : onFileAddedListeners)
							listener.on();
					}
				}


				// socket.close();
			} catch (Exception e) {
				
			}
			cleanup();
		}

		public void cleanup() {
			synchronized(serverHandlers) {
				serverHandlers.remove(this);
			}
		}
	}

	ArrayList<Listener> onFileAddedListeners = new ArrayList<>();

	public void addOnFileAddedListener(Listener listener) {
		onFileAddedListeners.add(listener);
	}

	public ArrayList<ItemWithRandomId<String>> getAvailableDownloads() {
		return availableDownloads;
	}
}
