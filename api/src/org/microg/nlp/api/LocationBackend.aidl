package org.microg.nlp.api;

import org.microg.nlp.api.LocationCallback;
import android.location.Location;

interface LocationBackend {
    void open(LocationCallback callback);
    Location update();
    void close();
}
