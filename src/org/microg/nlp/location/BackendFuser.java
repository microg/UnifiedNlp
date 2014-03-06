package org.microg.nlp.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import org.microg.nlp.api.NlpApiConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BackendFuser implements BackendHandler, LocationProvider {
	private static final String TAG = BackendFuser.class.getName();

	private final List<BackendHandler> backendHandlers;
	private final LocationProvider locationProvider;
	private boolean fusing = false;

	public BackendFuser(Context context, List<BackendInfo> backends, LocationProvider provider) {
		locationProvider = provider;
		backendHandlers = new ArrayList<BackendHandler>();
		for (BackendInfo backendInfo : backends) {
			Intent intent = new Intent(NlpApiConstants.ACTION_LOCATION_BACKEND);
			intent.setPackage(backendInfo.packageName);
			intent.setClassName(backendInfo.packageName, backendInfo.className);
			backendHandlers.add(new BackendHelper(context, this, intent));
		}
	}

	@Override
	public void unbind() {
		for (BackendHandler handler : backendHandlers) {
			handler.unbind();
		}
	}

	@Override
	public boolean bind() {
		fusing = false;
		boolean handlerBound = false;
		for (BackendHandler handler : backendHandlers) {
			if (handler.bind()) {
				handlerBound = true;
			}
		}
		return handlerBound;
	}

	@Override
	public Location update() {
		fusing = true;
		for (BackendHandler handler : backendHandlers) {
			handler.update();
		}
		fusing = false;
		return getLastLocation();
	}

	@Override
	public Location getLastLocation() {
		List<Location> locations = new ArrayList<Location>();
		for (BackendHandler handler : backendHandlers) {
			locations.add(handler.getLastLocation());
		}
		if (locations.isEmpty()) {
			return null;
		} else {
			Collections.sort(locations, LocationComparator.INSTANCE);
			if (locations.get(0) != null) {
				locationProvider.reportLocation(locations.get(0));
				Log.v(TAG, "location=" + locations.get(0));
			}
			return locations.get(0);
		}
	}

	@Override
	public void onEnable() {
		locationProvider.onEnable();
	}

	@Override
	public void onDisable() {
		locationProvider.onDisable();
	}

	@Override
	public void reportLocation(Location location) {
		if (fusing) return;
		getLastLocation();
	}

	@Override
	public IBinder getBinder() {
		return locationProvider.getBinder();
	}

	@Override
	public void reload() {
		locationProvider.reload();
	}

	public static class BackendInfo {
		private final String packageName;
		private final String className;

		public BackendInfo(String packageName, String className) {
			this.packageName = packageName;
			this.className = className;
		}
	}

	public static class LocationComparator implements Comparator<Location> {

		public static final LocationComparator INSTANCE = new LocationComparator();
		public static final long SWITCH_ON_FRESHNESS_CLIFF_MS = 30000; // 30 seconds TODO: make it a setting

		/**
		 * @return whether {@param lhs} is better than {@param rhs}
		 */
		@Override
		public int compare(Location lhs, Location rhs) {
			if (lhs == rhs)
				return 0;
			if (lhs == null) {
				return 1;
			}
			if (rhs == null) {
				return -1;
			}
			if (rhs.getTime() > lhs.getTime() + SWITCH_ON_FRESHNESS_CLIFF_MS) {
				return 1;
			}

			if (lhs.getTime() > rhs.getTime() + SWITCH_ON_FRESHNESS_CLIFF_MS) {
				return -1;
			}
			if (!lhs.hasAccuracy()) {
				return 1;
			}
			if (!rhs.hasAccuracy()) {
				return -1;
			}
			return (int) (lhs.getAccuracy() - rhs.getAccuracy());
		}
	}
}
