/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

@SuppressWarnings("unused")
public abstract class HelperLocationBackendService extends LocationBackendService {

    private boolean opened;
    private final Set<AbstractBackendHelper> helpers = new HashSet<>();

    public synchronized void addHelper(AbstractBackendHelper helper) {
        helpers.add(helper);
        if (opened) {
            helper.onOpen();
        }
    }

    public synchronized void removeHelpers() {
        if (opened) {
            for (AbstractBackendHelper helper : helpers) {
                helper.onClose();
            }
        }
        helpers.clear();
    }

    @Override
    protected synchronized void onOpen() {
        for (AbstractBackendHelper helper : helpers) {
            helper.onOpen();
        }
        opened = true;
    }

    @Override
    protected synchronized void onClose() {
        for (AbstractBackendHelper helper : helpers) {
            helper.onClose();
        }
        opened = false;
    }

    @Override
    protected synchronized Location update() {
        for (AbstractBackendHelper helper : helpers) {
            helper.onUpdate();
        }
        return null;
    }

    @Override
    protected Intent getInitIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Consider permissions
            List<String> perms = new LinkedList<>();
            for (AbstractBackendHelper helper : helpers) {
                perms.addAll(Arrays.asList(helper.getRequiredPermissions()));
            }
            // Request background location permission if needed as we are likely to run in background
            if (Build.VERSION.SDK_INT >= 29 && (perms.contains(ACCESS_COARSE_LOCATION) || perms.contains(ACCESS_FINE_LOCATION))) {
                perms.add(ACCESS_BACKGROUND_LOCATION);
            }
            for (Iterator<String> iterator = perms.iterator(); iterator.hasNext(); ) {
                String perm = iterator.next();
                if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                    iterator.remove();
                }
            }
            if (perms.isEmpty()) return null;
            Intent intent = new Intent(this, MPermissionHelperActivity.class);
            intent.putExtra(MPermissionHelperActivity.EXTRA_PERMISSIONS, perms.toArray(new String[0]));
            return intent;
        }
        return super.getInitIntent();
    }
}
