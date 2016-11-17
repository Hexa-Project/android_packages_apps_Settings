/*
 * Copyright (C) 2016 The CyanogenMod project
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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

public class KangDroidNavBarSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String KEY_NAVIGATION_HOME_LONG_PRESS = "navigation_home_long_press";
    private static final String KEY_NAVIGATION_HOME_DOUBLE_TAP = "navigation_home_double_tap";
    private static final String KEY_NAVIGATION_RECENTS_LONG_PRESS = "navigation_recents_long_press";
    private static final String CATEGORY_NAVBAR = "navigation_bar_category";

    private SwitchPreference mDisableNavigationKeys;
    private SwitchPreference mNavigationBarLeftPref;
    private ListPreference mNavigationHomeLongPressAction;
    private ListPreference mNavigationHomeDoubleTapAction;
    private ListPreference mNavigationRecentsLongPressAction;

    private PreferenceCategory mNavigationPreferencesCat;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.kangdroid_nav_bar_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mHandler = new Handler();
		
		final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
		final int deviceKeys = getResources().getInteger(
			                com.android.internal.R.integer.config_deviceHardwareKeys);

        // Force Navigation bar related options
        mDisableNavigationKeys = (SwitchPreference) findPreference(DISABLE_NAV_KEYS);

        mNavigationPreferencesCat = (PreferenceCategory) findPreference(CATEGORY_NAVBAR);

        // Navigation bar left
        mNavigationBarLeftPref = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);

        Action defaultHomeLongPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_longPressOnHomeBehavior));
        Action defaultHomeDoubleTapAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHomeBehavior));
        Action homeLongPressAction = Action.fromSettings(resolver,
                CMSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                defaultHomeLongPressAction);
        Action homeDoubleTapAction = Action.fromSettings(resolver,
                CMSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                defaultHomeDoubleTapAction);

        // Navigation bar home long press
        mNavigationHomeLongPressAction = initActionList(KEY_NAVIGATION_HOME_LONG_PRESS,
                homeLongPressAction);

        // Navigation bar home double tap
        mNavigationHomeDoubleTapAction = initActionList(KEY_NAVIGATION_HOME_DOUBLE_TAP,
                homeDoubleTapAction);

        // Hide navigation bar home settings if we have a hardware home key
        // so that action config options aren't duplicated.
        if (hasHomeKey) {
                mNavigationPreferencesCat.removePreference(mNavigationHomeLongPressAction);
                mNavigationPreferencesCat.removePreference(mNavigationHomeDoubleTapAction);
        }

        // Navigation bar recents long press activity needs custom setup
        mNavigationRecentsLongPressAction =
                initRecentsLongPressAction(KEY_NAVIGATION_RECENTS_LONG_PRESS);

        final CMHardwareManager hardware = CMHardwareManager.getInstance(getActivity());

        // Only visible on devices that does not have a navigation bar already,
        // and don't even try unless the existing keys can be disabled
        boolean needsNavigationBar = false;
        if (hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE)) {
            try {
                IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                needsNavigationBar = wm.needsNavigationBar();
            } catch (RemoteException e) {
            }

            if (needsNavigationBar) {
                prefScreen.removePreference(mDisableNavigationKeys);
            } else {
                // Remove keys that can be provided by the navbar
                updateDisableNavkeysOption();
                mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
            }
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }

        try {
            // Only show the navigation bar category on devices that have a navigation bar
            // unless we are forcing it via development settings
            boolean forceNavbar = CMSettings.Global.getInt(getContentResolver(),
                    CMSettings.Global.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                    || forceNavbar;

            if (!hasNavBar && (needsNavigationBar ||
                    !hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE))) {
                    // Hide navigation bar category
                    prefScreen.removePreference(mNavigationPreferencesCat);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private ListPreference initActionList(String key, Action value) {
        return initActionList(key, value.ordinal());
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private ListPreference initRecentsLongPressAction(String key) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setOnPreferenceChangeListener(this);

        // Read the componentName from Settings.Secure, this is the user's prefered setting
        String componentString = CMSettings.Secure.getString(getContentResolver(),
                CMSettings.Secure.RECENTS_LONG_PRESS_ACTIVITY);
        ComponentName targetComponent = null;
        if (componentString == null) {
            list.setSummary(getString(R.string.hardware_keys_action_split_screen));
        } else {
            targetComponent = ComponentName.unflattenFromString(componentString);
        }

        // Dyanamically generate the list array,
        // query PackageManager for all Activites that are registered for ACTION_RECENTS_LONG_PRESS
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(cyanogenmod.content.Intent.ACTION_RECENTS_LONG_PRESS);
        List<ResolveInfo> recentsActivities = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (recentsActivities.size() == 0) {
            // No entries available, disable
            list.setSummary(getString(R.string.hardware_keys_action_split_screen));
            CMSettings.Secure.putString(getContentResolver(),
                    CMSettings.Secure.RECENTS_LONG_PRESS_ACTIVITY, null);
            list.setEnabled(false);
            return list;
        }

        CharSequence[] entries = new CharSequence[recentsActivities.size() + 1];
        CharSequence[] values = new CharSequence[recentsActivities.size() + 1];
        // First entry is always default last app
        entries[0] = getString(R.string.hardware_keys_action_split_screen);
        values[0] = "";
        list.setValue(values[0].toString());
        int i = 1;
        for (ResolveInfo info : recentsActivities) {
            try {
                // Use pm.getApplicationInfo for the label,
                // we cannot rely on ResolveInfo that comes back from queryIntentActivities.
                entries[i] = pm.getApplicationInfo(info.activityInfo.packageName, 0).loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error package not found: " + info.activityInfo.packageName, e);
                // Fallback to package name
                entries[i] = info.activityInfo.packageName;
            }

            // Set the value to the ComponentName that will handle this intent
            ComponentName entryComponent = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            values[i] = entryComponent.flattenToString();
            if (targetComponent != null) {
                if (entryComponent.equals(targetComponent)) {
                    // Update the selected value and the preference summary
                    list.setSummary(entries[i]);
                    list.setValue(values[i].toString());
                }
            }
            i++;
        }
        list.setEntries(entries);
        list.setEntryValues(values);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        CMSettings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleSystemActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavigationRecentsLongPressAction) {
            // RecentsLongPressAction is handled differently because it intentionally uses
            // Settings.System
            String putString = (String) newValue;
            int index = mNavigationRecentsLongPressAction.findIndexOfValue(putString);
            CharSequence summary = mNavigationRecentsLongPressAction.getEntries()[index];
            // Update the summary
            mNavigationRecentsLongPressAction.setSummary(summary);
            if (putString.length() == 0) {
                putString = null;
            }
            CMSettings.Secure.putString(getContentResolver(),
                    CMSettings.Secure.RECENTS_LONG_PRESS_ACTIVITY, putString);
            return true;
        }
        return false;
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        CMSettings.Global.putInt(context.getContentResolver(),
                CMSettings.Global.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
    }

    private void updateDisableNavkeysOption() {
        boolean enabled = CMSettings.Global.getInt(getActivity().getContentResolver(),
                CMSettings.Global.DEV_FORCE_SHOW_NAVBAR, 0) != 0;

        mDisableNavigationKeys.setChecked(enabled);
    }

    public static void restoreKeyDisabler(Context context) {
        CMHardwareManager hardware = CMHardwareManager.getInstance(context);
        if (!hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE)) {
            return;
        }

        writeDisableNavkeysOption(context, CMSettings.Global.getInt(context.getContentResolver(),
                CMSettings.Global.DEV_FORCE_SHOW_NAVBAR, 0) != 0);
    }


    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDisableNavigationKeys) {
            mDisableNavigationKeys.setEnabled(false);
            mNavigationPreferencesCat.setEnabled(false);
            writeDisableNavkeysOption(getActivity(), mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDisableNavigationKeys.setEnabled(true);
                    mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
                }
            }, 1000);
        }

        return super.onPreferenceTreeClick(preference);
    }
}
