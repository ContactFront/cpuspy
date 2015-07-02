//-----------------------------------------------------------------------------
//
// (C) Rob Beane, 2015 <robbeane@gmail.com>
//
//-----------------------------------------------------------------------------

package org.axdev.cpuspy.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.axdev.cpuspy.R;
import org.axdev.cpuspy.activity.PrefsActivity;
import org.axdev.cpuspy.utils.CPUUtils;
import org.axdev.cpuspy.utils.TypefaceHelper;
import org.axdev.cpuspy.utils.ThemeUtils;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class InfoFragment extends Fragment implements OnClickListener {

    @InjectView(R.id.btn_kernel_more) ImageButton mKernelMoreButton;
    @InjectView(R.id.kernel_header) TextView mKernelHeader;
    @InjectView(R.id.kernel_governor_header) TextView mKernelGovernorHeader;
    @InjectView(R.id.kernel_governor) TextView mKernelGovernor;
    @InjectView(R.id.kernel_version_header) TextView mKernelVersionHeader;
    @InjectView(R.id.kernel_version) TextView mKernelVersion;
    @InjectView(R.id.cpu_header) TextView mCpuHeader;
    @InjectView(R.id.cpu_abi_header) TextView mCpuAbiHeader;
    @InjectView(R.id.cpu_abi) TextView mCpuAbi;
    @InjectView(R.id.cpu_arch_header) TextView mCpuArchHeader;
    @InjectView(R.id.cpu_arch) TextView mCpuArch;
    @InjectView(R.id.cpu_core_header) TextView mCpuCoreHeader;
    @InjectView(R.id.cpu_core) TextView mCpuCore;
    @InjectView(R.id.cpu_freq_header) TextView mCpuFreqHeader;
    @InjectView(R.id.cpu_freq) TextView mCpuFreq;
    @InjectView(R.id.cpu_temp_header) TextView mCpuTempHeader;
    @InjectView(R.id.cpu_temp) TextView mCpuTemp;
    @InjectView(R.id.cpu_features_header) TextView mCpuFeaturesHeader;
    @InjectView(R.id.cpu_features) TextView mCpuFeatures;
    @InjectView(R.id.device_header) TextView mDeviceInfo;
    @InjectView(R.id.device_build_header) TextView mDeviceBuildHeader;
    @InjectView(R.id.device_build) TextView mDeviceBuild;
    @InjectView(R.id.device_api_header) TextView mDeviceApiHeader;
    @InjectView(R.id.device_api) TextView mDeviceApi;
    @InjectView(R.id.device_manuf_header) TextView mDeviceManufHeader;
    @InjectView(R.id.device_manuf) TextView mDeviceManuf;
    @InjectView(R.id.device_model_header) TextView mDeviceModelHeader;
    @InjectView(R.id.device_model) TextView mDeviceModel;
    @InjectView(R.id.device_board_header) TextView mDeviceBoardHeader;
    @InjectView(R.id.device_board) TextView mDeviceBoard;
    @InjectView(R.id.device_platform_header) TextView mDevicePlatformHeader;
    @InjectView(R.id.device_platform) TextView mDevicePlatform;

    @InjectView(R.id.cpu0_header) TextView mCore0Header;
    @InjectView(R.id.cpu1_header) TextView mCore1Header;
    @InjectView(R.id.cpu2_header) TextView mCore2Header;
    @InjectView(R.id.cpu3_header) TextView mCore3Header;
    @InjectView(R.id.cpu4_header) TextView mCore4Header;
    @InjectView(R.id.cpu5_header) TextView mCore5Header;
    @InjectView(R.id.cpu6_header) TextView mCore6Header;
    @InjectView(R.id.cpu7_header) TextView mCore7Header;
    @InjectView(R.id.cpu_freq0) TextView mCore0;
    @InjectView(R.id.cpu_freq1) TextView mCore1;
    @InjectView(R.id.cpu_freq2) TextView mCore2;
    @InjectView(R.id.cpu_freq3) TextView mCore3;
    @InjectView(R.id.cpu_freq4) TextView mCore4;
    @InjectView(R.id.cpu_freq5) TextView mCore5;
    @InjectView(R.id.cpu_freq6) TextView mCore6;
    @InjectView(R.id.cpu_freq7) TextView mCore7;

    private boolean mIsVisible;
    private boolean mIsMonitoringTemp;
    private boolean mIsMonitoringCpu;
    private boolean mHasCpu0;
    private boolean mHasCpu1;
    private boolean mHasCpu2;
    private boolean mHasCpu3;
    private boolean mHasCpu4;
    private boolean mHasCpu5;
    private boolean mHasCpu6;
    private boolean mHasCpu7;

    private final Handler mHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.info_layout, container, false);
        setHasOptionsMenu(true);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTextViews();

        /** Set color for drawables based on selected theme */
        final ColorStateList dark = ColorStateList.valueOf(getResources().getColor(R.color.drawable_color_dark));
        final ColorStateList light = ColorStateList.valueOf(getResources().getColor(R.color.drawable_color_light));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mKernelMoreButton.setImageTintList(ThemeUtils.DARKTHEME ? dark : light);
        } else {
            final Drawable kernelMoreButton = DrawableCompat.wrap(mKernelMoreButton.getDrawable());
            mKernelMoreButton.setImageDrawable(kernelMoreButton);
            DrawableCompat.setTintList(kernelMoreButton, (ThemeUtils.DARKTHEME ? dark : light));
        }

        /** Set onClickListener for kernel info button */
        mKernelMoreButton.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mIsVisible) setMonitoring(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mIsVisible) setMonitoring(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible()) {
            if (!isVisibleToUser) {
                mIsVisible = false;
                setMonitoring(false);
            }

            if (isVisibleToUser) {
                mIsVisible = true;
                setMonitoring(true);
            }
        }
    }

    private void setMonitoring(boolean enabled) {
        if (enabled) {
            checkTempMonitor();
            checkCoreMonitor();
        } else {
            mIsMonitoringCpu = false;
            mIsMonitoringTemp = false;
        }
    }

    private void setMediumTypeface(TextView tv) {
        // Applying Roboto-Medium font
        Typeface mediumFont;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediumFont = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            mediumFont = TypefaceHelper.get(getActivity(), TypefaceHelper.MEDIUM_FONT);
        }

        tv.setTypeface(mediumFont);
    }

    private void setTextViews() {
        /** Set text and typeface for TextViews */
        final String api = CPUUtils.getSystemProperty("ro.build.version.sdk");
        final String platform = CPUUtils.getSystemProperty("ro.board.platform");

        mKernelVersion.setText(System.getProperty("os.version"));
        mKernelGovernor.setText(CPUUtils.getGovernor());
        mCpuAbi.setText(Build.CPU_ABI);
        mCpuArch.setText(CPUUtils.getArch());
        mCpuCore.setText(Integer.toString(CPUUtils.getCoreCount()));
        mCpuFreq.setText(CPUUtils.getMinMax());
        mCpuFeatures.setText(CPUUtils.getFeatures());
        mDeviceBuild.setText(Build.ID);
        mDeviceApi.setText(api);
        mDeviceManuf.setText(Build.MANUFACTURER);
        mDeviceModel.setText(Build.MODEL);
        mDeviceBoard.setText(Build.BOARD);
        mDevicePlatform.setText(platform);

        setMediumTypeface(mKernelHeader);
        setMediumTypeface(mKernelGovernorHeader);
        setMediumTypeface(mKernelVersionHeader);
        setMediumTypeface(mCpuHeader);
        setMediumTypeface(mCpuAbiHeader);
        setMediumTypeface(mCpuArchHeader);
        setMediumTypeface(mCpuCoreHeader);
        setMediumTypeface(mCpuFreqHeader);
        setMediumTypeface(mCpuFeaturesHeader);
        setMediumTypeface(mDeviceInfo);
        setMediumTypeface(mDeviceBuildHeader);
        setMediumTypeface(mDeviceApiHeader);
        setMediumTypeface(mDeviceManufHeader);
        setMediumTypeface(mDeviceModelHeader);
        setMediumTypeface(mDeviceBoardHeader);
        setMediumTypeface(mDevicePlatformHeader);
    }

    /** Check if we should monitor cpu temp */
    private void checkTempMonitor() {
        if (CPUUtils.hasTemp()) {
            mIsMonitoringTemp = true;
            mHandler.post(monitorTemp);
            setMediumTypeface(mCpuTempHeader);
        } else {
            mIsMonitoringTemp = false;
            mCpuTempHeader.setVisibility(View.GONE);
            mCpuTemp.setVisibility(View.GONE);
        }
    }

    /** Monitor CPU temperature */
    private final Runnable monitorTemp = new Runnable() {
        public void run() {
            if (mIsMonitoringTemp) {
                try {
                    if (CPUUtils.getTemp() != null) {
                        mCpuTemp.setText(CPUUtils.getTemp());
                    } else {
                        mCpuTemp.setText(R.string.unavailable);
                        mCpuTemp.setTypeface(null, Typeface.ITALIC);
                    }
                } catch (NumberFormatException e) {
                    mCpuTemp = null;
                }
                mHandler.postDelayed(monitorTemp, 3000);
            }
        }
    };

    private final Runnable monitorCpu = new Runnable() {
        public void run() {
            if (mIsMonitoringCpu) {
                /** Set the frequency for CPU0 */
                if (mHasCpu0) {
                    try {
                        final File cpu0 = new File(CPUUtils.CPU0);
                        if (cpu0.length() == 0) {
                            // CPU0 should never be empty
                            mCore0.setText(null);
                            Log.e("CpuSpyInfo", "Problem getting CPU cores");
                            return;
                        } else {
                            mCore0.setText(CPUUtils.getCpu0());
                        }
                    } catch (NumberFormatException e) {
                        mCore0 = null;
                    }
                }
                /** Set the frequency for CPU1 */
                if (mHasCpu1) {
                    try {
                        final File cpu1 = new File(CPUUtils.CPU1);
                        if (cpu1.length() == 0) {
                            mCore1.setText(R.string.core_offline);
                        } else {
                            mCore1.setText(CPUUtils.getCpu1());
                        }
                    } catch (NumberFormatException e) {
                        mCore1 = null;
                    }
                }
                /** Set the frequency for CPU2 */
                if (mHasCpu2) {
                    try {
                        final File cpu2 = new File(CPUUtils.CPU2);
                        if (cpu2.length() == 0) {
                            mCore2.setText(R.string.core_offline);
                        } else {
                            mCore2.setText(CPUUtils.getCpu2());
                        }
                    } catch (NumberFormatException e) {
                        mCore2 = null;
                    }
                }
                /** Set the frequency for CPU3 */
                if (mHasCpu3) {
                    try {
                        final File cpu3 = new File(CPUUtils.CPU3);
                        if (cpu3.length() == 0) {
                            mCore3.setText(R.string.core_offline);
                        } else {
                            mCore3.setText(CPUUtils.getCpu3());
                        }
                    } catch (NumberFormatException e) {
                        mCore3 = null;
                    }
                }
                /** Set the frequency for CPU4 */
                if (mHasCpu4) {
                    try {
                        final File cpu4 = new File(CPUUtils.CPU4);
                        if (cpu4.length() == 0) {
                            mCore4.setText(R.string.core_offline);
                        } else {
                            mCore4.setText(CPUUtils.getCpu4());
                        }
                    } catch (NumberFormatException e) {
                        mCore4 = null;
                    }
                }
                /** Set the frequency for CPU5 */
                if (mHasCpu5) {
                    try {
                        final File cpu5 = new File(CPUUtils.CPU5);
                        if (cpu5.length() == 0) {
                            mCore5.setText(R.string.core_offline);
                        } else {
                            mCore5.setText(CPUUtils.getCpu5());
                        }
                    } catch (NumberFormatException e) {
                        mCore5 = null;
                    }
                }
                /** Set the frequency for CPU6 */
                if (mHasCpu6) {
                    try {
                        final File cpu6 = new File(CPUUtils.CPU6);
                        if (cpu6.length() == 0) {
                            mCore6.setText(R.string.core_offline);
                        } else {
                            mCore6.setText(CPUUtils.getCpu6());
                        }
                    } catch (NumberFormatException e) {
                        mCore6 = null;
                    }
                }
                /** Set the frequency for CPU7 */
                if (mHasCpu7) {
                    try {
                        final File cpu7 = new File(CPUUtils.CPU7);
                        if (cpu7.length() == 0) {
                            mCore7.setText(R.string.core_offline);
                        } else {
                            mCore7.setText(CPUUtils.getCpu7());
                        }
                    } catch (NumberFormatException e) {
                        mCore7 = null;
                    }
                }

                mHandler.postDelayed(monitorCpu, 1000); // 1 second
            }
        }
    };

    /** Check which CPU cores to start monitoring */
    private void checkCoreMonitor() {
        switch (CPUUtils.getCoreCount()) {
            case 1:
                mCore0Header.setVisibility(View.VISIBLE);
                mCore0.setVisibility(View.VISIBLE);
                mHasCpu0 = true;
                break;
            case 2:
                mCore0Header.setVisibility(View.VISIBLE);
                mCore0.setVisibility(View.VISIBLE);
                mHasCpu0 = true;

                mCore1Header.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mHasCpu1 = true;
                break;
            case 4:
                mCore0Header.setVisibility(View.VISIBLE);
                mCore0.setVisibility(View.VISIBLE);
                mHasCpu0 = true;

                mCore1Header.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mHasCpu1 = true;

                mCore2Header.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.VISIBLE);
                mHasCpu2 = true;

                mCore3Header.setVisibility(View.VISIBLE);
                mCore3.setVisibility(View.VISIBLE);
                mHasCpu3 = true;
                break;
            case 6:
                mCore0Header.setVisibility(View.VISIBLE);
                mCore0.setVisibility(View.VISIBLE);
                mHasCpu0 = true;

                mCore1Header.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mHasCpu1 = true;

                mCore2Header.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.VISIBLE);
                mHasCpu2 = true;

                mCore3Header.setVisibility(View.VISIBLE);
                mCore3.setVisibility(View.VISIBLE);
                mHasCpu3 = true;

                mCore4Header.setVisibility(View.VISIBLE);
                mCore4.setVisibility(View.VISIBLE);
                mHasCpu4 = true;

                mCore5Header.setVisibility(View.VISIBLE);
                mCore5.setVisibility(View.VISIBLE);
                mHasCpu5 = true;
                break;
            case 8:
                mCore0Header.setVisibility(View.VISIBLE);
                mCore0.setVisibility(View.VISIBLE);
                mHasCpu0 = true;

                mCore1Header.setVisibility(View.VISIBLE);
                mCore1.setVisibility(View.VISIBLE);
                mHasCpu1 = true;

                mCore2Header.setVisibility(View.VISIBLE);
                mCore2.setVisibility(View.VISIBLE);
                mHasCpu2 = true;

                mCore3Header.setVisibility(View.VISIBLE);
                mCore3.setVisibility(View.VISIBLE);
                mHasCpu3 = true;

                mCore4Header.setVisibility(View.VISIBLE);
                mCore4.setVisibility(View.VISIBLE);
                mHasCpu4 = true;

                mCore5Header.setVisibility(View.VISIBLE);
                mCore5.setVisibility(View.VISIBLE);
                mHasCpu5 = true;

                mCore6Header.setVisibility(View.VISIBLE);
                mCore6.setVisibility(View.VISIBLE);
                mHasCpu6 = true;

                mCore7Header.setVisibility(View.VISIBLE);
                mCore7.setVisibility(View.VISIBLE);
                mHasCpu7 = true;
                break;
        }
        mIsMonitoringCpu = true;
        mHandler.post(monitorCpu);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_kernel_more:
                final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .content(CPUUtils.getKernelVersion())
                        .build();

                dialog.getWindow().getAttributes().windowAnimations = R.style.DialogInfoAnimation;
                dialog.show();
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    /** called to handle a menu event */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // what it do mayne
        switch (item.getItemId()) {
        /* pressed the load menu button */
            case R.id.menu_settings:
                this.startActivity(new Intent(getActivity(), PrefsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setMonitoring(false);
        ButterKnife.reset(this);
    }
}
