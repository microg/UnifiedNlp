package org.microg.nlp.location;

import android.content.Context;
import android.content.Intent;
import org.microg.nlp.api.NlpApiConstants;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadHelper implements Runnable {
	private BackendHelper api;
	private ScheduledThreadPoolExecutor executor;
	private long time = 5000; // Initialize with 5s
	private boolean enabled;

	public ThreadHelper(Context context, LocationProvider provider) {
		api = new BackendHelper(context, provider, new Intent(NlpApiConstants.ACTION_LOCATION_BACKEND));
	}

	public void disable() {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		if (enabled) {
			api.unbind();
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
			api.bind();
			enabled = true;
		}
		reset();
	}

	@Override
	public void run() {
		api.update();
	}
}
