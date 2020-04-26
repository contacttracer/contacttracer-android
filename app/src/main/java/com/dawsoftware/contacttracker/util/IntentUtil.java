package com.dawsoftware.contacttracker.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.core.content.FileProvider;
import com.dawsoftware.contacttracker.BuildConfig;

public class IntentUtil {
	
	public static final String DEFAULT_APP_SHARE_URL = "http://play.google.com/store/apps/details?id=com.dawsoftware.contacttracker";
	
	private IntentUtil() { }
	
	public static void shareBitmap(final Context context, final Bitmap bitmap) {
		
		if (context == null || bitmap == null) {
			return;
		}
		
		try {
			File cachePath = new File(context.getCacheDir(), "images");
			cachePath.mkdirs();
			FileOutputStream stream = new FileOutputStream(cachePath + "/image.png");
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File imagePath = new File(context.getCacheDir(), "images");
		File newFile = new File(imagePath, "image.png");
		Uri contentUri = FileProvider.getUriForFile(context, "com.dawsoftware.contacttracker.fileprovider", newFile);
		
		if (contentUri != null) {
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) {
				return;
			}
			
			shareIntent.setDataAndType(contentUri, resolver.getType(contentUri));
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			
			context.startActivity(Intent.createChooser(shareIntent, "Choose an app"));
		}
	}
	
	public static Intent getLocationServicesIntent() {
		return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	}
	
	public static Intent getAppSettingsIntent() {
		return new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
				Uri.parse("package:" + BuildConfig.APPLICATION_ID));
	}
	
	public static Intent getShareIntent(final String appUrl) {
		
		String url;
		
		if (StringUtil.isEmpty(appUrl)) {
			url = DEFAULT_APP_SHARE_URL;
		} else {
			url = appUrl;
		}
		
		final Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, url);
		sendIntent.setType("text/plain");
		
		return Intent.createChooser(sendIntent, null);
	}
	
	public static Intent getGeoShareIntent(float latitude, float longitude) {
		final String uri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude;
		return new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
	}
}
