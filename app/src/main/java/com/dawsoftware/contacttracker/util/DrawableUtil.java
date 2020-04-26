package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class DrawableUtil {
	private DrawableUtil() {}
	
	public static Drawable getTintedDrawableOfColorResId(@NonNull Context context, @NonNull Bitmap inputBitmap, @ColorRes int colorResId) {
		return getTintedDrawable(context, new BitmapDrawable(context.getResources(), inputBitmap), ContextCompat.getColor(context, colorResId));
	}
	
	public static Drawable getTintedDrawable(@NonNull Context context, @NonNull Bitmap inputBitmap, @ColorInt int color) {
		return getTintedDrawable(context, new BitmapDrawable(context.getResources(), inputBitmap), color);
	}
	
	public static Drawable getTintedDrawable(@NonNull Context context, @NonNull Drawable inputDrawable, @ColorInt int color) {
		Drawable wrapDrawable = DrawableCompat.wrap(inputDrawable);
		DrawableCompat.setTint(wrapDrawable, color);
		DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
		return wrapDrawable;
	}
	
	public static Bitmap getLargeIconForNotification(@DrawableRes final int drawableRes, final Context context,
	                                                 @ColorRes Integer tintColor) {
		
		if (context == null) {
			return null;
		}
		
		int color = context.getResources().getColor(tintColor);
		
		Drawable srcDrawable = context.getResources().getDrawable(drawableRes);
		Drawable tintedDrawable = getTintedDrawable(context, srcDrawable, color);
		
		Canvas canvas = new Canvas();
		Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
		canvas.setBitmap(bitmap);
		
		tintedDrawable.setBounds(0, 0, 100, 100);
		tintedDrawable.draw(canvas);
		
		return bitmap;
	}
}
