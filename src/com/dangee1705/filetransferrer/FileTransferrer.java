package com.dangee1705.filetransferrer;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

// look for other servers on network
// download file list
// display file list

// user can add files
// send list to clients

// select files to download
// download files

public class FileTransferrer {
	public static final int PORT = 10000;
	private Server server;
	private Client client;

	public FileTransferrer() {
		server = new Server();
		client = new Client();

		JFrame window = new JFrame("File Transferrer");
		window.setSize(400, 400);

		// server side
		ArrayList<File> files = new ArrayList<>();
		DefaultListModel<File> defaultListModel = new DefaultListModel<>();

		JButton addFilesButton = new JButton("Add Files");
		addFilesButton.addActionListener(event -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setApproveButtonText("Add");
			fileChooser.setDialogTitle("Add Files");

			int retval = fileChooser.showOpenDialog(window);
			if(retval == JFileChooser.APPROVE_OPTION) {
				File[] selectedFiles = fileChooser.getSelectedFiles();
				for(File selectedFile : selectedFiles) {
					defaultListModel.addElement(selectedFile);
					server.addFile(selectedFile);
				}
			}
		});
		window.add(addFilesButton, BorderLayout.PAGE_START);

		JScrollPane fileListScrollPane = new JScrollPane();
		JList<File> fileList = new JList<>(defaultListModel);
		fileList.addListSelectionListener(event -> {
			System.out.println("selection changed");
		});
		fileListScrollPane.setViewportView(fileList);
		window.add(fileListScrollPane, BorderLayout.LINE_START);

		// client side
		JScrollPane clientFileListScrollPane = new JScrollPane();
		DefaultListModel<String> clientFileListModel = new DefaultListModel<>();
		JList<String> clientFileList = new JList<>(clientFileListModel);
		clientFileList.addListSelectionListener(event -> {
			System.out.println("selection changed");
		});
		clientFileListScrollPane.setViewportView(clientFileList);
		window.add(clientFileListScrollPane, BorderLayout.LINE_END);
		client.addOnFileAddedListener(() -> {
			clientFileListModel.clear();
			for(ItemWithRandomId<String> item : client.getAvailableDownloads())
				clientFileListModel.addElement(item.getItem());
		});
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);

		
	}

	private int listFiles(File root) {
		System.out.println(root.getAbsolutePath() + " " + (root.isDirectory() ? ("D " + root.listFiles().length) : ("F "  + root.length())));
		int total = 1;
		if(root.isDirectory())
			for(File file : root.listFiles())
				total += listFiles(file);
		return total;
	}

	private long calculateSize(File root) {
		if(root.isDirectory()) {
			long total = 0;
			for(File file : root.listFiles())
				total += calculateSize(file);
			return total;
		} else {
			return root.length();
		}
	}
	
	private String byteSizeString(long bytes) {
		String[] suffixes = {"", "kibi", "mebi", "gibi", "tebi", "pebi"};
		int i = 0;
		while(bytes > 1023) {
			i++;
			bytes /= 1024;
		}
		return bytes + " " + suffixes[i] + "bytes";
	}

	public static void main(String[] args) {
		new FileTransferrer();
	}
}
