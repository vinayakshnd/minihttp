package com.gslab.bootcamp.minihttp;

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.*;

public class WebServer implements Runnable {

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private boolean created;
	private ExecutorService executor;
	private String IP_address;
	private String port;
	private String serverRootDir;
	private Thread mainThread;
	private boolean stopped;

	public WebServer(int port, String serverRootDir) {

		stopped = false;
		int backlog;
		created = false;
		executor = Executors.newFixedThreadPool(20);
		this.serverRootDir = serverRootDir;

		try {

			this.IP_address = Inet4Address.getLocalHost().getHostAddress();
			this.port = "" + port; 

			backlog = Integer.parseInt(WebUtil.prop.getProperty(WebUtil.Backlog));

			InetAddress addr = InetAddress.getByName(IP_address);

			serverSocket = new ServerSocket(port, backlog, addr);

			created = true;

		}
		catch (BindException e) {

			System.out.println("Address already in use.");
			System.out.println("Try with different port");

		}
		catch (IOException e) {
			System.out.println("Error in loading Config file");

		}
		catch (Exception e) {

		}

	}

	public boolean isCreated() {
		return created;
	}

	public void run() {
		System.out.println("Starting Web server.....");

		while (! stopped) {
			try {
				clientSocket = serverSocket.accept();
				ClientThread clientThread = new ClientThread(clientSocket, IP_address, port, serverRootDir, this);
				executor.execute(clientThread);
			}
			catch (IOException e) {
				System.out.println("Error in accepting the new connection");
			}

		}

	}

	public void stop() {
		System.out.println("shutting down the server");
		stopped = true;
	}

	public void start() {
		mainThread = new Thread(this);
		mainThread.start();

	}

	public boolean isStopped() {
		return stopped;
	}

}
