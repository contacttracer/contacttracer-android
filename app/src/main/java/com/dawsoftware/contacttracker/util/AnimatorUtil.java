package com.dawsoftware.contacttracker.util;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;

public class AnimatorUtil {
	private AnimatorUtil() { }
	
	public static class CustomAnimatorListener implements AnimatorListener {
		
		protected boolean mustStop = false;
		
		@Override
		public void onAnimationStart(final Animator animation) {
			mustStop = false;
		}
		
		@Override
		public void onAnimationEnd(final Animator animation) { }
		
		@Override
		public void onAnimationCancel(final Animator animation) {
			mustStop = true;
		}
		
		@Override
		public void onAnimationRepeat(final Animator animation) { }
	}
}
