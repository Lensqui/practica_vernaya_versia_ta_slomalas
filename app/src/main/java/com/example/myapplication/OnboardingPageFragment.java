package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class OnboardingPageFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private LinearLayout indicatorLayout;

    public static OnboardingPageFragment newInstance(int position) {
        OnboardingPageFragment fragment = new OnboardingPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_page, container, false);
        ImageView imageView = view.findViewById(R.id.imageView);
        TextView textView = view.findViewById(R.id.textView);
        TextView subtitleText = view.findViewById(R.id.subtitleText);
        indicatorLayout = view.findViewById(R.id.indicatorLayout);

        int position = getArguments().getInt(ARG_POSITION);
        switch (position) {
            case 0:
                imageView.setImageResource(R.drawable.botinok1);
                textView.setText(getResources().getString(R.string.welcome_title));
                subtitleText.setText(getResources().getString(R.string.welcome_subtitle));
                break;
            case 1:
                imageView.setImageResource(R.drawable.botinok2);
                textView.setText(getResources().getString(R.string.start_journey_title));
                subtitleText.setText(getResources().getString(R.string.start_journey_subtitle));
                break;
            case 2:
                imageView.setImageResource(R.drawable.botinok3);
                textView.setText(getResources().getString(R.string.you_have_power_title));
                subtitleText.setText(getResources().getString(R.string.you_have_power_subtitle));
                break;
        }


        setupIndicators(3, position);

        return view;
    }

    private void setupIndicators(int count, int selectedPosition) {
        indicatorLayout.removeAllViews();
        for (int i = 0; i < count; i++) {
            View indicator = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(20), dpToPx(2));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            indicator.setLayoutParams(params);
            if (i == selectedPosition) {
                indicator.setBackgroundResource(R.drawable.indicator_selected);
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_unselected);
            }
            indicatorLayout.addView(indicator);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
