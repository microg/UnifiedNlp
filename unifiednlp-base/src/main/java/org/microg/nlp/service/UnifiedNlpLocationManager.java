package org.microg.nlp.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.microg.nlp.Preferences;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.api.LocationCallback;
import org.microg.nlp.geocode.BackendFuser;
import org.microg.nlp.ui.SettingsActivity;
import org.microg.nlp.util.LogToFile;

import static org.microg.nlp.util.LogToFile.appendLog;

public class UnifiedNlpLocationManager extends Service {
    
    private static final String TAG = "UnifiedNlpLocationManager";
    
    public static final String ACTION_LOCATION_BACKEND = "org.microg.nlp.LOCATION_BACKEND";

    private BackendFuser backendFuser;
    
    private List<LocationBackend> backends = new ArrayList<LocationBackend>();
    
    private String destinationPackageName;
    private boolean resolveAddress;
    private Location inputLocation;
    private LocationListener locationListener;
        
    @Override
    public void onCreate() {
        super.onCreate();

        LogToFile.logFilePathname = PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_DEBUG_FILE,"");
        LogToFile.logToFileEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_DEBUG_TO_FILE, false);
        LogToFile.logFileHoursOfLasting = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_DEBUG_FILE_LASTING_HOURS, "24"));

        bindAndEnableBackends();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        
        if (intent == null) {
            return ret;
        }
        
        appendLog(TAG, "onStartCommand:" + intent);
        destinationPackageName = null;
        resolveAddress = false;
        inputLocation = null;
        locationListener = null;
        if (intent.getExtras() != null) {
            destinationPackageName = intent.getExtras().getString("destinationPackageName", null);
            if (intent.getExtras().getParcelable("locationListener") != null) {
                locationListener = (LocationListener) intent.getExtras().getParcelable("locationListener");
            }
            resolveAddress = intent.getExtras().getBoolean("resolveAddress");
            inputLocation = intent.getExtras().getParcelable("location");
        }
        bindAndEnableBackends();
        appendLog(TAG, "onStartCommand:inputLocation:" + inputLocation);
        appendLog(TAG, "onStartCommand:destinationPackageName:" + destinationPackageName);
        if (inputLocation != null) {
            processUpdateOfLocation(inputLocation);
        } else {
            sendUpdateToLocationBackends();
        }
        return START_STICKY;
    }
    
    private void bindAndEnableBackends() {
        if ((backends.size() > 0) && (backendFuser != null) && backendFuser.haveAllHelpersBackend()) {
            return;
        }
        appendLog(TAG, "sendUpdateToLocationBackends:Preferences.splitBackendString(new Preferences(getBaseContext()).getLocationBackends()):" + Preferences.splitBackendString(new Preferences(getBaseContext()).getLocationBackends()));
        for (String backend : Preferences
                .splitBackendString(new Preferences(getBaseContext()).getLocationBackends())) {
            appendLog(TAG, "sendUpdateToLocationBackends:backend:" + backend);
            String[] parts = backend.split("/");
            appendLog(TAG, "sendUpdateToLocationBackends:parts.length:" + parts.length);
            if (parts.length >= 2) {
                Intent intent = new Intent(ACTION_LOCATION_BACKEND);
                intent.setPackage(parts[0]);
                intent.setClassName(parts[0], parts[1]);
                bindService(intent, mConnection2, Context.BIND_AUTO_CREATE);
            }
        }
        backendFuser = new BackendFuser(getBaseContext());
        backendFuser.reset();
        backendFuser.bind();
    }
    
    private void sendUpdateToLocationBackends() {
        appendLog(TAG, "sendUpdateToLocationBackends:backends:" + backends + ":" + ((backends != null)?backends.size():""));
        try {
            for (LocationBackend backend: backends) {
                backend.update();
            }
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private final ServiceConnection mConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            LocationBackend locationBackend = LocationBackend.Stub.asInterface(service);
            try {
                locationBackend.open(new LocationUpdate());
            } catch (RemoteException re) {
                re.printStackTrace();
            }
            backends.add(locationBackend);
            for (BackendInfo bi: queryKnownBackends()) {
                enableBackend(bi);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            backends.clear();
        }
    };
    
    public class LocationUpdate extends LocationCallback.Stub {
        @Override
        public void report(Location location) throws RemoteException {
            processUpdateOfLocation(location);
        }
    }
    
    private void processUpdateOfLocation(Location location) {
        appendLog(TAG, "processUpdateOfLocation:location:" + location + ":destinationPackageName:" + destinationPackageName + ":" + locationListener);
        if ((destinationPackageName == null) || ("".equals(destinationPackageName))) {
            if (locationListener != null) {
                locationListener.onLocationChanged(location);
            }
            return;
        }
        Intent sendIntent = new Intent("android.intent.action.LOCATION_UPDATE");
        sendIntent.setPackage(destinationPackageName);
        sendIntent.putExtra("location", location);
        appendLog(TAG, "processUpdateOfLocation:resolveAddress:" + resolveAddress);
        if (resolveAddress && (location != null) && (backendFuser != null)) {
            appendLog(TAG, "processUpdateOfLocation:location:" + location.getLatitude() + ", " + location.getLongitude() + ", " + Locale.getDefault().getLanguage());
            List<Address> addresses = backendFuser.getFromLocation(location.getLatitude(), location.getLongitude(), 1, Locale.getDefault().getLanguage());
            appendLog(TAG, "processUpdateOfLocation:addresses:" + addresses);
            if ((addresses != null) && (addresses.size() > 0)) {
                sendIntent.putExtra("addresses", addresses.get(0));
            }
        }
        appendLog(TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
        startService(sendIntent);
    }
    
    protected void enableBackend(BackendInfo backendInfo) {
        try {
                Intent intent = buildBackendIntent();
                intent.setPackage(backendInfo.serviceInfo.packageName);
                intent.setClassName(backendInfo.serviceInfo.packageName, backendInfo.serviceInfo.name);
                
                getBaseContext().bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {

                    }
                }, BIND_AUTO_CREATE);
        } catch (Exception e) {
            backendInfo.enabled = false;
        }
    }
    
    protected Intent buildBackendIntent() {
        return new Intent(ACTION_LOCATION_BACKEND);
    }
    
    List<BackendInfo> queryKnownBackends() {
        return intentToKnownBackends(buildBackendIntent());
    }
    
    List<BackendInfo> intentToKnownBackends(Intent intent) {
        List<BackendInfo> knownBackends = new ArrayList<BackendInfo>();
        List<ResolveInfo> resolveInfos = getBaseContext().getPackageManager()
                .queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfos) {
            ServiceInfo serviceInfo = info.serviceInfo;
            String simpleName = String.valueOf(serviceInfo.loadLabel(getBaseContext().getPackageManager()));
            String signatureDigest = Preferences.firstSignatureDigest(getBaseContext(), serviceInfo.packageName);
            knownBackends.add(new BackendInfo(serviceInfo, simpleName, signatureDigest));
        }
        return knownBackends;
    }
    
    private class BackendInfo {
        private final ServiceInfo serviceInfo;
        private final String simpleName;
        private final String signatureDigest;
        private boolean enabled = false;

        public BackendInfo(ServiceInfo serviceInfo, String simpleName, String signatureDigest) {
            this.serviceInfo = serviceInfo;
            this.simpleName = simpleName;
            this.signatureDigest = signatureDigest;
        }

        public String getMeta(String metaName) {
            return serviceInfo.metaData != null ? serviceInfo.metaData.getString(metaName) : null;
        }

        @Override
        public String toString() {
            return simpleName;
        }

        public String toSettingsString() {
            return serviceInfo.packageName + "/" + serviceInfo.name + "/" + signatureDigest;
        }
    }
}
