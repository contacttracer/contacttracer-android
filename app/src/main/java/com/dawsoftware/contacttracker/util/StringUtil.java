package com.dawsoftware.contacttracker.util;

public class StringUtil {
	private StringUtil() {}
	
	public static boolean isEmpty(final String string) {
		return string == null || string.isEmpty();
	}
}
