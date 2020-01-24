/*
 * SPDX-FileCopyrightText: 2012, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import com.android.internal.location.ProviderRequest;

import java.util.List;

/**
 * This class is an interface to Provider Requests for unbundled applications.
 * <p/>
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 */
public final class ProviderRequestUnbundled {
    public ProviderRequestUnbundled(ProviderRequest request) {
    }

    public boolean getReportLocation() {
        return false;
    }

    public long getInterval() {
        return 0;
    }

    /**
     * Never null.
     */
    public List<LocationRequestUnbundled> getLocationRequests() {
        return null;
    }
}
