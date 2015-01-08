package com.dynatrace.diagnostics.plugins.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.net.www.protocol.http.AuthCacheImpl;
import sun.net.www.protocol.http.AuthCacheValue;

import com.dynatrace.diagnostics.plugins.exception.ReportCreationException;
import com.dynatrace.diagnostics.sdk.resources.BaseConstants;

public class HelperUtils {
	public static final String DEFAULT_ENCODING = System.getProperty("file.encoding","UTF-8");
	public static final String EMPTY_STRING = "";
	public final static Map<String, String> CONTENT_TYPES_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {
		private static final long serialVersionUID = -4867569384183231001L;
	{
		put("HTML", "text/html");
		put("PDF", "application/pdf");
		put("XML", "application/xml");
		put("CSV", "text/csv");
		put("XSD", "text/plain");
		put("XLS", "application/vnd.ms-excel");
		}});
	static private SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
	static private SimpleDateFormat sdfMonth = new SimpleDateFormat("MM");
	static private SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
	static private SimpleDateFormat sdfHours = new SimpleDateFormat("HH");
	static private SimpleDateFormat sdfMinutes = new SimpleDateFormat("mm");
	static private SimpleDateFormat sdfSeconds = new SimpleDateFormat("ss");
	static private SimpleDateFormat sdfOracleDate = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
	private static final Logger log = Logger.getLogger(HelperUtils.class.getName());
	
	public static String getExceptionAsString(Exception e) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getExceptionAsString method");
		}
		String msg;
		if ((msg = e.getMessage()) == null) {
			msg = BaseConstants.DASH;
		}
		return new StringBuilder(e.getClass().getCanonicalName()
				+ " exception occurred. Message = '").append(msg)
				.append("'; Stacktrace is '").append(getStackTraceAsString(e))
				.append("'").toString();
	}

	public static String getStackTraceAsString(Exception e) {
		String returnString = "";
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getStackTraceAsString method");
		}
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		try {
			e.printStackTrace(new PrintStream(ba, true, DEFAULT_ENCODING));
			returnString = ba.toString(DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e1) {
			log.finer("getStackTraceAsString method: UnsupportedEncodingException ; message is '" + e1.getMessage() + "'");
		}
		return returnString;
	}
	
	public static synchronized Double getIntDate(Date date) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getIntDate method");
		}
		if (date == null) {
			return Double.NaN;
		}
		int result = Integer.parseInt(sdfYear.format(date)) * 10000 + Integer.parseInt(sdfMonth.format(date))*100 + Integer.parseInt(sdfDay.format(date));
		log.finer("getIntDate method: integer date is " + result);
		return Double.valueOf(result);
	}
	
	public static synchronized Double getIntTime(Date date) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getIntTime method");
		}
		if (date == null) {
			return Double.NaN;
		}
		int result = Integer.parseInt(sdfHours.format(date)) * 10000 + Integer.parseInt(sdfMinutes.format(date))*100 + Integer.parseInt(sdfSeconds.format(date));
		log.finer("getIntTime method: integer time is " + result);
		return Double.valueOf(result);
	}
	
	public static synchronized Date getOracleDate(String dateString) {
		Date date = null;
		if (dateString != null && !dateString.isEmpty()) {
			try {
				date = sdfOracleDate.parse(dateString);
				log.finer("getOracleDate method: date is '" + date + "'");
			} catch (ParseException e) {
				log.severe("getOracleDate method: parse exception of the date string '" + dateString + "'");
			}
		}
		
		return date;
	}
	
	public static synchronized Double getLongOracleDateTime(Date date) {
		if (date == null) {
			return Double.NaN;
		}
		long result = Long.parseLong(sdfYear.format(date)) * 10000000000L + Long.parseLong(sdfMonth.format(date))*100000000L + Long.parseLong(sdfDay.format(date)) * 1000000L 
				+ Long.parseLong(sdfHours.format(date)) * 10000L + Long.parseLong(sdfMinutes.format(date))*100L + Long.parseLong(sdfSeconds.format(date));
		return Double.valueOf(result);
	}
	
	public static String getUniqueKey(Map<String, String> keys, String key) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getUniqueKey method");
		}
		int i = 0;
		String newKey = new StringBuilder(key).append("_").append(++i).toString();
		while (keys.containsKey(newKey)) {
			newKey = new StringBuilder(key).append("_").append(++i).toString();
			log.finer("getUniqueKey method: newKey is '" + newKey + "'");
		}
		
		return newKey;
	}

	public static File getFileFromUrl(String strUrl, String dashboardName, String dashboardType, final String user, final String pwd) {
		String s, s1;
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getFileFromUrl method");
		}
		
		File file = null;
		if (log.isLoggable(Level.FINER)) {
			log.finer("getFileFromUrl method: contacting URL : '" + strUrl + "'; dashboardName is '" + dashboardName + "'; user is '" + user + "'; password is '" + pwd + "'");
		}

		URL url;
		FileOutputStream fos1 = null;
		InputStream is1 = null;
		try {
			url = new URL(strUrl);

			AuthCacheValue.setAuthCache(new AuthCacheImpl());
			Authenticator.setDefault(new Authenticator() { 
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					PasswordAuthentication pa = new PasswordAuthentication(user, pwd.toCharArray());
					if (log.isLoggable(Level.FINER)) {
						log.finer(String.format("getPasswordAuthentication method: user is '%s'; password is '%s'; class is '%s'", pa.getUserName(), new String(pa.getPassword()), pa.getClass().getCanonicalName()));
					}
					
					return pa;
				}
			});
			
			file = File.createTempFile(dashboardName, (s = new StringBuilder().append(".").append(dashboardType.toLowerCase()).toString()));
			if (log.isLoggable(Level.FINER)) {
				log.finer("getFileFromUrl method: suffix of the report file is '" + s + "'");
			}
			// The temp file needs to be deleted upon termination of this application
			file.deleteOnExit();

			byte[] ba1 = new byte[1024];
			int baLength;
			fos1 = new FileOutputStream(file);

			// Checking whether the URL contains requested report type
			URLConnection urlConnection = url.openConnection();
			int code;
			if (isHttpURLConnection(url) && !((code = getResponseCode(urlConnection)) >= 100 && code < 300)) {
				String msg = String.format("Response code '%d' returned with the message '%s' from url path '%s' url query '%s'", code, getResponseMessage(urlConnection), url.getPath(), url.getQuery());
				log.severe("getFileFromUrl method: '" + msg + "'");
				throw new ReportCreationException(msg);
			}
			if (!(s = urlConnection.getContentType()).equalsIgnoreCase((s1 = CONTENT_TYPES_MAP.get(dashboardType.toUpperCase())))) {
				String msg = String.format("Retrieved dashboard report has content type '%s', while it should have '%s' content type", s, s1);
				log.severe("getFileFromUrl method: " + msg);
				throw new ReportCreationException(msg);
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("getFileFromUrl method: content of the report file is '" + s + "'");
				}
				// Read report file from the URL and save to a local file
				is1 = url.openStream();
				while ((baLength = is1.read(ba1)) != -1) {
					fos1.write(ba1, 0, baLength);
				}
			}

		} catch (FileNotFoundException e) {
			String msg;
			log.severe("getFileFromUrl method: " + (msg = HelperUtils.getExceptionAsString(e)));
			throw new ReportCreationException(msg, e);
		} catch (MalformedURLException e) {
			String msg;
			log.severe("getFileFromUrl method: " + (msg = HelperUtils.getExceptionAsString(e)));
			throw new ReportCreationException(msg, e);
		} catch (IOException e) {
			String msg;
			log.severe("getFileFromUrl method: " + (msg = HelperUtils.getExceptionAsString(e)));
			throw new ReportCreationException(msg, e);
		} catch (RuntimeException e) {
			String msg;
			log.severe("getFileFromUrl method: " + (msg = HelperUtils.getExceptionAsString(e)));
			throw new ReportCreationException(msg, e);
		} finally {
			try {
				if (fos1 != null) fos1.flush();
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (fos1 != null) fos1.close();
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (is1 != null) is1.close();
			} catch (IOException e) {
				// ignore
			}
		}

		return file;
	}
	
	public static boolean isHttpURLConnection(URL url) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering isHttpURLConnection method");
			log.finer("isHttpURLConnection method: protocol is '" + url.getProtocol() + "'");
		}
		return url.getProtocol().toUpperCase().startsWith("HTTP");
	}
	
	public static int getResponseCode(URLConnection urlConnection) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getResponseCode method");
		}
		if (!(urlConnection instanceof HttpURLConnection)) {
			return -1;
		}
		
		try {
			if (log.isLoggable(Level.FINER)) {
				log.finer("getResponseCode method: response code is '" + ((HttpURLConnection)urlConnection).getResponseCode() + "'");
			}
			return ((HttpURLConnection)urlConnection).getResponseCode();
		} catch (IOException e) {
			log.severe("getResponseCode method: '" + e.getMessage() + "'");
			throw new ReportCreationException(e.getMessage(), e);
		}
	}
	
	public static String getResponseMessage(URLConnection urlConnection) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getResponseMessage method");
		}
		if (!(urlConnection instanceof HttpURLConnection)) {
			return EMPTY_STRING;
		}
		
		try {
			if (log.isLoggable(Level.FINER)) {
				log.finer("getResponseMessage method: response message is '" + ((HttpURLConnection)urlConnection).getResponseMessage() + "'");
			}
			return ((HttpURLConnection)urlConnection).getResponseMessage();
		} catch (IOException e) {
			log.severe("getResponseMessage method: '" + e.getMessage() + "'");
			throw new ReportCreationException(e.getMessage(), e);
		}
	}

}
