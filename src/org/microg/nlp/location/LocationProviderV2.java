package org.microg.nlp.location;

import android.content.Context;
import android.location.Criteria;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.Log;
import com.android.location.provider.LocationProviderBase;
import com.android.location.provider.LocationRequestUnbundled;
import com.android.location.provider.ProviderPropertiesUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;

import static android.location.LocationProvider.AVAILABLE;

public class LocationProviderV2 extends LocationProviderBase implements LocationProvider {
	private static final String TAG = LocationProviderV2.class.getName();
	private static final ProviderPropertiesUnbundled props = ProviderPropertiesUnbundled.create(
			false, // requiresNetwork
			false, // requiresSatellite
			false, // requiresCell
			false, // hasMonetaryCost
			true, // supportsAltitude
			true, // supportsSpeed
			true, // supportsBearing
			Criteria.POWER_LOW, // powerRequirement
			Criteria.ACCURACY_COARSE); // accuracy
	private ThreadHelper helper;

	public LocationProviderV2(Context context) {
		super(TAG, props);
		this.helper = new ThreadHelper(context, this);
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void reload() {
		helper.reload();
	}

	@Override
	public void onEnable() {
	}

	@Override
	public int onGetStatus(Bundle extras) {
		return AVAILABLE;
	}

	@Override
	public long onGetStatusUpdateTime() {
		return 0;
	}

	@Override
	public void onSetRequest(ProviderRequestUnbundled requests, WorkSource source) {
		Log.v(TAG, "onSetRequest: " + requests + " by " + source);

		long autoTime = Long.MAX_VALUE;
		boolean autoUpdate = false;
		for (LocationRequestUnbundled request : requests.getLocationRequests()) {
			Log.v(TAG, "onSetRequest: request: " + request);
			if (autoTime > request.getInterval()) {
				autoTime = request.getInterval();
			}
			autoUpdate = true;
		}

		if (autoTime < 1500) {
			// Limit to 1.5s
			autoTime = 1500;
		}
		Log.v(TAG, "using autoUpdate=" + autoUpdate + " autoTime=" + autoTime);

		if (autoUpdate) {
			helper.setTime(autoTime);
			helper.enable();
		} else {
			helper.disable();
		}
	}
}
