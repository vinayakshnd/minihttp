package com.gslab.bootcamp.minihttp;

import java.io.*;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.StringTokenizer;

/*
 *  RequestProcessor is responsible for processing the Request object 
 *  and to produce response in byte array
 */


public class RequestProcessor {

	private static String CRLF="\r\n";

	public RequestProcessor() {

			}

	
        public static byte[] processRequest(Request request, String IP_address, String port, String serverRootDir) {
	
        	byte[] responce = null;
		String responce_header;

		try {

			if (request.get("Method").compareTo("GET") == 0 || request.get("Method").compareTo("HEAD") == 0) {
				responce = processGET_HEADrequest(request, IP_address, port, serverRootDir);
			}
			else if (request.get("Method").compareTo("PUT") == 0) {
				responce = processPUTrequest(request, serverRootDir);
			}
			else {
				responce_header = getResponceHeaders(501) + CRLF + "<html><body>Http Method not implemented</body></html>";
				System.out.println(responce_header);
				responce = responce_header.getBytes();
			}
		}
		catch (FileNotFoundException e) {
			responce_header = getResponceHeaders(404) + CRLF + "<html><body>File not found</body></html>";
			System.out.println(responce_header);
			responce = responce_header.getBytes();
		}
		catch (Exception e) {
			responce_header = getResponceHeaders(500) + CRLF + "<html><body>Internal Server Error</body></html>";
			System.out.println(responce_header);
			responce = responce_header.getBytes();
		}
		return responce;
	}

	private static byte[] processGET_HEADrequest(Request request, String IP_address, String port, String serverRootDir) throws FileNotFoundException,
			IOException, ParseException {
		
		byte[] responce;
		String responce_header;

		/*
		 * Host is compulsory in request
		 * If Host header absent , give response as 400 Bad Request
		 */
		
		if (request.get("Host") == null && request.get("Version").equalsIgnoreCase("HTTP/1.1")) {
			responce_header = getResponceHeaders(400) 
					+ "Content-type:text/html" + CRLF 
					+ "<html><body><h2>No Host: request received</h2>"
					+ "HTTP 1.1 requests must include the Host: request."
					+ "</body></html>";
			
			System.out.println(responce_header);
			return responce_header.getBytes();
		}

		/*
		 * If both If-Match and If-None-Match are present in same request
		 * then server behavior is unspecified in RFC 2616
		 * 
		 */
		
		if (request.get("If-None-Match") != null) 
		{
			responce = processIf_None_Match(request, IP_address, port, serverRootDir);
			if (responce != null) return responce;
		}

		if (request.get("If-Match") != null) 
		{
			responce = processIf_Match(request, IP_address, port, serverRootDir);
			if (responce != null) return responce;
		}
		
		
		if (request.get("If-Modified-Since") != null) 
		{
			responce = processIf_Modified_Since(request, serverRootDir);
			if (responce != null) return responce;

		}
		if (request.get("If-Unmodified-Since") != null) 
		{
			responce = processIf_Unmodified_Since(request, serverRootDir);
			if (responce != null) return responce;

		}

		 /* if entity is unchanged(by comparing Etags) , send me parts
		*    that i am missing , otherwise send me entire new entity.
		*/										 
		if (request.get("If-Range") != null)
		{

			responce = processIf_Range(request, IP_address, port, serverRootDir);
			if (responce != null) return responce;
		}

		if (request.get("Range") != null) 
		{

			responce = processRange(request, IP_address, port, serverRootDir);
			if (responce != null) return responce;
		}

		responce = processOK(request, IP_address, port, serverRootDir);
		return responce;
	}

	/*
	 *  client says:if none of the tags matches(Whatever have is not matching) then only send me updated one
	 */

	private static byte[] processIf_None_Match(Request request, String IP_address, String port, String serverRootDir) throws IOException {
		
		String value = request.get("If-None-Match").trim();
		String responce_header;

		StringTokenizer tokenizer = new StringTokenizer(value, ",");

		String[] tags = new String[tokenizer.countTokens()];
		int tag_count = 0;

		while (tokenizer.hasMoreTokens()) {
			tags[tag_count] = tokenizer.nextToken();
			System.out.println(tags[tag_count]);
			tag_count++;
		}
		
		File file;
		int flag = 0;

		String path = getFilePathFromRequest(request, serverRootDir);
		file = new File(path);

		String EtagOfFile = WebUtil.calculateEtag(file);

		for (int counter = 0; counter < tag_count; counter++) {
			if (EtagOfFile.equalsIgnoreCase(tags[counter])) {
				flag = 1;
				
				/*
				 *  At least One tag matches with the file tag , 
				 *  so send Not modified
				 */
				break; 
			}
		}
		if (flag == 1) {
			/*
			 *  Send 304 Not Modified
			 */
			responce_header = getResponceHeaders(304) + CRLF;
			System.out.println(responce_header);
			return responce_header.getBytes();
		}
		else {
			return processOK(request, IP_address, port, serverRootDir); // Other wise send Modifed fresh entity

		}

	}

	private static byte[] processIf_Match(Request request, String IP_address, String port, String serverRootDir) throws IOException {
		
		String value = request.get("If-Match");
		String responce_header;

		StringTokenizer tokenizer = new StringTokenizer(value, ",");

		String[] tags = new String[tokenizer.countTokens()];
		int tag_count = 0;

		while (tokenizer.hasMoreTokens()) // separate the tags in the request
		{
			tags[tag_count] = tokenizer.nextToken();
			System.out.println(tags[tag_count]);
			tag_count++;
		}
		int flag = 0;
		File file;

		String path = getFilePathFromRequest(request, serverRootDir);
		file = new File(path);
		String EtagOfFile = WebUtil.calculateEtag(file);

		for (int counter = 0; counter < tag_count; counter++) {
			if (EtagOfFile.equalsIgnoreCase(tags[counter])) {
				flag = 1;
				/* 
				 *  At least One tag matches with the file tag , so send Not modified
				 */
				break;
			}
		}

		/*
		 * If none of the tag matches ,so send 412 Precondition Failed
		 */
		if (flag == 0)  
		{

			responce_header = getResponceHeaders(412) + CRLF;

			System.out.println(responce_header);

			return responce_header.getBytes();
		}
		/*
		 * Otherwise send the normal response
		 */
		else {
			return processOK(request, IP_address, port, serverRootDir);
		}
	}

	private static byte[] processIf_Modified_Since(Request request, String serverRootDir) {
		
		Date ifModifiedSinceDate, FilelastModifiedDate;
		DateFormat dateFormat = new SimpleDateFormat(WebUtil.DateFormat);
		File file;
		String path = getFilePathFromRequest(request, serverRootDir);
		file = new File(path);
		String responce_header;


		try {
			ifModifiedSinceDate = dateFormat.parse(request.get("If-Modified-Since"));

			ifModifiedSinceDate.getTime();

			FilelastModifiedDate = dateFormat.parse(WebUtil.getLastModifiedDate(file));

			FilelastModifiedDate.getTime();

			if (FilelastModifiedDate.compareTo(ifModifiedSinceDate) == 0 || FilelastModifiedDate.before(ifModifiedSinceDate)) {

				/*
				 * Send 304 Not Modified
				 * 
				 */
				responce_header = getResponceHeaders(304) + CRLF;
				System.out.println(responce_header);
				return responce_header.getBytes();
			}
		}
		catch (ParseException e) {
			/*
			 * If the date specified in the header is not in standard format specified in RFC  
			 * then just ignore the header
			 */
			
		}
		return null;
	}

	private static byte[] processIf_Unmodified_Since(Request request, String serverRootDir) {
	

		Date ifUnmodifiedSinceDate, FilelastModifiedDate;

		DateFormat dateFormat = new SimpleDateFormat(WebUtil.DateFormat);

		File file;
		String path = getFilePathFromRequest(request, serverRootDir);
		file = new File(path);
		String responce_header;

		try {
			ifUnmodifiedSinceDate = dateFormat.parse(request.get("If-Unmodified-Since"));

			FilelastModifiedDate = dateFormat.parse(WebUtil.getLastModifiedDate(file));

			if (FilelastModifiedDate.after(ifUnmodifiedSinceDate)) {

				responce_header = getResponceHeaders(412) + CRLF;

				System.out.println(responce_header);
				return responce_header.getBytes();
			}
		}
		catch (ParseException e) {

			/*
			 * If the date specified in the header is not in standard format specified in RFC  
			 * then just ignore the header
			 */
		}
		return null;

	}

	private static byte[] processIf_Range(Request request, String IP_address, String port, String serverRootDir) throws FileNotFoundException, IOException {

		File file;

		byte[] responce;

		String EtagOfFile, EtagFromRequest;
		String path = getFilePathFromRequest(request, serverRootDir);

		file = new File(path);
		
		System.out.println("Inside If-range");
		EtagFromRequest = request.get("If-Range").trim();
		EtagOfFile = WebUtil.calculateEtag(file);

		if (EtagOfFile.equalsIgnoreCase(EtagFromRequest))
		{
			/*
			 * If-Range header must be accompanied by Range header
			 * Otherwise ignore the If-Range header
			 */
			if (request.get("Range") != null) {
				responce = processRange(request, IP_address, port, serverRootDir);
				return responce;
			}
		}
		else
		/*
		 * if Etag is changed (ie. Entity is modified then send the entire Entity )
		*/
		{
			responce = processOK(request, IP_address, port, serverRootDir);
			return responce;
		}
		return null;
	}

	private static byte[] processOK(Request request, String IP_address, String port, String serverRootDir) throws FileNotFoundException, IOException {

		String responce_header;
		byte[] responce = null;
		byte[] fileInByte;
		File file = null;
		int file_len;
		InputStream inputFromFile;

		String EtagOfFile;

		String path = getFilePathFromRequest(request, serverRootDir);

		if (path != null) file = new File(path);

		/*
		 * If no path is provided (ie. no specific file is requested ) then show the list of all the files present in the server root dir 
		 * Or if folder is requested then show the list of files present in that folder
		 * 
		 */
		if (path == null || file.isDirectory()) 
		{
			responce = processDirectory(request, IP_address, port, serverRootDir);
			return responce;
		}
		else {

			file_len = (int) file.length();
			fileInByte = new byte[file_len];

			inputFromFile = new FileInputStream(file);

			EtagOfFile = WebUtil.calculateEtag(file);

			responce_header = getResponceHeaders(200)
					+ "Content-type:" + get_contentType(path) + CRLF 
					+ "Content-Length:" + file_len + CRLF
					+ "Last-Modified:" + WebUtil.getLastModifiedDate(file) + CRLF
					+ "Etag:" + EtagOfFile + CRLF 
					+ CRLF;

			System.out.println(responce_header + "\n");
			responce = responce_header.getBytes();

			/*
			 * If method is not head ,only then send the responce body
			 */
			if (request.get("Method").compareTo("HEAD") != 0) 
			{

				/*
				 * here ByteArrayOutputStream is used to concatenate the two byte arrays
				 */
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

				inputFromFile.read(fileInByte, 0, file_len);

				outputStream.write(responce_header.getBytes());
				outputStream.write(fileInByte);

				responce = outputStream.toByteArray();
				inputFromFile.close();

			}
		}
		return responce;
	}

	private static byte[] processRange(Request request, String IP_address, String port, String serverRootDir) throws FileNotFoundException, IOException {

		int first_byte[] = new int[10];
		int last_byte[] = new int[10];
		boolean satisfiable[] = new boolean[10];
		boolean syntatical[] = new boolean[10];
		boolean tobeSent[] = new boolean[10];
		int range_count = 0;
		int content_length = 0;
		String responce_header;
		byte[] responce = null;
		byte[] fileInByte;
		RandomAccessFile random_file;
		File file;
		int file_len;

		String path = getFilePathFromRequest(request, serverRootDir);

		random_file = new RandomAccessFile(path, "r");
		file = new File(path);
		file_len = (int) random_file.length();

		String value = request.get("Range");
		String tokens[];
		StringTokenizer tokenizer = new StringTokenizer(value, "=");

		if (tokenizer.nextToken().trim().equalsIgnoreCase("bytes")) {
			while (tokenizer.hasMoreTokens()) {
				value = tokenizer.nextToken();
				tokens = value.split(",");

				for (String i : tokens) {
					if (i.matches("[0-9]+\\-")) {
						first_byte[range_count] = Integer.parseInt(i.substring(0, i.length() - 1));
						last_byte[range_count] = file_len;
					}
					else if (i.matches("[0-9]+\\-[0-9]+")) {
						String[] fl = i.split("-");
						first_byte[range_count] = Integer.parseInt(fl[0]);
						last_byte[range_count] = Integer.parseInt(fl[1]);
					}
					else if (i.matches("\\-[0-9]+")) {
						first_byte[range_count] = 0;
						last_byte[range_count] = Integer.parseInt(i.substring(1, i.length()));
					}
					range_count++;
				}

			}

			int i;
			int flag = 0;

			/*
			 * This loop determines whether the ranges are satisfiable
			 *  and syntactically correct
			 */
			for (i = 0; i < range_count; i++) 
			{
				System.out.println(first_byte[i] + "-" + last_byte[i]);
				if (first_byte[i] < file_len) {
					flag++;
					satisfiable[i] = true;
				}
				if (last_byte[i] >= first_byte[i]) {
					syntatical[i] = true;
				}
			}
			int NoOfCorrectRanges = 0;
			for (i = 0; i < range_count; i++) {
				if (satisfiable[i] && syntatical[i]) {
					tobeSent[i] = true;
					NoOfCorrectRanges++;
				}
			}

			/*
			 * Not a single range satisfying condition 
			 * send either 416 or 200
			 */
			if (NoOfCorrectRanges == 0)
			{

				for (i = 0; i < range_count; i++) {
					if (satisfiable[i]) 
					{
						/*
						 * At least one range satisfiable
						 */
						flag = 1;
						break;
					}
				}
				if (flag == 0) 
				{
					/*
					 * If all the ranges are unsatisfiable 
					 * then send 412 Requested Range Not Satisfiable
					 * 
					 */
					responce_header = getResponceHeaders(416) 
							+ "Last-Modified:" + WebUtil.getLastModifiedDate(file) + CRLF
							+ CRLF;
					
					System.out.println(responce_header);
					random_file.close();
					return responce_header.getBytes();
				}
				else 
				{
					/*
					 * All the specified ranges are syntactically incorrect 
					 * Just ignore them and send normal response
					 */
					responce = processOK(request, IP_address, port, serverRootDir);
					random_file.close();
					return responce;
				}
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			if (NoOfCorrectRanges > 1) {
				responce_header = getResponceHeaders(206)
						+ "Content-type:multipart/byteranges; boundary=THIS_STRING_SEPARATES" + CRLF
						+ "Last-Modified:" + WebUtil.getLastModifiedDate(file) + CRLF;

				System.out.println(responce_header);
				outputStream.write(responce_header.getBytes());
				
				for (i = 0; i < range_count; i++) {
					if (tobeSent[i]) {
						content_length = last_byte[i] - first_byte[i] + 1;
						responce_header = CRLF + "--THIS_STRING_SEPARATES" + CRLF
								+ "Content-Type:" + get_contentType(path) + CRLF
								+ "Content-Range:bytes " + first_byte[i] + " " + last_byte[i] + "/" + file_len + CRLF
								+ CRLF;
						
						System.out.println(responce_header);

						fileInByte = new byte[content_length + 1];
						random_file.seek(first_byte[i]);
						random_file.read(fileInByte, 0, content_length);
						outputStream.write(responce_header.getBytes());
						outputStream.write(fileInByte);

					}
				}
				responce_header = CRLF + "--THIS_STRING_SEPARATES--";
				System.out.println(responce_header);
				outputStream.write(responce_header.getBytes());
				responce = outputStream.toByteArray();

			}
			else if (NoOfCorrectRanges == 1) {
				/*
				 * To determine which range to send
				 */
				for (i = 0; i < range_count; i++) 
				{
					if (tobeSent[i]) 
						break;
				}

				content_length = last_byte[i] - first_byte[i] + 1;

				responce_header = getResponceHeaders(206)
						+ "Content-Type:" + get_contentType(path) + CRLF 
						+ "Content-Range:bytes "+ first_byte[i] + " " + last_byte[i] + "/" + file_len + CRLF
						 + "Content-Length:" + content_length + CRLF 
						 + "Last-Modified:"+ WebUtil.getLastModifiedDate(file) + CRLF
						 + CRLF;
						

				System.out.println(responce_header + "\n");

				fileInByte = new byte[content_length + 10];
				random_file.seek(first_byte[i]);
				random_file.read(fileInByte, 0, content_length);
				random_file.close();

				outputStream.write(responce_header.getBytes());
				outputStream.write(fileInByte);

				responce = outputStream.toByteArray();

			}

		}
		else {
			/*
			 * Malformed  Range header or Range Unit Not supported(ie other than Byte)
			 * 
			 */
			responce = null; 
		}
		return responce;
	}

	private static byte[] processPUTrequest(Request request, String serverRootDir) throws FileNotFoundException, SocketTimeoutException, IOException {

		String path, responce;
		File file;
		byte[] fileInByte;
		char[] fileInChar;
		BufferedOutputStream outToFile;

		path = getFilePathFromRequest(request, serverRootDir);

		System.out.println("Path=" + path);

		file = new File(path);
		outToFile = new BufferedOutputStream(new FileOutputStream(file));

		if (get_contentType(path).equalsIgnoreCase("text/plain") || get_contentType(path).equalsIgnoreCase("text/html")) {

			fileInChar = request.getBodyChar();

			for (int i = 0; i < fileInChar.length; i++) {
				outToFile.write((char) fileInChar[i]);
			}
		}
		else {
			fileInByte = request.getBodyByte();
			outToFile.write(fileInByte);
			outToFile.flush();
		}

		responce = getResponceHeaders(200) 
				+ "Last-Modified:" + WebUtil.getLastModifiedDate(file) + CRLF
				+ CRLF;

		System.out.println(responce + "\n");
		outToFile.close();

		return responce.getBytes();

	}

	/*
	 * If the request is made for the folder then this function
	 * displays the list of files present in the folder
	 * 
	 */
	private static byte[] processDirectory(Request request, String IP_address, String port, String serverRootDir) {

		String path = getFilePathFromRequest(request, serverRootDir);
		File file;
		file = new File(path);

		String[] file_list = file.list();

		StringBuffer responce1;

		String responce_header;
		String server_addr = "http://" + IP_address + ":" + port;
		String Folder_Icons_Path = server_addr + "/folder.jpg";
		String File_Icons_Path = server_addr + "/file.png";

		String intermediate_URI = request.get("URI").substring(1);
		String back_URI;

		responce1 = new StringBuffer("<html><head><title>Mini HTTP Server</title></head><body>");
		responce1.append("<p align=center ><h2>Mini HTTP Server</h2></p>");
		responce1.append("<Table border=0>");
		responce1.append("<TR>");
		responce1.append("<TD width='50'> </TD>");
		responce1.append("<TD><strong>Name</strong></TD>");
		responce1.append("<TD align='center'><strong>Last Modified</strong></TD>");
		responce1.append("<TD align='center' width='50'><strong>Size</strong></TD>");
		responce1.append("</TR>");

		for (int i = 0; i < file_list.length; i++) {

			responce1.append("<TR>");

			if (new File(path + "/" + file_list[i]).isDirectory()) {

				responce1.append("<TD width='50'><img src=\"" + Folder_Icons_Path + "\" height=22 widhth=22></TD>");
				if (! intermediate_URI.equalsIgnoreCase(""))
					responce1.append("<TD><a href='" + server_addr + '/' + intermediate_URI + '/' + file_list[i] + "'>" + file_list[i] + "</a>"+ "</TD>");
							
				else
					responce1.append("<TD><a href='" + server_addr + '/' + file_list[i] + "'>" + file_list[i] + "</a>" + "</TD>");

				responce1.append("<TD align='center' width='300'>" + new Date(new File(path + "\\" + file_list[i]).lastModified()).toString()+ "</TD>");
						
				responce1.append("<TD align='center' width='50'><font color=red><strong>&lt;DIR&gt;</strong></font></TD>");

			}
			else {
				responce1.append("<TD width='50'><img src=\"" + File_Icons_Path + "\" height=22 widhth=22 ></TD>");
				if (! intermediate_URI.equalsIgnoreCase(""))
					responce1.append("<TD><a href='" + server_addr + '/' + intermediate_URI + '/' + file_list[i] + "'>" + file_list[i] + "</a>"+ "</TD>");
							
				else
					responce1.append("<TD><a href='" + server_addr + '/' + file_list[i] + "'>" + file_list[i] + "</a>" + "</TD>");
				responce1.append("<TD align='center' width='300'>" + new Date(new File(path + "\\" + file_list[i]).lastModified()).toString()+ "</TD>");
						
				responce1.append("<TD align='center' width='50'><font color=red><strong>"
						+ String.valueOf(new File(path + "\\" + file_list[i]).length()) + "</font></strong></TD>");

			}
			responce1.append("</TR>");
		}
		if (! intermediate_URI.equalsIgnoreCase("")) {
			if (! intermediate_URI.contains("/")) {
				back_URI = "";
				intermediate_URI = "";
			}
			else {
				back_URI = intermediate_URI.split("/(?=[^/]+$)")[0];
			}
			responce1.append("<TR><TD></TD><TD><B><a href=" + server_addr + '/' + back_URI + ">\\..</a></B></TD></TR>");
		}
		responce1.append("</TABLE></body></html>");

		responce_header = getResponceHeaders(200) + "Content-type:" + "text/html" + CRLF + "Content-Length:" + ("" + responce1).getBytes().length
				+ CRLF + CRLF;

		return (responce_header + responce1).getBytes();
	}

	/*
	 * Returns the content-type by examining extension of filename
	 */
	private static String get_contentType(String filename) {
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

	/*
	 * Returns the local machine file system path of file specified in  the request
	 */
	private static String getFilePathFromRequest(Request request, String rootDir) {

		URI uri;

		String filename = request.get("URI").trim().substring(1);

		if (filename.equalsIgnoreCase("")) return rootDir;

		try {
			uri = new URI(filename);
			return rootDir + "\\" + uri.getPath();

		}
		catch (URISyntaxException e) {

			System.out.println("Incorrect URI");
			return null;
		}

	}

	/*
	 * Generates the default responce headers depending upon the status codes
	 */

	private static String getResponceHeaders(int statusCode) {

		String header = null;
		String defaultHeader = "Date:" + WebUtil.getCurrentDate() + CRLF + "Server: Mini HTTP server" + CRLF;

		switch (statusCode) {
		case 200:
			header = "HTTP/1.1 200 OK" + CRLF + defaultHeader;
			break;

		case 501:
			header = "HTTP/1.1 501 Not Implemented" + CRLF + defaultHeader;
			break;

		case 404:
			header = "HTTP/1.1 404 Not Found" + CRLF + defaultHeader;
			break;

		case 500:
			header = "HTTP/1.1 500 Internal Server Error" + CRLF + defaultHeader;
			break;

		case 400:
			header = "HTTP/1.1 400 Bad Request" + CRLF + defaultHeader;
			break;

		case 304:
			header = "HTTP/1.1  304 Not Modified" + CRLF + defaultHeader;
			break;

		case 412:
			header = "HTTP/1.1  412 Precondition Failed" + CRLF + defaultHeader;
			break;

		case 416:
			header = "HTTP/1.1 416 Requested Range Not Satisfiable" + CRLF + defaultHeader;
			break;

		case 206:
			header = "HTTP/1.1 206 Partial Content" + CRLF + defaultHeader;
			break;
		}
		return header;
	}
}