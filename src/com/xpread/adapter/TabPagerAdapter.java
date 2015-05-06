
package com.xpread.adapter;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TabPagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> mList;

    public TabPagerAdapter(FragmentManager fm, List<Fragment> list) {
        super(fm);
        mList = list;
    }

    @Override
    public Fragment getItem(int postion) {
        return mList.get(postion);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

}
