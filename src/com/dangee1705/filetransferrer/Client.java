package com.dangee1705.filetransferrer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;

public class Client {
	private ArrayList<ServerHandler> serverHandlers = new ArrayList<>();
	private ArrayList<AvailableDownload> availableDownloads = new ArrayList<>();

	public void connectTo(InetAddress inetAddress) {
		synchronized(serverHandlers) {
			for(ServerHandler serverHandler : serverHandlers)
				if(serverHandler.getInetAddress().equals(inetAddress))
					return;
			serverHandlers.add(new ServerHandler(inetAddress));
		}
	}

	public void scanNetwork() {
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
							if(!interfaceAddress.getAddress().equals(newInetAddress)) {
								connectTo(newInetAddress);
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
		private DataInputStream dataInputStream;
		private DataOutputStream dataOutputStream;

		public ServerHandler(InetAddress inetAddress) {
			this.inetAddress = inetAddress;
			thread = new Thread(this, "Server-Handler-" + inetAddress.getHostAddress());
			thread.start();
		}

		private String readString() throws IOException {
			int length = dataInputStream.readInt();
			byte[] bytes = dataInputStream.readNBytes(length);
			return new String(bytes);
		}

		private void readFile() throws IOException {
			String name = readString();
			boolean isDirectory = dataInputStream.readBoolean();
			File file = new File(System.getProperty("user.home") + "/Downloads/" + name);
			if(isDirectory) {
				file.mkdirs();
				int fileCount = dataInputStream.readInt();
				for(int i = 0; i < fileCount; i++)
					readFile();
			} else {
				long fileLength = dataInputStream.readLong();
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				byte[] buffer = new byte[8192];
				long count = 0;
				while(count < fileLength) {
					int last_count = dataInputStream.read(buffer, 0, (int) (fileLength - count < 8192 ? fileLength - count : 8192));
					count += last_count;
					fileOutputStream.write(buffer, 0, last_count);
				}
				fileOutputStream.close();
			}
		}

		@Override
		public void run() {
			try {
				Socket socket = new Socket(inetAddress, FileTransferrer.PORT);
				dataInputStream = new DataInputStream(socket.getInputStream());
				dataOutputStream = new DataOutputStream(socket.getOutputStream());

				while(true) {
					int messageType = dataInputStream.readInt();
					if(messageType == 0) {
						int numFiles = dataInputStream.readInt();
						for(int i = 0; i < numFiles; i++) {
							long fileId = dataInputStream.readLong();
							String name = readString();
							synchronized(availableDownloads) {
								AvailableDownload download = new AvailableDownload(this, name, fileId);
								if(!availableDownloads.contains(download))
									availableDownloads.add(download);
							}
						}

						onFileAddedListeners.forEach(listener -> listener.on());
					} else if(messageType == 2) {
						long fileId = dataInputStream.readLong();
						boolean isAvailable = dataInputStream.readBoolean();
						if(isAvailable) {
							readFile();
						}
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

		public InetAddress getInetAddress() {
			return inetAddress;
		}

		public void download(long id) throws IOException {
			dataOutputStream.writeInt(0);
			dataOutputStream.writeLong(id);
			dataOutputStream.flush();
		}
	}

	ArrayList<Listener> onFileAddedListeners = new ArrayList<>();

	public void addOnFileAddedListener(Listener listener) {
		onFileAddedListeners.add(listener);
	}

	public ArrayList<AvailableDownload> getAvailableDownloads() {
		return availableDownloads;
	}

	public void download(long id) {
		for(AvailableDownload download : getAvailableDownloads()) {
			if(download.getId() == id) {
				try {
					download.getServerHandler().download(download.getId());
				} catch (IOException e) {
					// TODO: handle this error
				}
			}
		}
	}

	// onDownloadUnavailable
	// onDownloadCompleted
}
