package com.gslab.bootcamp.minihttp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class WebUtil {
	public static String DateFormat = " EEE, dd MMM yyyy HH:mm:ss z";
	public static String DefaultFileName = "index.html";
	public static Properties prop;
	public static String Connection_Timeout = "Connection_timeout";
	public static String Backlog = "Backlog";
	public static String Root_dir = "Root_dir";

	static // executes only when class is loaded
	{
		prop = new Properties();
		try {

			String path = System.getProperty("user.dir") + "\\config.txt";

			prop.load(new FileInputStream(path));

			// trim all the spaces
		}
		catch (FileNotFoundException e) {

			System.out.println("No user defined config file......");
			System.out.println("Loading default configurations....");
		}
		catch (NullPointerException e) {
			System.out.println("Loading default configurations....");
		}
		catch (IOException e) {
			System.out.println("IOException");
		}

		catch (ExceptionInInitializerError e) {
			System.out.println("ExceptionInInitializerError ");
		}

		if (! prop.containsKey(WebUtil.Connection_Timeout)) {
			System.out.println("No Connection_timeout found in config file");
			prop.setProperty("Connection_timeout", "5000");
		}
		if (! prop.containsKey(WebUtil.Backlog)) {
			System.out.println("No Backlog found in config file");
			prop.setProperty(WebUtil.Backlog, "10");
		}

		System.out.println(prop);
	}

	public static String getLastModifiedDate(File file) {

		DateFormat dateFormat = new SimpleDateFormat(WebUtil.DateFormat);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format((new Date(file.lastModified())));
	}

	/*****************************************************************************************/

	public static String getCurrentDate() {

		DateFormat dateFormat = new SimpleDateFormat(WebUtil.DateFormat);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format((new Date()));
	}
	
	public static String calculateEtag(File file) throws IOException 
	{ 
		/*
		 * Last modified time can not be used since it might happen that 
		 * two different files are modified at the same time concurrently
		 */

		byte[] plainText;
		int file_len = (int) file.length();
		plainText = new byte[file_len];

		InputStream inputFromFile;
		inputFromFile = new FileInputStream(file);
		inputFromFile.read(plainText, 0, file_len);
		inputFromFile.close();

		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("MD5");
		
		}
		catch (Exception e) {
			System.err.println(e.toString());
		}
		md.reset();
		md.update(plainText);
		byte[] msgDigest = md.digest();

		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < msgDigest.length; i++) {
			if ((msgDigest[i] & 0xff) < 0x10) {
				sb.append("0");
			}
			sb.append(Long.toString(msgDigest[i] & 0xff, 16));
		}
		return new String(sb);
	}
}
