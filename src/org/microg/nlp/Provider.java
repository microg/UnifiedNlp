package org.microg.nlp;

import android.os.IBinder;

public interface Provider {
	IBinder getBinder();

	void reload();
}
