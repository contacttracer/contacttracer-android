package com.dawsoftware.contacttracker.util.executors;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class MainThreadExecutor implements Executor {
	private final Handler mainHandler;
	
	public MainThreadExecutor() {
		mainHandler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public void execute(final Runnable command) {
		mainHandler.post(command);
	}
}
