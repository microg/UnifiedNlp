package org.microg.nlp.api.sample;

import android.location.Location;
import android.util.Log;
import org.microg.nlp.api.LocationBackendService;

import java.util.Random;

public class ThirdSampleService extends LocationBackendService {
	private static final String TAG = ThirdSampleService.class.getName();

	private Thread regular;
	private Random random = new Random();

	@Override
	protected void onOpen() {
		super.onOpen();
		regular = new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (regular) {
					while (!regular.isInterrupted()) {
						try {
							regular.wait(60000);
						} catch (InterruptedException e) {
							return;
						}
						Location location = new Location("random");
						location.setLatitude(random.nextDouble() * 90);
						location.setLongitude(random.nextDouble() * 90);
						location.setAccuracy(random.nextFloat() * 90);
						Log.d(TAG, "Just reported: " + location);
						report(location);
					}
				}
			}
		});
		regular.start();
	}

	@Override
	protected void onClose() {
		if (regular != null && regular.isAlive()) {
			regular.interrupt();
		}
	}
}
