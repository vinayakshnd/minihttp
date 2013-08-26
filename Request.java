package com.gslab.bootcamp.minihttp;

import java.util.HashMap;

/*
 * This class is used to store the incoming request in parsed format
 */

public class Request {

	HashMap<String, String> headers;
	char[] fileInChar;
	byte[] fileInByte;
	boolean isEmpty;

	public Request() {

		headers = new HashMap<String, String>();
		fileInChar = null;
		fileInByte = null;
		isEmpty = true;
	}

	public String get(String key) {
		return headers.get(key);
	}

	public String set(String key, String value) {
		return headers.put(key, value);
	}

	public char[] getBodyChar() {
		return fileInChar;
	}

	public boolean isEmpty() {
		return isEmpty;
	}

	public void setEmpty(boolean value) {
		isEmpty = value;
	}

	public void setBodyChar(char[] fileInChar) {
		this.fileInChar = new char[fileInChar.length];
		System.arraycopy(fileInChar, 0, this.fileInChar, 0, fileInChar.length);
	}

	public byte[] getBodyByte() {
		return fileInByte;
	}

	public void setBodyByte(byte[] fileInByte) {
		this.fileInByte = new byte[fileInByte.length];
		System.arraycopy(fileInByte, 0, this.fileInByte, 0, fileInByte.length);
	}

	public String toString() {
		String result = null;
		for (String key : headers.keySet()) {
			String value = headers.get(key);
			result += key + ":" + value + "\n";
		}
		return result;
	}

}
