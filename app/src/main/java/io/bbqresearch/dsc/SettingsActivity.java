package io.bbqresearch.dsc;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import io.bbqresearch.roomwordsample.R;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    //int GENERAL_PREF_FRAG_REQ_CODE = 2;

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        /*sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        this.getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
        //Intent intent = new Intent(SettingsActivity.this, GeneralPreferenceFragment.class);
        //startActivityForResult(intent, GENERAL_PREF_FRAG_REQ_CODE);

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /*   /**
     * {@inheritDoc}
     */
   /* @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    /*@Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }*/

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("settings");
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("airplane_mode"));
            Preference pref;

            pref = findPreference("alias");
            pref.setSummary(getPreferenceManager().getSharedPreferences().getString("alias", "AC"));
            bindPreferenceSummaryToValue(pref);

            pref = findPreference("btname");
            pref.setSummary(getPreferenceManager().getSharedPreferences().getString("btname", ""));
            bindPreferenceSummaryToValue(pref);

            pref = findPreference("btaddr");
            pref.setSummary(getPreferenceManager().getSharedPreferences().getString("btaddr", ""));
            bindPreferenceSummaryToValue(pref);

            String shared_val = getPreferenceManager().getSharedPreferences().getString("total_nodes", "2");
            pref = findPreference("total_nodes");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("tdma_slot", "0");
            pref = findPreference("tdma_slot");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("tx_time", "4");
            pref = findPreference("tx_time");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("tx_deadband", "1");
            pref = findPreference("tx_deadband");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("tx_power", "11");
            pref = findPreference("tx_power");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            pref = findPreference("freq");
            pref.setSummary(getPreferenceManager().getSharedPreferences().getString("freq", "915.000"));
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("bandwidth", "3");
            pref = findPreference("bandwidth");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("spread_factor", "10");
            pref = findPreference("spread_factor");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("coding_rate", "2");
            pref = findPreference("coding_rate");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);

            shared_val = getPreferenceManager().getSharedPreferences().getString("sync_word", "255");
            pref = findPreference("sync_word");
            pref.setSummary(shared_val);
            bindPreferenceSummaryToValue(pref);


            /*bindPreferenceSummaryToValue(findPreference("notifications_new_message"));
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_vibrate"));*/

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
