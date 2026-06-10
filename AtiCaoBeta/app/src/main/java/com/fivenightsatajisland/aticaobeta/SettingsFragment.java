package com.fivenightsatajisland.aticaobeta;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment implements TutorialHandler {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        view.findViewById(R.id.btn_theme_light).setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_NO).apply();
        });
            
        view.findViewById(R.id.btn_theme_dark).setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            prefs.edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_YES).apply();
        });

        SwitchMaterial switchRecImages = view.findViewById(R.id.switch_rec_images);
        switchRecImages.setChecked(prefs.getBoolean("show_rec_images", true));
        switchRecImages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("show_rec_images", isChecked).apply();
        });

        view.findViewById(R.id.btn_about).setOnClickListener(v -> showAboutDialog());
        view.findViewById(R.id.btn_contact).setOnClickListener(v -> showContactDialog());
        
        view.findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    AppDatabase.getDatabase(getContext()).scanHistoryDao().deleteAll();
                    Toast.makeText(getContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
        });

        view.findViewById(R.id.btn_clear_sensor_history).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.clear_trends_title)
                    .setMessage(R.string.clear_trends_message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        AppDatabase.getDatabase(getContext()).sensorHistoryDao().deleteAll();
                        Toast.makeText(getContext(), R.string.trends_cleared, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return view;
    }

    @Override
    public void showTutorial() {
        if (getView() == null) return;
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(getView().findViewById(R.id.btn_theme_light), getString(R.string.tut_appearance_title), getString(R.string.tut_appearance_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(getView().findViewById(R.id.switch_rec_images), getString(R.string.tut_rec_images_title), getString(R.string.tut_rec_images_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(getView().findViewById(R.id.btn_clear_history), getString(R.string.tut_data_mgmt_title), getString(R.string.tut_data_mgmt_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40)
                )
                .start();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showContactDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.contact_title)
            .setMessage(R.string.contact_message)
            .setPositiveButton("OK", null)
            .show();
    }
}