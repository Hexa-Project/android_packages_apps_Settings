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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;

public class KangDroidAOSPRecents extends SettingsPreferenceFragment implements Indexable, Preference.OnPreferenceChangeListener {

    private static final String IMMERSIVE_RECENTS = "immersive_recents";
	private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";

    private ListPreference mImmersiveRecents;
	private ListPreference mRecentsClearAllLocation;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.kangdroid_aosp_recents);
		
		PreferenceScreen prefSet = getPreferenceScreen();
		final ContentResolver resolver = getActivity().getContentResolver();
		
        mImmersiveRecents = (ListPreference) findPreference(IMMERSIVE_RECENTS);
        mImmersiveRecents.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.RECENTS_FULL_SCREEN, 0)));
        mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());
        mImmersiveRecents.setOnPreferenceChangeListener(this);
		
        // clear all location
        mRecentsClearAllLocation = (ListPreference) findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntry());
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);
    }
	
    @Override
    public void onResume() {
        super.onResume();
    }
	
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mImmersiveRecents) {
            Settings.System.putInt(getContentResolver(), Settings.System.RECENTS_FULL_SCREEN,
                    Integer.valueOf((String) newValue));
            mImmersiveRecents.setValue(String.valueOf(newValue));
            mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) newValue);
            int index = mRecentsClearAllLocation.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntries()[index]);
            return true;
        }
        return false;
    }
	
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }
	
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.kangdroid_aosp_recents;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return new ArrayList<String>();
                }
            };
}