package com.dawsoftware.contacttracker.util.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WorkerThreadExecutor implements Executor {
	private final int THREAD_POOL_CAPACITY = 5;
	
	private Executor workerExecutor;
	
	public WorkerThreadExecutor() {
		workerExecutor = Executors.newFixedThreadPool(THREAD_POOL_CAPACITY);
	}
	
	@Override
	public void execute(final Runnable command) {
		workerExecutor.execute(command);
	}
}
