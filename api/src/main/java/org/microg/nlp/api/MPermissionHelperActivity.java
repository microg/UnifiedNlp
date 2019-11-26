/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.M)
public class MPermissionHelperActivity extends Activity {
    public static final String EXTRA_PERMISSIONS = "org.microg.nlp.api.mperms";
    private static final int REQUEST_CODE_PERMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] mperms = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
        if (mperms == null || mperms.length == 0) {
            setResult(RESULT_OK);
            finish();
        } else {
            requestPermissions(mperms, REQUEST_CODE_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean ok = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) ok = false;
        }
        setResult(ok ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
