package com.gslab.bootcamp.minihttp;

import java.io.*;
import java.net.*;

public class ClientThread implements Runnable {

	private Socket socket;
	private DataOutputStream socketOut;

	private Reader reader;
	private String IP_address;
	private String port;
	private String serverRootDir;
	private WebServer parentServer;

	public ClientThread(Socket socket, String IP_address, String port, String serverRootDir, WebServer parentWebServer) {

		this.socket = socket;
		this.IP_address = IP_address;
		this.port = port;
		this.serverRootDir = serverRootDir;

		reader = new Reader(socket);

		
		parentServer = parentWebServer;

		int timeout;
		try {
			timeout = Integer.parseInt(WebUtil.prop.getProperty(WebUtil.Connection_Timeout));
			socket.setSoTimeout(timeout);
			socketOut = new DataOutputStream(socket.getOutputStream());

		}
		catch (SocketException e) {

			e.printStackTrace();
		}
		catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void run() {

		Request request;

		byte[] responce;

		try {

			while (! parentServer.isStopped()) {
				request = reader.read();

				if (! request.isEmpty()) {
					System.out.println(request);
					responce = RequestProcessor.processRequest(request, IP_address, port, serverRootDir);
					sendResponce(responce, socketOut);
					if (request.get("Connection") != null && request.get("Connection").equalsIgnoreCase("Closed")) { throw new SocketTimeoutException(); }

				}
			}
		}
		catch (SocketTimeoutException e) {
			try {
				System.out.println("Closing the client socket");
				socket.close();
			}
			catch (IOException e1) {

				System.out.println("Error in closeing the client socket");
				e.printStackTrace();
			}

		}
		catch (SocketException e) {
			System.out.println("Connection terminated by client");
		}
	}

	private void sendResponce(byte[] responce, DataOutputStream oSocket) {

		try {

			oSocket.write(responce);
			oSocket.flush();

		}
		catch (IOException e) {

			System.out.println("connection closed by client");
		}
		catch (NullPointerException e) {

			System.out.println("Null responce");
		}
	}
}
