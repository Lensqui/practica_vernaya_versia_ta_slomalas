package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private AppCompatButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        button = findViewById(R.id.button);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateButtonText(position);
            }
        });

        button.setOnClickListener(v -> onButtonClick(viewPager.getCurrentItem()));
    }

    private void updateButtonText(int position) {
        if (position == 0) {
            button.setText(getResources().getString(R.string.start_button));
        } else {
            button.setText(getResources().getString(R.string.next_button));
        }
    }

    public void onButtonClick(int position) {
        if (position < 2) {
            viewPager.setCurrentItem(position + 1, true);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public Fragment createFragment(int position) {
            return OnboardingPageFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}