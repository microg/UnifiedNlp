package org.microg.nlp.location;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import org.microg.nlp.ProviderService;

import static org.microg.nlp.api.NlpApiConstants.*;

public abstract class LocationService extends ProviderService<LocationProvider> {
    public static void reloadLocationService(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_SETTINGS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            intent.setClass(context, LocationServiceV2.class);
        } else {
            intent.setClass(context, LocationServiceV1.class);
        }
        context.startService(intent);
    }

    /**
     * Creates an LocationService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public LocationService(String tag) {
        super(tag);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_FORCE_LOCATION.equals(intent.getAction())) {
            if (checkCallingPermission(PERMISSION_FORCE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                LocationProvider provider = getCurrentProvider();
                if (provider != null && intent.hasExtra(INTENT_EXTRA_LOCATION)) {
                    provider.forceLocation(
                            (Location) intent.getParcelableExtra(INTENT_EXTRA_LOCATION));
                }
            }
        }

        if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            LocationProvider provider = getCurrentProvider();
            if (provider != null) {
                provider.reload();
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LocationProvider provider = getCurrentProvider();
        if (provider != null) {
            provider.onDisable();
        }
        return super.onUnbind(intent);
    }
}
