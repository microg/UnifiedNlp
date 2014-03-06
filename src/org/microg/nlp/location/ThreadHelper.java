package org.microg.nlp.location;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadHelper implements Runnable {
	private final Context context;
	private final LocationProvider locationProvider;
	private BackendHandler backendHandler;
	private ScheduledThreadPoolExecutor executor;
	private long time = 5000; // Initialize with 5s
	private boolean enabled;

	public ThreadHelper(Context context, LocationProvider locationProvider) {
		this.context = context;
		this.locationProvider = locationProvider;
		updateBackendHandler();
	}

	private void updateBackendHandler() {
		List<BackendFuser.BackendInfo> backendList = new ArrayList<BackendFuser.BackendInfo>();
		String backends = context.getSharedPreferences("config", Context.MODE_PRIVATE).getString("location_backends", "");
		for (String backend : backends.split("\\|")) {
			String[] parts = backend.split("/");
			if (parts.length == 2) {
				backendList.add(new BackendFuser.BackendInfo(parts[0], parts[1]));
			}
		}
		backendHandler = new BackendFuser(context, backendList, locationProvider);
	}

	public void reload() {
		disable();
		updateBackendHandler();
		enable();
	}

	public void disable() {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		if (enabled) {
			backendHandler.unbind();
			enabled = false;
		}
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void reset() {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		executor = new ScheduledThreadPoolExecutor(1);
		executor.scheduleAtFixedRate(this, 0, time, TimeUnit.MILLISECONDS);
	}

	public void enable() {
		if (!enabled) {
			backendHandler.bind();
			enabled = true;
		}
		reset();
	}

	@Override
	public void run() {
		backendHandler.update();
	}
}
