package org.microg.nlp.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.microg.nlp.api.NlpApiConstants;

import java.util.ArrayList;
import java.util.List;

public class LocationBackendConfig extends ListActivity {

	private List<String> activeBackends;
	private Adapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activeBackends = new ArrayList<String>();
		List<KnownBackend> backends = new ArrayList<KnownBackend>();
		Intent intent = new Intent(NlpApiConstants.ACTION_LOCATION_BACKEND);
		List<ResolveInfo> resolveInfos = getPackageManager().queryIntentServices(intent, 0);
		for (ResolveInfo info : resolveInfos) {
			String packageName = info.serviceInfo.packageName;
			String simpleName = String.valueOf(info.serviceInfo.loadLabel(getPackageManager()));
			Drawable icon = info.serviceInfo.loadIcon(getPackageManager());
			backends.add(new KnownBackend(packageName, simpleName, icon));
		}
		adapter = new Adapter(backends);
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		KnownBackend backend = adapter.getItem(position);
		if (activeBackends.contains(backend.packageName)) {
			activeBackends.remove(backend.packageName);
		} else {
			activeBackends.add(backend.packageName);
		}
		adapter.notifyDataSetChanged();
	}

	private class Adapter extends ArrayAdapter<KnownBackend> {
		public Adapter(List<KnownBackend> backends) {
			super(LocationBackendConfig.this, android.R.layout.select_dialog_multichoice, android.R.id.text1, backends);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// User super class to create the View
			View v = super.getView(position, convertView, parent);
			CheckedTextView tv = (CheckedTextView) v.findViewById(android.R.id.text1);

			// Put the image on the TextView
			tv.setCompoundDrawablesWithIntrinsicBounds(getItem(position).icon, null,
					null, null);

			// Add margin between image and text (support various screen densities)
			int dp10 = (int) (10 * getContext().getResources().getDisplayMetrics().density + 0.5f);
			tv.setCompoundDrawablePadding(dp10);

			tv.setChecked(activeBackends.contains(getItem(position).packageName));

			return v;
		}
	}

	private class KnownBackend {
		private String packageName;
		private String simpleName;
		private Drawable icon;

		public KnownBackend(String packageName, String simpleName, Drawable icon) {
			this.packageName = packageName;
			this.simpleName = simpleName;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return simpleName;
		}
	}
}