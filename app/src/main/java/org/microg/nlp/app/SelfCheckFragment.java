/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.microg.nlp.app.tools.selfcheck.PermissionCheckGroup;
import org.microg.nlp.app.tools.selfcheck.SelfCheckGroup;
import org.microg.nlp.app.tools.ui.AbstractSelfCheckFragment;
import org.microg.nlp.app.tools.ui.AbstractSettingsActivity;
import org.microg.nlp.app.tools.selfcheck.NlpOsCompatChecks;
import org.microg.nlp.app.tools.selfcheck.NlpStatusChecks;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

public class SelfCheckFragment extends AbstractSelfCheckFragment {

    @Override
    protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
        if (SDK_INT > LOLLIPOP_MR1) {
            checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION));
        }
        checks.add(new NlpOsCompatChecks());
        checks.add(new NlpStatusChecks());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        reset(LayoutInflater.from(getContext()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        reset(LayoutInflater.from(getContext()));
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new SelfCheckFragment();
        }
    }
}
