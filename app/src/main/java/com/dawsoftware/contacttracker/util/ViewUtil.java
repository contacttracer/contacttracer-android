package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

public class ViewUtil {
	private ViewUtil() {}
	
	public static void setTint(final Context context, final ImageView view, int color) {
		if (view == null) {
			return;
		}
		
		view.setColorFilter(
				ContextCompat.getColor(context, color),
				android.graphics.PorterDuff.Mode.SRC_IN);
	}
	
	public static Bitmap createBitmapFromView(final View v) {
		Bitmap b = Bitmap.createBitmap( v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
		v.draw(c);
		return b;
	}
}
