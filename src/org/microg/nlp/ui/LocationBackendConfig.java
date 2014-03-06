package org.microg.nlp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import org.microg.nlp.R;
import org.microg.nlp.api.NlpApiConstants;
import org.microg.nlp.location.LocationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationBackendConfig extends Activity {
	private static final String TAG = LocationBackendConfig.class.getName();

	private List<ServiceInfo> activeBackends;
	private Map<ServiceInfo, KnownBackend> knownBackends;
	private List<ServiceInfo> unusedBackends;
	private Adapter adapter;
	private DynamicListView listView;
	private View addButton;
	private PopupMenu popUp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pluginselection);
		listView = (DynamicListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> view, View view2, int i, long l) {
				Log.d(TAG, "onItemClick: " + l);
			}
		});
		addButton = findViewById(android.R.id.button1);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showAddPluginPopup(view);
			}
		});

		updateBackends();
	}

	private void updateBackends() {
		activeBackends = new ArrayList<ServiceInfo>();
		knownBackends = new HashMap<ServiceInfo, KnownBackend>();
		unusedBackends = new ArrayList<ServiceInfo>();
		Intent intent = new Intent(NlpApiConstants.ACTION_LOCATION_BACKEND);
		List<ResolveInfo> resolveInfos = getPackageManager().queryIntentServices(intent, PackageManager.GET_META_DATA);
		for (ResolveInfo info : resolveInfos) {
			ServiceInfo serviceInfo = info.serviceInfo;
			String simpleName = String.valueOf(serviceInfo.loadLabel(getPackageManager()));
			Drawable icon = serviceInfo.loadIcon(getPackageManager());
			knownBackends.put(serviceInfo, new KnownBackend(serviceInfo, simpleName, icon));
		}
		String activeBackendString = getSharedPreferences("config", MODE_PRIVATE).getString("location_backends", "");
		for (String backend : activeBackendString.split("\\|")) {
			String[] parts = backend.split("/");
			if (parts.length == 2) {
				for (ServiceInfo serviceInfo : knownBackends.keySet()) {
					if (serviceInfo.packageName.equals(parts[0]) && serviceInfo.name.equals(parts[1])) {
						activeBackends.add(serviceInfo);
					}
				}
			}
		}
		updateAddButton();
		resetAdapter();
	}

	private void updateAddButton() {
		if (activeBackends.size() == knownBackends.size()) {
			if (knownBackends.isEmpty()) {
				// No backend installed
				// TODO: notify user about that
			}
			addButton.setVisibility(View.GONE);
		} else {
			addButton.setVisibility(View.VISIBLE);
		}
	}

	private void resetAdapter() {
		List<ServiceInfo> backends = activeBackends;
		adapter = new Adapter(backends);
		listView.setList(backends);
		listView.setAdapter(adapter);
	}

	private void updateUnusedBackends() {
		unusedBackends.clear();
		for (KnownBackend backend : knownBackends.values()) {
			if (!activeBackends.contains(backend.serviceInfo)) {
				unusedBackends.add(backend.serviceInfo);
			}
		}
	}

	private void enableBackend(ServiceInfo serviceInfo) {
		activeBackends.add(serviceInfo);
		onBackendsChanged();
	}

	private void disabledBackend(ServiceInfo serviceInfo) {
		activeBackends.remove(serviceInfo);
		onBackendsChanged();
	}

	private void onBackendsChanged() {
		updateAddButton();
		resetAdapter();
		getSharedPreferences("config", MODE_PRIVATE).edit().putString("location_backends", backendString(activeBackends)).commit();
		LocationService.reloadLocationService(this);
	}

	private String backendString(List<ServiceInfo> backends) {
		StringBuilder sb = new StringBuilder();
		for (ServiceInfo backend : backends) {
			if (sb.length() != 0) {
				sb.append("|");
			}
			sb.append(backend.packageName).append("/").append(backend.name);
		}
		return sb.toString();
	}

	private void showAddPluginPopup(View anchorView) {
		updateUnusedBackends();

		if (popUp != null) {
			popUp.dismiss();
		}
		popUp = new PopupMenu(this, anchorView);

		for (int i = 0; i < unusedBackends.size(); i++) {
			KnownBackend backend = knownBackends.get(unusedBackends.get(i));
			String label = backend.simpleName;
			if (TextUtils.isEmpty(label)) {
				label = backend.serviceInfo.name;
			}
			popUp.getMenu().add(Menu.NONE, i, Menu.NONE, label);
		}
		popUp.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				popUp.dismiss();
				popUp = null;

				enableBackend(unusedBackends.get(menuItem.getItemId()));
				return true;
			}
		});
		popUp.show();
	}

	private Intent createSettingsIntent(ComponentInfo componentInfo) {
		Intent settingsIntent = new Intent(Intent.ACTION_VIEW);
		settingsIntent.setPackage(componentInfo.packageName);
		settingsIntent.setClassName(componentInfo.packageName,
				componentInfo.metaData.getString(NlpApiConstants.METADATA_BACKEND_SETTINGS_ACTIVITY));
		return settingsIntent;
	}

	private void showSettingsPopup(View anchorView, final KnownBackend backend) {
		if (popUp != null) {
			popUp.dismiss();
		}
		popUp = new PopupMenu(this, anchorView);
		popUp.getMenu().add(Menu.NONE, 0, Menu.NONE, "Remove");  // TODO label
		if (backend.serviceInfo.metaData != null &&
				backend.serviceInfo.metaData.getString(NlpApiConstants.METADATA_BACKEND_SETTINGS_ACTIVITY) != null) {
			if (getPackageManager().resolveActivity(createSettingsIntent(backend.serviceInfo), 0) != null) {
				popUp.getMenu().add(Menu.NONE, 1, Menu.NONE, "Settings"); // TODO label
			}
		}
		popUp.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				popUp.dismiss();
				popUp = null;

				if (item.getItemId() == 0) {
					disabledBackend(backend.serviceInfo);
				} else if (item.getItemId() == 1) {
					startActivity(createSettingsIntent(backend.serviceInfo));
				}
				return true;
			}
		});
		popUp.show();
	}

	private class Adapter extends DynamicListView.StableArrayAdapter {
		public Adapter(List<ServiceInfo> backends) {
			super(LocationBackendConfig.this, R.layout.backend_list_entry, android.R.id.text2, backends);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
			final KnownBackend backend = knownBackends.get(getItem(position));
			ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
			icon.setImageDrawable(backend.icon);
			TextView title = (TextView) v.findViewById(android.R.id.text1);
			title.setText(backend.simpleName);
			TextView subtitle = (TextView) v.findViewById(android.R.id.text2);
			subtitle.setText(backend.serviceInfo.name);
			View overflow = v.findViewById(android.R.id.button1);
			overflow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showSettingsPopup(view, backend);
				}
			});
			return v;
		}
	}

	private class KnownBackend {
		private ServiceInfo serviceInfo;
		private String simpleName;
		private Drawable icon;

		private KnownBackend(ServiceInfo serviceInfo, String simpleName, Drawable icon) {
			this.serviceInfo = serviceInfo;
			this.simpleName = simpleName;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return simpleName;
		}
	}
}