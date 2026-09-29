package com.example.mediamatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.widget.TextView;

public class NavUtil {

    public static void saveRole(Context context, String role) {
        if (context == null || role == null) return;
        context.getSharedPreferences("user_role_pref", Context.MODE_PRIVATE)
                .edit()
                .putString("user_role", role.toLowerCase().trim())
                .apply();
    }

    public static boolean isEditor(Context context) {
        if (context == null) return false;
        String role = context.getSharedPreferences("user_role_pref", Context.MODE_PRIVATE)
                .getString("user_role", "");
        if ("editor".equalsIgnoreCase(role)) return true;
        if ("client".equalsIgnoreCase(role)) return false;

        if (context instanceof Activity) {
            String name = ((Activity) context).getClass().getSimpleName();
            return name.contains("Editor");
        }
        return false;
    }

    public static void setupClientNav(Activity activity, int activeIndex) {
        View indicator = activity.findViewById(R.id.nav_indicator);
        View navHome = activity.findViewById(R.id.nav_home);
        View navOrder = activity.findViewById(R.id.nav_order);
        View navChat = activity.findViewById(R.id.nav_chat);
        View navProfile = activity.findViewById(R.id.nav_profile);
        View pillContainer = activity.findViewById(R.id.nav_pill_container);

        if (indicator == null || navHome == null || navOrder == null || navChat == null
                || navProfile == null || pillContainer == null || activeIndex < 0 || activeIndex > 3) return;

        View[] navs = {navHome, navOrder, navChat, navProfile};
        int sourceIndex = activity.getIntent().getIntExtra("nav_source_index", -1);

        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing()) return;

            float density = activity.getResources().getDisplayMetrics().density;
            View targetView = navs[activeIndex];
            if (targetView.getWidth() <= 0) return;
            int targetWidth = (int) (targetView.getWidth() * 0.85f);
            float targetX = targetView.getLeft() + (targetView.getWidth() / 2f) - (targetWidth / 2f);

            // Ensure navigation container is brought to top layer
            if (pillContainer.getParent() instanceof View) {
                ((View) pillContainer.getParent()).bringToFront();
            }

            // Animate indicator pill smoothly on stationary bottom navigation bar with GPU layer
            indicator.animate().cancel();
            indicator.setScaleX(1.0f);
            indicator.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            ViewGroup.LayoutParams lp = indicator.getLayoutParams();
            lp.width = targetWidth;
            indicator.setLayoutParams(lp);

            if (sourceIndex != -1 && sourceIndex != activeIndex && sourceIndex < navs.length) {
                View sourceView = navs[sourceIndex];
                int startWidth = (int) (sourceView.getWidth() * 0.85f);
                float startX = sourceView.getLeft() + (sourceView.getWidth() / 2f) - (startWidth / 2f);

                indicator.setTranslationX(startX);
                indicator.setVisibility(View.VISIBLE);
                indicator.setAlpha(1.0f);

                float scaleFactor = (float) targetWidth / startWidth;
                indicator.animate()
                        .translationX(targetX)
                        .scaleX(scaleFactor)
                        .setDuration(240)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .withEndAction(() -> {
                            indicator.setScaleX(1.0f);
                            indicator.setLayerType(View.LAYER_TYPE_NONE, null);
                            ViewGroup.LayoutParams finalLp = indicator.getLayoutParams();
                            finalLp.width = targetWidth;
                            indicator.setLayoutParams(finalLp);
                            setActiveStyle((TextView) navs[activeIndex], density);
                        })
                        .start();
            } else {
                indicator.setTranslationX(targetX);
                indicator.setVisibility(View.VISIBLE);
                indicator.setAlpha(1.0f);
                indicator.setLayerType(View.LAYER_TYPE_NONE, null);
                setActiveStyle((TextView) navs[activeIndex], density);
            }

            for (int i = 0; i < navs.length; i++) {
                if (i != activeIndex) setInactiveStyle((TextView) navs[i]);
            }
        });

        navHome.setOnClickListener(v -> handleNavigation(activity, activeIndex, 0));
        navOrder.setOnClickListener(v -> handleNavigation(activity, activeIndex, 1));
        navChat.setOnClickListener(v -> handleNavigation(activity, activeIndex, 2));
        navProfile.setOnClickListener(v -> handleNavigation(activity, activeIndex, 3));
    }

    private static void setActiveStyle(TextView tv, float density) {
        tv.setTextColor(Color.WHITE);
        tv.setAlpha(1.0f);
        tv.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
        tv.animate().scaleX(1.08f).scaleY(1.08f).translationY(-density * 2).setDuration(180).start();
    }

    private static void setInactiveStyle(TextView tv) {
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setAlpha(0.7f);
        tv.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
        tv.setScaleX(1.0f); tv.setScaleY(1.0f); tv.setTranslationY(0);
    }

    private static void handleNavigation(Activity activity, int current, int target) {
        if (current == target) return;
        boolean isEditorMode = isEditor(activity);

        Class<?> targetClass;
        if (target == 0) targetClass = isEditorMode ? EditorHomeActivity.class : HomeClientActivity.class;
        else if (target == 1) targetClass = isEditorMode ? EditorOrdersActivity.class : OrderClientActivity.class;
        else if (target == 2) targetClass = ChatListActivity.class;
        else targetClass = ProfileActivity.class;

        Intent intent = new Intent(activity, targetClass);
        intent.putExtra("nav_source_index", current);
        activity.startActivity(intent);
        activity.finish();
        activity.overridePendingTransition(R.anim.no_anim, R.anim.no_anim);
    }
}
