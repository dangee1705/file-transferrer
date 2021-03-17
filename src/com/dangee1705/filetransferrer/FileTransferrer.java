package com.dangee1705.filetransferrer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

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

	private JButton downloadButton;

	public FileTransferrer() {
		server = new Server();
		client = new Client();

		JFrame window = new JFrame("File Transferrer");
		window.setSize(600, 600);

		JPanel wrapperPanel = new JPanel();
		wrapperPanel.setLayout(new GridLayout(1, 2));
		JPanel serverPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(serverPanel);

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
		serverPanel.add(addFilesButton, BorderLayout.PAGE_START);

		JScrollPane fileListScrollPane = new JScrollPane();
		JList<File> fileList = new JList<>(defaultListModel);
		fileList.addListSelectionListener(event -> {
			System.out.println("selection changed");
		});
		fileListScrollPane.setViewportView(fileList);
		serverPanel.add(fileListScrollPane, BorderLayout.CENTER);

		// client side
		JPanel clientPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(clientPanel);

		JScrollPane clientFileListScrollPane = new JScrollPane();
		DefaultListModel<String> clientFileListModel = new DefaultListModel<>();
		JList<String> clientFileList = new JList<>(clientFileListModel);
		clientFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		clientFileList.addListSelectionListener(event -> downloadButton.setEnabled(clientFileList.getSelectedIndices().length > 0));
		clientFileListScrollPane.setViewportView(clientFileList);
		clientPanel.add(clientFileListScrollPane, BorderLayout.CENTER);

		downloadButton = new JButton("Download file");
		downloadButton.setEnabled(false);
		clientPanel.add(downloadButton, BorderLayout.PAGE_END);
		
		window.add(wrapperPanel, BorderLayout.CENTER);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setLocationRelativeTo(null);
		window.setVisible(true);

		client.addOnFileAddedListener(() -> {
			clientFileListModel.clear();
			for(ItemWithRandomId<String> item : client.getAvailableDownloads())
				clientFileListModel.addElement(item.getItem());
		});
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
