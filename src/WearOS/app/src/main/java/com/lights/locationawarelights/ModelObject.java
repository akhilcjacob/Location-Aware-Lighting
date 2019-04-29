package com.lights.locationawarelights;

public enum ModelObject {

    FIRST(1,R.layout.home_page),
    SECOND(2, R.layout.second_page),
    THIRD(3, R.layout.third_page);

    private int mTitleResId;
    private int mLayoutResId;

    ModelObject(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }

}