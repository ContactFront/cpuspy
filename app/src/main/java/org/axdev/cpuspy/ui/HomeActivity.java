//-----------------------------------------------------------------------------
//
// (C) Brandon Valosek, 2011 <bvalosek@gmail.com>
// (C) Rob Beane, 2015 <robbeane@gmail.com>
//
//-----------------------------------------------------------------------------

package org.axdev.cpuspy.ui;

// imports

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;

import com.crashlytics.android.Crashlytics;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.axdev.cpuspy.CpuSpyApp;
import org.axdev.cpuspy.CpuStateMonitor;
import org.axdev.cpuspy.CpuStateMonitor.CpuState;
import org.axdev.cpuspy.CpuStateMonitor.CpuStateMonitorException;
import org.axdev.cpuspy.fragments.WhatsNewDialog;
import org.axdev.cpuspy.utils.TypefaceSpan;
import org.axdev.cpuspy.R;
import org.axdev.cpuspy.utils.ThemeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

import io.fabric.sdk.android.Fabric;

/** main activity class */
public class HomeActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener, OnClickListener
{
    private static final String TAG = "CpuSpy";

    private final String VERSION_KEY = "version_number";
    private final String WELCOME_SCREEN = "welcomeScreenShown";

    private final String CPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    private final String CPU1 = "/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq";
    private final String CPU2 = "/sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq";
    private final String CPU3 = "/sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq";

    private final Handler mHandler = new Handler();

    private CpuSpyApp _app = null;
    private SwipeRefreshLayout mSwipeLayout;

    // main ui views
    @InjectView(R.id.btn_welcome) Button mWelcomeButton;
    @InjectView(R.id.card_view_states) CardView mStatesCardView;
    @InjectView(R.id.card_view_welcome) CardView mWelcomeCardView;
    @InjectView(R.id.card_view_time) CardView mTimeCardView;
    @InjectView(R.id.btn_info) ImageButton mInfoButton;
    @InjectView(R.id.btn_info_dark) ImageButton mInfoButtonDark;
    @InjectView(R.id.img_show) ImageView mShowImage;
    @InjectView(R.id.img_show_dark) ImageView mShowImageDark;
    @InjectView(R.id.ui_states_view) LinearLayout mStatesView;
    @InjectView(R.id.ui_charged_view) LinearLayout mChargedView;
    @InjectView(R.id.ui_states_warning) LinearLayout mStatesWarning;
    @InjectView(R.id.additional_layout) LinearLayout mAdditionalLayout;
    @InjectView(R.id.ui_additional_states) TextView mAdditionalStates;
    @InjectView(R.id.ui_additional_states_show) TextView mAdditionalStatesShow;
    @InjectView(R.id.ui_additional_states_hide) TextView mAdditionalStatesHide;
    @InjectView(R.id.ui_total_state_time) TextView mTotalStateTime;
    @InjectView(R.id.ui_header_total_state_time) TextView mHeaderTotalStateTime;

    @Optional @InjectView(R.id.ripple_main) MaterialRippleLayout mMaterialRippleLayout;

    @InjectView(R.id.ui_cpu_freq0) TextView mCore0;
    @InjectView(R.id.ui_cpu_freq1) TextView mCore1;
    @InjectView(R.id.ui_cpu_freq2) TextView mCore2;
    @InjectView(R.id.ui_cpu_freq3) TextView mCore3;

    private String mFreq0;
    private String mFreq1;
    private String mFreq2;
    private String mFreq3;

    private boolean mMonitorCpu0;
    private boolean mMonitorCpu1;
    private boolean mMonitorCpu2;
    private boolean mMonitorCpu3;

    /** whether or not auto refresh is enabled */
    private boolean mAutoRefresh = false;

    /** lets us know if the battery is fully charged or not */
    private boolean mIsCharged = false;

    /** Initialize the Activity */
    @Override public void onCreate(Bundle savedInstanceState)
    {
        if (Build.VERSION.SDK_INT >= 21) {
            ThemeUtils.onActivityCreateSetNavBar(this);
        }
        ThemeUtils.onActivityCreateSetTheme(this);

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // inflate the view, stash the app context, and get all UI elements
        setContentView(R.layout.home_layout);
        _app = (CpuSpyApp)getApplicationContext();
        ButterKnife.inject(this);
        checkVersion();
        startCoreMonitor();
        cardViewAnimation();
        setTypeface();

        // second argument is the default to use if the preference can't be found
        boolean welcomeScreenShown = sp.getBoolean(WELCOME_SCREEN, false);

        if (!welcomeScreenShown) {
            mWelcomeCardView.setVisibility(View.VISIBLE);
        }

        // Use custom Typeface for action bar title on KitKat devices
        if (Build.VERSION.SDK_INT == 19) {
            SpannableString s = new SpannableString(getResources().getString(R.string.app_name_long));
            s.setSpan(new TypefaceSpan(this, "Roboto-Medium.ttf"), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Update the action bar title with the TypefaceSpan instance
            getSupportActionBar().setTitle(s);
        } else {
            getSupportActionBar().setTitle(R.string.app_name_long);
            getSupportActionBar().setElevation(0);
        }

        // Set UI elements for dark and light themes
        if (sp.getBoolean("darkTheme", true)) {
            mStatesCardView.setCardBackgroundColor(getResources().getColor(R.color.card_dark_background));
            mTimeCardView.setCardBackgroundColor(getResources().getColor(R.color.card_dark_background));
            mTotalStateTime.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mAdditionalStatesHide.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mAdditionalStatesShow.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mAdditionalStates.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mAdditionalLayout.setBackgroundColor(getResources().getColor(R.color.layout_dark_background));
            mShowImageDark.setVisibility(View.VISIBLE);
            mInfoButtonDark.setVisibility(View.VISIBLE);
            mShowImage = mShowImageDark;
            mInfoButton = mInfoButtonDark;
            if (Build.VERSION.SDK_INT == 19) {
                mMaterialRippleLayout.setRippleColor(getResources().getColor(R.color.ripple_material_dark));
            }
        } else {
            mStatesCardView.setCardBackgroundColor(getResources().getColor(R.color.card_light_background));
            mTimeCardView.setCardBackgroundColor(getResources().getColor(R.color.card_light_background));
            mTotalStateTime.setTextColor(getResources().getColor(R.color.primary_text_color));
            mAdditionalStatesHide.setTextColor(getResources().getColor(R.color.primary_text_color));
            mAdditionalStatesShow.setTextColor(getResources().getColor(R.color.primary_text_color));
            mAdditionalStates.setTextColor(getResources().getColor(R.color.primary_text_color));
            mAdditionalLayout.setBackgroundColor(getResources().getColor(R.color.layout_light_background));
            mShowImage.setVisibility(View.VISIBLE);
            mInfoButton.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT == 19) {
                mMaterialRippleLayout.setRippleColor(getResources().getColor(R.color.ripple_material_light));
            }
        }

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeColors(getResources().getColor(android.R.color.white));
        mSwipeLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.primary));

        // Set onClickListener for all buttons
        mInfoButton.setOnClickListener(this);
        mWelcomeButton.setOnClickListener(this);
        mStatesCardView.setOnClickListener(this);

        // Register receiver
        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override public void onStart () {
        super.onStart();
        checkAutoRefresh();
    }

    /** Disable auto refresh when app is in the background */
    @Override public void onPause () {
        super.onPause();
        mAutoRefresh = false;
    }

    /** Update the view when the application regains focus */
    @Override public void onResume () {
        super.onResume();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // Recreate activity if navigation bar or theme changes
        if (PrefsActivity.mThemeChanged || PrefsActivity.mNavBarChanged) {
            this.recreate();
            PrefsActivity.mThemeChanged = false;
            if (Build.VERSION.SDK_INT >= 21) {
                PrefsActivity.mNavBarChanged = false;
            }
        } else {
            if (!mAutoRefresh) { refreshData(); }
        }

        // Initialize and start automatic crash reporting
        if(sp.getBoolean("crashReport", true)) {
            Fabric.with(this, new Crashlytics());
        }
    }

    @Override public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeLayout.setRefreshing(false);
                refreshData();
            }
        }, 1950);
    }

    /** Check to see if the device is fully charged or not */
    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int percent = (level * 100) / scale;
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            mIsCharged = percent >= 97 && isCharging;

            if (sp.getBoolean("autoReset", true) || mIsCharged) {
                if (!mAutoRefresh) { updateView(); }
            }
        }
    };

    /** Show WhatsNewDialog if versionCode has changed */
    private void checkVersion() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int currentVersionNumber = 0;
        int savedVersionNumber = sp.getInt(VERSION_KEY, 0);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        } catch (Exception ignored) {}

        if (currentVersionNumber > savedVersionNumber) {
            showWhatsNewDialog();
            Editor editor = sp.edit();
            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.commit();
        }
    }

    private void showWhatsNewDialog() {
        WhatsNewDialog newFragment = new WhatsNewDialog();
        newFragment.show(getFragmentManager(), "whatsnew");
    }

    /** Check to see if autoRefresh is enabled or not **/
    private void checkAutoRefresh() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (sp.getBoolean("autoRefresh", true)) {
            mAutoRefresh = true;
            mHandler.post(refreshAuto);
        } else {
            mAutoRefresh = false;
        }
    }

    /** Animate cardview sliding up from bottom */
    private void cardViewAnimation() {
        Animation slideUp = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_in_up);

        slideUp.setDuration(750);
        mStatesCardView.startAnimation(slideUp);
        mTimeCardView.startAnimation(slideUp);
    }

    /** Global On click listener for all views */
    @Override
    public void onClick(View v) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        Intent intent;

        switch (v.getId()) {
            case R.id.btn_info:
                intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_info_dark:
                intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_welcome:
                // setVisibility to GONE before removing to allow animations
                mWelcomeCardView.setVisibility(View.GONE);
                mWelcomeCardView.removeAllViews();

                editor.putBoolean(WELCOME_SCREEN, true);
                editor.commit();
                break;
            case R.id.card_view_states:
                if (mAdditionalStatesShow.isShown()) {
                    AnimationSet animSet = new AnimationSet(true);
                    animSet.setInterpolator(new DecelerateInterpolator());
                    animSet.setFillAfter(true);
                    animSet.setFillEnabled(true);

                    final RotateAnimation animRotate = new RotateAnimation(0.0f, 180.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);

                    animRotate.setDuration(300);
                    animRotate.setFillAfter(true);
                    animSet.addAnimation(animRotate);

                    mShowImage.startAnimation(animSet);

                    mAdditionalStatesShow.setVisibility(View.GONE);
                    mAdditionalStatesHide.setVisibility(View.VISIBLE);
                    mAdditionalStates.setVisibility(View.VISIBLE);
                } else {
                    AnimationSet animSet = new AnimationSet(true);
                    animSet.setInterpolator(new DecelerateInterpolator());
                    animSet.setFillAfter(true);
                    animSet.setFillEnabled(true);

                    final RotateAnimation animRotate = new RotateAnimation(-180.0f, 0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);

                    animRotate.setDuration(500);
                    animRotate.setFillAfter(true);
                    animSet.addAnimation(animRotate);

                    mShowImage.startAnimation(animSet);

                    mAdditionalStatesShow.setVisibility(View.VISIBLE);
                    mAdditionalStatesHide.setVisibility(View.GONE);
                    mAdditionalStates.setVisibility(View.GONE);
                }
                break;
        }
    }

    /** Get the current frequency for CPU0 */
    private String getCpu0() {
        try {
            InputStream is = new FileInputStream(CPU0);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine())!= null ) {
                mFreq0 = line;
            }

            is.close();
        } catch (IOException ignored) {}

        // made it
        return mFreq0;
    }

    /** Get the current frequency for CPU1 */
    private String getCpu1() {
        try {
            InputStream is = new FileInputStream(CPU1);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine())!= null ) {
                mFreq1 = line;
            }

            is.close();
        } catch (IOException ignored) {}

        // made it
        return mFreq1;
    }

    /** Get the current frequency for CPU2 */
    private String getCpu2() {
        try {
            InputStream is = new FileInputStream(CPU2);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine())!= null ) {
                mFreq2 = line;
            }

            is.close();
        } catch (IOException ignored) {}

        // made it
        return mFreq2;
    }

    /** Get the current frequency for CPU3 */
    private String getCpu3() {
        try {
            InputStream is = new FileInputStream(CPU3);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine())!= null ) {
                mFreq3 = line;
            }

            is.close();
        } catch (IOException ignored) {}

        // made it
        return mFreq3;
    }

    private final Runnable monitorCpu = new Runnable() {
        public void run() {
            File cpu0 = new File(CPU0);
            File cpu1 = new File(CPU1);
            File cpu2 = new File(CPU2);
            File cpu3 = new File(CPU3);

            /** Set the frequency for CPU0 */
            if(mMonitorCpu0) {
                try {
                    if (cpu0.length() == 0) {
                        // CPU0 should never be empty
                        mCore0.setText(null);
                        Log.e(TAG, "Problem getting CPU cores");
                        return;
                    } else {
                        int i = Integer.parseInt(getCpu0()) / 1000;
                        String s = String.valueOf(i) + "MHz";
                        mCore0.setText(s);
                    }
                } catch (NumberFormatException ignored) {
                    //DO SOMETHING
                }
            }

            /** Set the frequency for CPU1 */
            if(mMonitorCpu1) {
                try {
                    if (cpu1.length() == 0) {
                        mCore1.setText(R.string.core_offline);
                    } else {
                        int i = Integer.parseInt(getCpu1()) / 1000;
                        String s = String.valueOf(i) + "MHz";
                        mCore1.setText(s);
                    }
                } catch (NumberFormatException ignored) {
                    // DO SOMETHING
                }
            }

            /** Set the frequency for CPU2 */
            if(mMonitorCpu2) {
                try {
                    if (cpu2.length() == 0) {
                        mCore2.setText(R.string.core_offline);
                    } else {
                        int i = Integer.parseInt(getCpu2()) / 1000;
                        String s = String.valueOf(i) + "MHz";
                        mCore2.setText(s);
                    }
                } catch (NumberFormatException ignored) {
                    // DO SOMETHING
                }
            }

            /** Set the frequency for CPU3 */
            if(mMonitorCpu3) {
                try {
                    if (cpu3.length() == 0) {
                        mCore3.setText(R.string.core_offline);
                    } else {
                        int i = Integer.parseInt(getCpu3()) / 1000;
                        String s = String.valueOf(i) + "MHz";
                        mCore3.setText(s);
                    }
                } catch (NumberFormatException ignored) {
                    //DO SOMETHING
                }
            }

            mHandler.postDelayed(monitorCpu, 1000); // 1 second
        }
    };

    /** Check which CPU cores to start monitoring */
    private void startCoreMonitor() {
        int numCores = Runtime.getRuntime().availableProcessors();
        switch (numCores) {
            case 1:
                mMonitorCpu0 = true;
                mMonitorCpu1 = false;
                mMonitorCpu2 = false;
                mMonitorCpu3 = false;

                mCore0.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.GONE);
                mCore2.setVisibility(View.GONE);
                mCore3.setVisibility(View.GONE);
                break;
            case 2:
                mMonitorCpu0 = true;
                mMonitorCpu1 = true;
                mMonitorCpu2 = false;
                mMonitorCpu3 = false;

                mCore0.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.GONE);
                mCore3.setVisibility(View.GONE);
                break;
            case 3:
                mMonitorCpu0 = true;
                mMonitorCpu1 = true;
                mMonitorCpu2 = true;
                mMonitorCpu3 = false;

                mCore0.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.VISIBLE);
                mCore3.setVisibility(View.GONE);
                break;
            case 4:
                mMonitorCpu0 = true;
                mMonitorCpu1 = true;
                mMonitorCpu2 = true;
                mMonitorCpu3 = true;

                mCore0.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.VISIBLE);
                mCore3.setVisibility(View.VISIBLE);
                break;
        }
        mHandler.post(monitorCpu);
    }

    /** Apply custom typeface to textviews */
    private void setTypeface() {
        // Loading Font Face
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");

        TextView mWelcomeSummary = (TextView)findViewById(R.id.welcome_summary);
        TextView mWelcomeFeatures = (TextView)findViewById(R.id.welcome_features);

        // Apply roboto medium typeface
        mWelcomeSummary.setTypeface(tf);
        mWelcomeFeatures.setTypeface(tf);
        mAdditionalStatesShow.setTypeface(tf);
        mAdditionalStatesHide.setTypeface(tf);
        mHeaderTotalStateTime.setTypeface(tf);
    }

    /** called when we want to inflate the menu */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // request inflater from activity and inflate into its menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        inflater.inflate(R.menu.prefs_menu, menu);

        // made it
        return true;
    }

    /** called to handle a menu event */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // what it do mayne
        switch (item.getItemId()) {
        /* pressed the load menu button */
            case R.id.menu_reset:
                try {
                    _app.getCpuStateMonitor().setOffsets();
                } catch (CpuStateMonitorException e) {
                    // TODO: something
                }
                _app.saveOffsets();
                updateView();
                SnackbarManager.show(Snackbar.with(this)
                        .text(R.string.snackbar_text_reset) // text to display
                        .actionLabel(R.string.action_dismiss) // action button label
                        .actionColor(Color.parseColor("#f4b400")));
                mStatesCardView.setVisibility(View.GONE);
                break;
            case R.id.menu_restore:
                _app.getCpuStateMonitor().removeOffsets();
                _app.saveOffsets();
                updateView();
                SnackbarManager.show(Snackbar.with(this)
                        .text(R.string.snackbar_text_restore) // text to display
                        .actionLabel(R.string.action_dismiss) // action button label
                        .actionColor(Color.parseColor("#f4b400")));
                break;
            case R.id.menu_settings:
                Intent intent = new Intent(this, PrefsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Generate and update all UI elements */
    private void updateView() {
        /** Get the CpuStateMonitor from the app, and iterate over all states,
         * creating a row if the duration is > 0 or otherwise marking it in
         * extraStates (missing) */

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        CpuStateMonitor monitor = _app.getCpuStateMonitor();
        mStatesView.removeAllViews();
        List<String> extraStates = new ArrayList<>();
        for (CpuState state : monitor.getStates()) {
            if (state.duration > 0) {
                generateStateRow(state, mStatesView);
            } else {
                if (state.freq == 0) {
                    extraStates.add("Deep Sleep");
                } else {
                    extraStates.add(state.freq / 1000 + "MHz");
                }
            }
        }

        // update the total state time
        long totTime = monitor.getTotalStateTime() / 100;
        mTotalStateTime.setText(sToString(totTime));

        // for all the 0 duration states, add the the Unused State area
        if (extraStates.size() > 0) {
            int n = 0;
            String str = "";

            for (String s : extraStates) {
                if (n++ > 0)
                    str += ", ";
                str += s;
            }
            mAdditionalStates.setText(str);
        } else {
            mAdditionalStates.setText("Empty");
        }


        /** Reset timers and show info when battery is charged */
        if (sp.getBoolean("autoReset", true) && mIsCharged) {
            mStatesWarning.setVisibility(View.GONE);
            mStatesCardView.setVisibility(View.GONE);
            mTimeCardView.setVisibility(View.GONE);
            mWelcomeCardView.setVisibility(View.GONE);
            mChargedView.setVisibility(View.VISIBLE);

            try {
                _app.getCpuStateMonitor().setOffsets();
            } catch (CpuStateMonitorException e) {
                // TODO: something
            }
            _app.saveOffsets();
        } else {
            mStatesWarning.setVisibility(View.GONE);
            mChargedView.setVisibility(View.GONE);
            mStatesCardView.setVisibility(View.VISIBLE);
            mTimeCardView.setVisibility(View.VISIBLE);
        }

        /** show warning label if no states found */
        if (monitor.getStates().size() == 0) {
            mStatesWarning.setVisibility(View.VISIBLE);
            mTimeCardView.setVisibility(View.GONE);
            mWelcomeCardView.setVisibility(View.GONE);
            mStatesCardView.setVisibility(View.GONE);
            mChargedView.setVisibility(View.GONE);
        }
    }

    /** Attempt to update the time-in-state info */
    private void refreshData() {
        new RefreshStateDataTask().execute((Void) null);
    }

    /** @return A nicely formatted String representing tSec seconds */
    private static String sToString(long tSec) {
        long h = (long)Math.floor(tSec / (60*60));
        long m = (long)Math.floor((tSec - h*60*60) / 60);
        long s = tSec % 60;
        String sDur;
        sDur = h + ":";
        if (m < 10)
            sDur += "0";
        sDur += m + ":";
        if (s < 10)
            sDur += "0";
        sDur += s;

        return sDur;
    }

    /**
     * @return a View that correpsonds to a CPU freq state row as specified
     * by the state parameter
     */
    private View generateStateRow(CpuState state, ViewGroup parent) {
        // inflate the XML into a view in the parent
        LayoutInflater inf = LayoutInflater.from(_app);
        RelativeLayout theRow = (RelativeLayout)inf.inflate(
                R.layout.state_row, parent, false);

        // what percentage we've got
        CpuStateMonitor monitor = _app.getCpuStateMonitor();
        float per = (float)state.duration * 100 /
                monitor.getTotalStateTime();
        String sPer = String.format("%.01f", per) + "%";

        // state name
        String sFreq;
        if (state.freq == 0) {
            sFreq = "Deep Sleep";
        } else {
            sFreq = state.freq / 1000 + "MHz";
        }

        // duration
        long tSec = state.duration / 100;
        String sDur = sToString(tSec);

        // map UI elements to objects
        TextView mFreqText = (TextView)theRow.findViewById(R.id.ui_freq_text);
        TextView mDurText = (TextView)theRow.findViewById(
                R.id.ui_duration_text);
        TextView mPerText = (TextView)theRow.findViewById(
                R.id.ui_percentage_text);
        ProgressBar mBar = (ProgressBar)theRow.findViewById(R.id.ui_bar);
        ProgressBar mBarDark = (ProgressBar)theRow.findViewById(R.id.ui_bar_dark);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (sp.getBoolean("darkTheme", true)) {
            mFreqText.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mDurText.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mPerText.setTextColor(getResources().getColor(R.color.primary_text_color_dark));
            mBarDark.setVisibility(View.VISIBLE);
            mBar = mBarDark;
        } else {
            mFreqText.setTextColor(getResources().getColor(R.color.primary_text_color));
            mDurText.setTextColor(getResources().getColor(R.color.primary_text_color));
            mPerText.setTextColor(getResources().getColor(R.color.primary_text_color));
            mBar.setVisibility(View.VISIBLE);
        }

        // modify the row
        mFreqText.setText(sFreq);
        mPerText.setText(sPer);
        mDurText.setText(sDur);
        mBar.setProgress(Math.round(per));

        // add it to parent and return
        parent.addView(theRow);
        return theRow;
    }

    /** Keep updating the state data off the UI thread for slow devices */
    private class RefreshStateDataTask extends AsyncTask<Void, Void, Void> {

        /** Stuff to do on a seperate thread */
        @Override protected Void doInBackground(Void... v) {
            CpuStateMonitor monitor = _app.getCpuStateMonitor();
            try {
                monitor.updateStates();
            } catch (CpuStateMonitorException e) {
                Log.e(TAG, "Problem getting CPU states");
            }

            return null;
        }

        /** Executed on the UI thread right before starting the task */
        @Override protected void onPreExecute() {}

        /** Executed on UI thread after task */
        @Override protected void onPostExecute(Void v) {
            updateView();
        }
    }

    /** Update data every 1 second if auto refresh is enabled */
    private final Runnable refreshAuto = new Runnable() {
        public void run() {
            if(mAutoRefresh) {
                refreshData();
                mHandler.postDelayed(refreshAuto, 1000); // 1 second
            }
        }
    };

    @Override protected void onDestroy() {
        super.onDestroy();
        mAutoRefresh = false;
        mMonitorCpu0 = false;
        mMonitorCpu1 = false;
        mMonitorCpu2 = false;
        mMonitorCpu2 = false;
        this.unregisterReceiver(this.mBatInfoReceiver); // unregister receiver
    }
}
