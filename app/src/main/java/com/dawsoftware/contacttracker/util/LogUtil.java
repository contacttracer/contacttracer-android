package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class LogUtil {
	private LogUtil() {}
	
	public static final String NET = "netLog.txt";
	public static final String DB = "dbLog.txt";
	public static final String PUSH = "pushLog.txt";
	
	public static synchronized void writeToFile(String data, Context context, String fileName) {
		try {
			OutputStreamWriter outputStreamWriter =
					new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE | Context.MODE_APPEND));
			outputStreamWriter.write(data+"\n");
			outputStreamWriter.close();
		}
		catch (IOException e) {
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}
	
	public static String readFromFile(Context context, String fileName) {
		
		String ret = "";
		
		try {
			InputStream inputStream = context.openFileInput(fileName);
			
			if ( inputStream != null ) {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String receiveString = "";
				StringBuilder stringBuilder = new StringBuilder();
				
				while ( (receiveString = bufferedReader.readLine()) != null ) {
					stringBuilder.append("\n").append(receiveString);
				}
				
				inputStream.close();
				ret = stringBuilder.toString();
			}
		}
		catch (FileNotFoundException e) {
			Log.e("login activity", "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e("login activity", "Can not read file: " + e.toString());
		}
		
		return ret;
	}
	
	public static void clearFile(Context context, String fileName) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
			outputStreamWriter.flush();
			outputStreamWriter.close();
		}
		catch (IOException e) {
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}
}
