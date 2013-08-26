package com.gslab.bootcamp.minihttp;

import java.io.IOException;

public class WebserverConsumer {

	public static void main(String args[]) throws IOException {

		System.out.println("Initializing  web server...");

		WebServer server = new WebServer(3000, "C:\\Users\\vinayak\\workspace\\WebServer\\Server_root_dir");

		if (server.isCreated()) server.start();
		
		
		
	}
}
