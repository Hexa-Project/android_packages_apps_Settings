/*
 * Copyright (C) 2014 The KangDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.kangdroid;

import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import android.telephony.TelephonyManager;
import com.android.settings.kangdroid.SeekBarPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;

public class KangDroidOtherSettings extends SettingsPreferenceFragment implements Indexable, Preference.OnPreferenceChangeListener {
	
	private static final String RR_INCALL = "rr_incall";
	
	private PreferenceScreen mIncall;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.kangdroid_other_settings);
		
        PreferenceScreen mIncall = (PreferenceScreen) findPreference(RR_INCALL);
        if (!isVoiceCapable(getActivity())) {
            getPreferenceScreen().removePreference(mIncall);
        }
    }
	
    @Override
    public void onResume() {
        super.onResume();
    }
	
    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }
	
	@Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
     }
	
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }
}