package com.dawsoftware.contacttracker.util.executors;

public class AppExecutors {
	public final MainThreadExecutor mainThread;
	public final WorkerThreadExecutor workerThread;
	
	public AppExecutors(final MainThreadExecutor mainThreadExecutor, final WorkerThreadExecutor workerThreadExecutor) {
		mainThread = mainThreadExecutor;
		workerThread = workerThreadExecutor;
	}
}
