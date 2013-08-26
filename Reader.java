package com.gslab.bootcamp.minihttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.StringTokenizer;

public class Reader {

	Socket socket;
	Request request;

	public Reader(Socket socket) {
		this.socket = socket;
		request = new Request();
	}

	public Request read() throws SocketTimeoutException, SocketException {

		String line;
		StringTokenizer tokenizer;
		char fileInChar[];
		byte fileInByte[];

		String field, value;
		int fieldLength;

		BufferedReader inBuffer;

		try {

			InputStream is = socket.getInputStream();
			inBuffer = new BufferedReader(new InputStreamReader(is));

			line = inBuffer.readLine();

			if (line == null) {
				request.setEmpty(true);
				return request;
			}
			else {
				tokenizer = new StringTokenizer(line);

				/* Parsing of the initial line based upon the
				* http standard.
				*/
				
				request.set("Method", tokenizer.nextToken().trim());
				request.set("URI", tokenizer.nextToken().trim());
				request.set("Version", tokenizer.nextToken().trim());

				/*
				 * Read the headers line by line , split it into (key,value) pair and store them in request Loop executes till blank line is read
				 */
				while (! (line = inBuffer.readLine()).equalsIgnoreCase("")) {

					tokenizer = new StringTokenizer(line, ":\r\n");
					if (tokenizer.hasMoreTokens()) {
						field = tokenizer.nextToken();
						fieldLength = field.length();
						value = line.substring(fieldLength + 1);
						value.trim();
						request.set(field, value);

					}

				}

				/*
				 * Presence of content-Length header indicates that the request also contains the entity in the body.
				 */

				if (request.get("Content-Length") != null) {
					int contentLength = Integer.parseInt(request.headers.get("Content-Length").trim());

					if (request.get("Content-type") == null) {
						request.set("Content-type", getContentType(request.headers.get("URI")));
					}

					if (request.get("Content-type").equalsIgnoreCase("text/plain") || request.get("Content-type").equalsIgnoreCase("text/html")
							|| request.get("Content-type").equalsIgnoreCase("text/xml")) {
						//for text files
						fileInChar = new char[contentLength];
						inBuffer.read(fileInChar);
						request.setBodyChar(fileInChar);

					}
					else // for binary files 
					{
						fileInByte = new byte[contentLength];
						is.read(fileInByte, 0, contentLength);
						request.setBodyByte(fileInByte);
					}
				}
				request.setEmpty(false);
				return request;
			}
		}
		catch (NullPointerException e) {

		}
		catch (SocketTimeoutException e) {
			throw e;
		}
		catch (SocketException e) {
			throw e;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return request;
	}

	public String getContentType(String filename) {
		String contentType = "";

		if (filename.endsWith("html") || filename.endsWith("htm")) {
			contentType = "text/html";
		}
		else if (filename.endsWith("txt")) {
			contentType = "text/plain";
		}
		else if (filename.endsWith("xml")) {
			contentType = "text/xml";
		}
		else if (filename.endsWith("rtf")) {
			contentType = "text/richtext";
		}

		else if (filename.endsWith("jpg")) {
			contentType = "image/jpg";
		}
		else if (filename.endsWith("gif")) {
			contentType = "image/gif";
		}
		else if (filename.endsWith("png")) {
			contentType = "image/png";

		}
		else if (filename.endsWith("mp4")) {
			contentType = "video/mp4";
		}
		else if (filename.endsWith("3gpp")) {
			contentType = "video/3gpp";
		}
		else if (filename.endsWith("mpeg")) {
			contentType = "video/mpeg";
		}
		else if (filename.endsWith("oog")) {
			contentType = "video/oog";

		}
		else if (filename.endsWith(".pdf")) {
			contentType = "application/pdf";
		}

		return contentType;
	}
}
