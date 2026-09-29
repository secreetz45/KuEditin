package com.example.mediamatch;

import android.app.Activity;

public class TransitionUtil {
    public static void slideRight(Activity activity) {
        if (activity != null) activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void slideLeft(Activity activity) {
        if (activity != null) activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    public static void slideUp(Activity activity) {
        if (activity != null) activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void slideDown(Activity activity) {
        if (activity != null) activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    public static void fade(Activity activity) {
        if (activity != null) activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
