UnifiedNlpApi
=============
This library contains anything needed to build a backend for UnifiedNlp.

Writing the service
-------------------
### The easy way (Location)
Writing a service is fairly easy. Just create a class that extends `org.microg.nlp.api.LocationBackendService`, it provides several methods:

#### `update()`-method
You'll most likely want to override this method. It is called every time when an application requests a location.

However, as this method is blocking, you should not do heavy I/O operations (like network) in it.
If your backend uses a remote provider for location retrieval, do requests in an additional thread and return null in `update()`. 
On request success, use `report()` to send the new location to the requesting application.

See JavaDoc for additional information.

#### `onOpen()`-method and `onClose()`-method
These might be interesting to override too. `onOpen()` is called after UnifiedNlp connected to this backend and `onClose()` is called before connection closure.
This is a good place to initialize or respectively destroy whatever you need during `update()` calls.

#### `report(Location)`-method
You can call this method every time to report the given location as soon as possible.

### The easy way (Geocoding)
Providing a Geocoder is even simpler than a LocationProvider. Extend `org.microg.nlp.api.GeocoderBackendService` and implement the methods `getFromLocation` and `getFromLocationName`.
Both methods reflect a call to the corresponding method in `android.location.Geocoder`.

### The flexible way
Instead of using the `LocationBackendService` helper class you can do it by hand. 
It's important that your service overrides the `onBind()` method and responds with a `Binder` to the `LocationBackend` interface.

Advertise your service
----------------------
To let UnifiedNlp see your service you need to advertise it by providing the `org.microg.nlp.LOCATION_BACKEND` action. 

For security reasons, you should add an `android:permission` restriction to `android.permission.ACCESS_COARSE_LOCATION`. This ensures only application with access to coarse locations will be able to connect to your service.

You may want to set `android:icon` and `android:label` to something reasonable, else your applications icon/label are used.
If your backend has settings you can advertise it's activity using the `org.microg.nlp.BACKEND_SETTINGS_ACTIVITY` meta-data so that it is callable from the UnifiedNlp settings.

A service entry for a backend service could be:

	<service
		android:name=".SampleService"
		android:exported="true"
		android:permission="android.permission.ACCESS_COARSE_LOCATION"
		android:label="A very nice Backend">
		<intent-filter>
			<action android:name="org.microg.nlp.LOCATION_BACKEND" />
		</intent-filter>
		<meta-data
			android:name="org.microg.nlp.BACKEND_SETTINGS_ACTIVITY"
			android:value="org.microg.nlp.api.sample.SampleActivity" />
	</service>
