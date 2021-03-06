//-----------------------------------------------------------------------------
//
// (C) Rob Beane, 2015 <robbeane@gmail.com>
//
//-----------------------------------------------------------------------------

package org.axdev.cpuspy.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.axdev.cpuspy.R;
import org.axdev.cpuspy.activity.ThemedActivity;
import org.axdev.cpuspy.utils.CPUUtils;
import org.axdev.cpuspy.utils.TypefaceHelper;
import org.axdev.cpuspy.utils.Utils;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class InfoFragment extends Fragment {

    @Bind(R.id.card_view_kernelfull) CardView mCardKernelFull;
    @Bind(R.id.kernel_header) TextView mKernelHeader;
    @Bind(R.id.kernel_governor_header) TextView mKernelGovernorHeader;
    @Bind(R.id.kernel_governor) TextView mKernelGovernor;
    @Bind(R.id.kernel_version_header) TextView mKernelVersionHeader;
    @Bind(R.id.kernel_version) TextView mKernelVersion;
    @Bind(R.id.kernel_version_full_header) TextView mKernelVersionFullHeader;
    @Bind(R.id.kernel_version_full) TextView mKernelVersionFull;
    @Bind(R.id.cpu_header) TextView mCpuHeader;
    @Bind(R.id.cpu_abi_header) TextView mCpuAbiHeader;
    @Bind(R.id.cpu_abi) TextView mCpuAbi;
    @Bind(R.id.cpu_arch_header) TextView mCpuArchHeader;
    @Bind(R.id.cpu_arch) TextView mCpuArch;
    @Bind(R.id.cpu_core_header) TextView mCpuCoreHeader;
    @Bind(R.id.cpu_core) TextView mCpuCore;
    @Bind(R.id.cpu_freq_header) TextView mCpuFreqHeader;
    @Bind(R.id.cpu_freq) TextView mCpuFreq;
    @Bind(R.id.cpu_temp_header) TextView mCpuTempHeader;
    @Bind(R.id.cpu_temp) TextView mCpuTemp;
    @Bind(R.id.cpu_usage_header) TextView mCpuUsageHeader;
    @Bind(R.id.cpu_usage) TextView mCpuUsage;
    @Bind(R.id.cpu_features_header) TextView mCpuFeaturesHeader;
    @Bind(R.id.cpu_features) TextView mCpuFeatures;
    @Bind(R.id.device_header) TextView mDeviceHeader;
    @Bind(R.id.device_build_header) TextView mDeviceBuildHeader;
    @Bind(R.id.device_build) TextView mDeviceBuild;
    @Bind(R.id.device_api_header) TextView mDeviceApiHeader;
    @Bind(R.id.device_api) TextView mDeviceApi;
    @Bind(R.id.device_manuf_header) TextView mDeviceManufHeader;
    @Bind(R.id.device_manuf) TextView mDeviceManuf;
    @Bind(R.id.device_model_header) TextView mDeviceModelHeader;
    @Bind(R.id.device_model) TextView mDeviceModel;
    @Bind(R.id.device_board_header) TextView mDeviceBoardHeader;
    @Bind(R.id.device_board) TextView mDeviceBoard;
    @Bind(R.id.device_platform_header) TextView mDevicePlatformHeader;
    @Bind(R.id.device_platform) TextView mDevicePlatform;
    @Bind(R.id.device_runtime_header) TextView mDeviceRuntimeHeader;
    @Bind(R.id.device_runtime) TextView mDeviceRuntime;
    @Bind(R.id.device_bootloader_header) TextView mDeviceBootloaderHeader;
    @Bind(R.id.device_bootloader) TextView mDeviceBootloader;
    @Bind(R.id.scroll_container) ScrollView mScrollView;
    @Bind(R.id.content_overlay) View mContentOverlay;

    @Bind(R.id.cpu0_header) TextView mCore0Header;
    @Bind(R.id.cpu1_header) TextView mCore1Header;
    @Bind(R.id.cpu2_header) TextView mCore2Header;
    @Bind(R.id.cpu3_header) TextView mCore3Header;
    @Bind(R.id.cpu4_header) TextView mCore4Header;
    @Bind(R.id.cpu5_header) TextView mCore5Header;
    @Bind(R.id.cpu6_header) TextView mCore6Header;
    @Bind(R.id.cpu7_header) TextView mCore7Header;
    @Bind(R.id.cpu_freq0) TextView mCore0;
    @Bind(R.id.cpu_freq1) TextView mCore1;
    @Bind(R.id.cpu_freq2) TextView mCore2;
    @Bind(R.id.cpu_freq3) TextView mCore3;
    @Bind(R.id.cpu_freq4) TextView mCore4;
    @Bind(R.id.cpu_freq5) TextView mCore5;
    @Bind(R.id.cpu_freq6) TextView mCore6;
    @Bind(R.id.cpu_freq7) TextView mCore7;

    private boolean mDisableScrolling;
    private boolean mIsVisible;
    private boolean mIsMonitoringTemp;
    private boolean mIsMonitoringCpu;
    private boolean mIsMonitoringUsage;
    private boolean mHasCpu0;
    private boolean mHasCpu1;
    private boolean mHasCpu2;
    private boolean mHasCpu3;
    private boolean mHasCpu4;
    private boolean mHasCpu5;
    private boolean mHasCpu6;
    private boolean mHasCpu7;

    private Context mContext;
    private Handler mHandler;
    private Resources res;
    private Typeface robotoMedium;

    private int mNumCores;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.info_layout, container, false);
        setHasOptionsMenu(true);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /** Set text and typeface for TextViews */
        this.mContext = this.getActivity();
        mHandler = new Handler();
        res = getResources();
        robotoMedium = TypefaceHelper.mediumTypeface(mContext);
        final String api = CPUUtils.getSystemProperty("ro.build.version.sdk");
        final String platform = CPUUtils.getSystemProperty("ro.board.platform");
        final String kernelVersion = System.getProperty("os.version");

        /** @return the current number of CPU cores */
        mNumCores = CPUUtils.getCoreCount();
        if (mNumCores != 0) {
            mCpuCoreHeader.setTypeface(robotoMedium);
            mCpuCore.setText(String.format("%d", mNumCores));
        } else {
            mCpuCoreHeader.setVisibility(View.GONE);
            mCpuCore.setVisibility(View.GONE);
        }

        if (kernelVersion != null) mKernelVersion.setText(kernelVersion);
        if (CPUUtils.getGovernor() != null) mKernelGovernor.setText(CPUUtils.getGovernor());
        if (Build.CPU_ABI != null) mCpuAbi.setText(Build.CPU_ABI);
        if (CPUUtils.getArch() != null) mCpuArch.setText(CPUUtils.getArch());
        if (CPUUtils.getMinMax() != null) mCpuFreq.setText(CPUUtils.getMinMax());
        if (CPUUtils.getFeatures() != null) mCpuFeatures.setText(CPUUtils.getFeatures());
        if (Build.ID != null) mDeviceBuild.setText(Build.ID);
        if (api != null) mDeviceApi.setText(api);
        if (Build.MANUFACTURER != null) mDeviceManuf.setText(Build.MANUFACTURER);
        if (Build.MODEL != null) mDeviceModel.setText(Build.MODEL);
        if (Build.BOARD != null) mDeviceBoard.setText(Build.BOARD);
        if (Build.BOOTLOADER != null) mDeviceBootloader.setText(Build.BOOTLOADER);
        if (platform != null) mDevicePlatform.setText(platform);
        if (getRuntime() != null) mDeviceRuntime.setText(getRuntime());

        mKernelHeader.setTypeface(robotoMedium);
        mKernelGovernorHeader.setTypeface(robotoMedium);
        mKernelVersionHeader.setTypeface(robotoMedium);
        mCpuHeader.setTypeface(robotoMedium);
        mCpuAbiHeader.setTypeface(robotoMedium);
        mCpuArchHeader.setTypeface(robotoMedium);
        mCpuFreqHeader.setTypeface(robotoMedium);
        mCpuFeaturesHeader.setTypeface(robotoMedium);
        mCpuUsageHeader.setTypeface(robotoMedium);
        mDeviceHeader.setTypeface(robotoMedium);
        mDeviceBuildHeader.setTypeface(robotoMedium);
        mDeviceApiHeader.setTypeface(robotoMedium);
        mDeviceManufHeader.setTypeface(robotoMedium);
        mDeviceModelHeader.setTypeface(robotoMedium);
        mDeviceBoardHeader.setTypeface(robotoMedium);
        mDevicePlatformHeader.setTypeface(robotoMedium);
        mDeviceRuntimeHeader.setTypeface(robotoMedium);
        mDeviceBootloaderHeader.setTypeface(robotoMedium);
        mKernelVersionFullHeader.setTypeface(robotoMedium);

        final ThemedActivity act = (ThemedActivity) mContext;
        final int color = act.accentColor();
        final int accentColor = color == 0 ? ContextCompat.getColor(mContext, R.color.accent) : color;
        mKernelHeader.setTextColor(accentColor);
        mCpuHeader.setTextColor(accentColor);
        mDeviceHeader.setTextColor(accentColor);

        mScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mDisableScrolling;
            }
        });

        // Allow dismissing full kernel cardview with back button
        if (getView() != null) {
            getView().setFocusableInTouchMode(true);
            getView().requestFocus();

            getView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (mCardKernelFull.isShown()) {
                                showFullKernelVersion(false);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mIsVisible) setMonitoring(false);
        if (mCardKernelFull.isShown()) showFullKernelVersion(false);
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

            mIsMonitoringUsage = true;
            mHandler.post(monitorCpuUsage);
        } else {
            mIsMonitoringCpu = false;
            mIsMonitoringTemp = false;
            mIsMonitoringUsage = false;
        }
    }

    /** @return the current runtime: ART or Dalvik */
    private String getRuntime() {
        String runtime;
        final String vmVersion = System.getProperty("java.vm.version");

        if (vmVersion != null) {
            if (vmVersion.startsWith("2")) {
                runtime = res.getString(R.string.information_device_runtime_art)
                        + " v" + vmVersion.substring(0, 5);
            } else {
                runtime = res.getString(R.string.information_device_runtime_dalvik)
                        + " v" + vmVersion.substring(0, 5);
            }
        } else {
            runtime = null;
            mDeviceRuntimeHeader.setVisibility(View.GONE);
            mDeviceRuntime.setVisibility(View.GONE);
        }
        return runtime;
    }

    /** Check if we should monitor cpu temp */
    private void checkTempMonitor() {
        if (CPUUtils.hasTemp()) {
            mIsMonitoringTemp = true;
            mHandler.post(monitorTemp);
            mCpuTempHeader.setTypeface(robotoMedium);
            mCpuTemp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(mContext)
                            .content(CPUUtils.getTempFile())
                            .show();
                }
            });
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
                        mIsMonitoringTemp = false;
                        mCpuTemp.setText(res.getString(R.string.error));
                        mCpuTemp.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                        Log.e("CpuSpyInfo", "Error reading cpu temp: null");
                    }
                } catch (NumberFormatException e) {
                    mCpuTemp = null;
                }
                mHandler.postDelayed(this, 3000);
            }
        }
    };

    /** Monitor CPU usage */
    private final Runnable monitorCpuUsage = new Runnable() {
        @Override
        public void run() {
            if (mIsMonitoringUsage) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final float usage = CPUUtils.getCpuUsage();
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mCpuUsage.setText(String.format("%.01f%%", usage));
                                }
                            });
                        } catch (NumberFormatException e) {
                            mCpuUsage = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                            mIsMonitoringUsage = false;
                            mCpuUsageHeader.setVisibility(View.GONE);
                            mCpuUsage.setVisibility(View.GONE);
                        }
                    }
                }).start();
                mHandler.postDelayed(this, 3000);
            }
        }
    };

    private final Runnable monitorCpu = new Runnable() {
        public void run() {
            if (mIsMonitoringCpu) {
                /** Set the frequency for CPU0 */
                if (mHasCpu0) {
                    try {
                        // CPU0 should never be null
                        if (CPUUtils.getCpu0() != null) {
                            mCore0.setText(CPUUtils.getCpu0());
                        } else {
                            mIsMonitoringCpu = false;
                            mCore0.setText(res.getString(R.string.error));
                            mCore0.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                            Log.e("CpuSpyInfo", "Error reading cpu0: null");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        mCore0 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu0 = false;
                        mCore0.setText(res.getString(R.string.error));
                        mCore0.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU1 */
                if (mHasCpu1) {
                    try {
                        final File cpu1 = new File(CPUUtils.CPU1);
                        if (cpu1.length() == 0) {
                            mCore1.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore1.setText(CPUUtils.getCpu1());
                        }
                    } catch (NumberFormatException e) {
                        mCore1 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu1 = false;
                        mCore1.setText(res.getString(R.string.error));
                        mCore1.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU2 */
                if (mHasCpu2) {
                    try {
                        final File cpu2 = new File(CPUUtils.CPU2);
                        if (cpu2.length() == 0) {
                            mCore2.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore2.setText(CPUUtils.getCpu2());
                        }
                    } catch (NumberFormatException e) {
                        mCore2 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu2 = false;
                        mCore2.setText(res.getString(R.string.error));
                        mCore2.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU3 */
                if (mHasCpu3) {
                    try {
                        final File cpu3 = new File(CPUUtils.CPU3);
                        if (cpu3.length() == 0) {
                            mCore3.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore3.setText(CPUUtils.getCpu3());
                        }
                    } catch (NumberFormatException e) {
                        mCore3 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu3 = false;
                        mCore3.setText(res.getString(R.string.error));
                        mCore3.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU4 */
                if (mHasCpu4) {
                    try {
                        final File cpu4 = new File(CPUUtils.CPU4);
                        if (cpu4.length() == 0) {
                            mCore4.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore4.setText(CPUUtils.getCpu4());
                        }
                    } catch (NumberFormatException e) {
                        mCore4 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu4 = false;
                        mCore4.setText(res.getString(R.string.error));
                        mCore4.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU5 */
                if (mHasCpu5) {
                    try {
                        final File cpu5 = new File(CPUUtils.CPU5);
                        if (cpu5.length() == 0) {
                            mCore5.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore5.setText(CPUUtils.getCpu5());
                        }
                    } catch (NumberFormatException e) {
                        mCore5 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu5 = false;
                        mCore5.setText(res.getString(R.string.error));
                        mCore5.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU6 */
                if (mHasCpu6) {
                    try {
                        final File cpu6 = new File(CPUUtils.CPU6);
                        if (cpu6.length() == 0) {
                            mCore6.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore6.setText(CPUUtils.getCpu6());
                        }
                    } catch (NumberFormatException e) {
                        mCore6 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu6 = false;
                        mCore6.setText(res.getString(R.string.error));
                        mCore6.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }
                /** Set the frequency for CPU7 */
                if (mHasCpu7) {
                    try {
                        final File cpu7 = new File(CPUUtils.CPU7);
                        if (cpu7.length() == 0) {
                            mCore7.setText(res.getString(R.string.core_offline));
                        } else {
                            mCore7.setText(CPUUtils.getCpu7());
                        }
                    } catch (NumberFormatException e) {
                        mCore7 = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHasCpu7 = false;
                        mCore7.setText(res.getString(R.string.error));
                        mCore7.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text_color_error));
                    }
                }

                mHandler.postDelayed(this, 1000); // 1 second
            }
        }
    };

    /** Check which CPU cores to start monitoring */
    private void checkCoreMonitor() {
        switch (mNumCores) {
            default:
                return;
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

    /** Bind button listeners */
    @OnClick({R.id.btn_kernel_more, R.id.btn_kernel_close})
    void kernelMoreButton() {
        if (!mCardKernelFull.isShown()) {
            showFullKernelVersion(true);
            mContentOverlay.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent ev) {
                    if (mCardKernelFull.isShown()
                            && Utils.isOutOfBounds(mCardKernelFull, ev)) {
                        showFullKernelVersion(false);
                        return true;
                    }
                    return false;
                }
            });
        } else {
            showFullKernelVersion(false);
        }
    }

    private boolean showFullKernelVersion(boolean enabled) {
        if (enabled) {
            if (!mCardKernelFull.isShown()) {
                final Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
                mContentOverlay.startAnimation(fadeIn);
                mContentOverlay.setVisibility(View.VISIBLE);

                final Animation slideUp = AnimationUtils.loadAnimation(mContext, R.anim.slide_up);
                mCardKernelFull.startAnimation(slideUp);
                mCardKernelFull.setVisibility(View.VISIBLE);

                mDisableScrolling = true;
                if (CPUUtils.getKernelVersion() != null) {
                    mKernelVersionFull.setText(CPUUtils.getKernelVersion());
                } else {
                    mKernelVersionFull.setText(res.getString(R.string.information_kernel_version_unavailable));
                }
            }
            return true;
        } else {
            final Animation fadeOut = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
            mContentOverlay.startAnimation(fadeOut);
            mContentOverlay.setVisibility(View.GONE);

            final Animation slideDown = AnimationUtils.loadAnimation(mContext, R.anim.slide_down);
            mCardKernelFull.startAnimation(slideDown);
            mCardKernelFull.setVisibility(View.GONE);

            mDisableScrolling = false;

            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setMonitoring(false);
        ButterKnife.unbind(this);
    }
}
