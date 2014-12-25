package org.microg.nlp.api;

import android.location.Location;

interface LocationCallback {
    void report(in Location location);
}
