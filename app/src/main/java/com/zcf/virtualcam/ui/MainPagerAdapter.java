package com.zcf.virtualcam.ui;

// Usage: supplies fragments for common/camera/location pages.

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.zcf.virtualcam.R;

public class MainPagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 3;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CommonFragment();
            case 1:
                return new CameraFragment();
            default:
                return new LocationFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    public int getTitleResId(int position) {
        switch (position) {
            case 0:
                return R.string.label_tab_common;
            case 1:
                return R.string.label_tab_camera;
            default:
                return R.string.label_tab_location;
        }
    }
}
