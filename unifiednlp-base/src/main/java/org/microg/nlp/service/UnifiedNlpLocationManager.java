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
import android.os.IBinder;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.microg.nlp.Preferences;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.api.LocationCallback;
import org.microg.nlp.geocode.BackendFuser;

public class UnifiedNlpLocationManager extends Service {
    
    private static final String TAG = "UnifiedNlpLocationManager";
    
    public static final String ACTION_LOCATION_BACKEND = "org.microg.nlp.LOCATION_BACKEND";

    private BackendFuser backendFuser;
    
    private List<LocationBackend> backends = new ArrayList<LocationBackend>();
    
    private String destinationPackageName;
    private boolean resolveAddress;
    private Location inputLocation;
        
    @Override
    public void onCreate() {
        super.onCreate();
        bindAndEnableBackends();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent.getExtras() != null) {
            destinationPackageName = intent.getExtras().getString("destinationPackageName");
            resolveAddress = intent.getExtras().getBoolean("resolveAddress");
            inputLocation = intent.getExtras().getParcelable("location");
        }
        bindAndEnableBackends();
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
        for (BackendInfo bi: queryKnownBackends()) {
            enableBackend(bi);
        }
        
        for (String backend : Preferences
                .splitBackendString(new Preferences(getBaseContext()).getLocationBackends())) {
            String[] parts = backend.split("/");
            if (parts.length >= 2) {
                Intent intent = new Intent(ACTION_LOCATION_BACKEND);
                intent.setPackage(parts[0]);
                intent.setClassName(parts[0], parts[1]);
                bindService(intent, mConnection2, Context.BIND_AUTO_CREATE);
            }
        }
    }
    
    private void sendUpdateToLocationBackends() {
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
            backendFuser = new BackendFuser(getBaseContext());
            backendFuser.bind();
            for (BackendInfo bi: queryKnownBackends()) {
                enableBackend(bi);
            }
            Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
            sendIntent.setPackage("org.microg.nlp");
            startService(sendIntent);
            
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
        if ((destinationPackageName == null) || ("".equals(destinationPackageName))) {
            return;
        }
        Intent sendIntent = new Intent("android.intent.action.LOCATION_UPDATE");
        sendIntent.setPackage(destinationPackageName);
        sendIntent.putExtra("location", location);
        if (resolveAddress) {
            List<Address> addresses = backendFuser.getFromLocation(location.getLatitude(), location.getLongitude(), 1, Locale.getDefault().getLanguage());
        
            if ((addresses != null) && (addresses.size() > 0)) {
                sendIntent.putExtra("addresses", addresses.get(0));
            }
        }
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
