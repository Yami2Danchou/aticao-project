package com.fivenightsatajisland.aticaobeta;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.fivenightsatajisland.aticaobeta.databinding.FragmentFirstBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;

public class FirstFragment extends Fragment implements TutorialHandler {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }
//Improved offline inference processing
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkOnboarding();

        binding.buttonFirst.setOnClickListener(v -> navigateToSecond());
        binding.cardScan.setOnClickListener(v -> navigateToSecond());
        
        binding.cardHistory.setOnClickListener(v -> 
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_HistoryFragment));

        binding.cardInfo.setOnClickListener(v -> 
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_InfoFragment));

        binding.cardMonitoring.setOnClickListener(v ->
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_MonitoringFragment));

        binding.cardSensorHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SensorHistoryFragment));
    }

    @Override
    public void showTutorial() {
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.tvScanTitle, getString(R.string.tut_disease_det_title), getString(R.string.tut_disease_det_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .titleTextColor(R.color.white)
                                .descriptionTextColor(R.color.white)
                                .drawShadow(true)
                                .cancelable(true)
                                .tintTarget(false)
                                .transparentTarget(true)
                                .targetRadius(45),
                        TapTarget.forView(binding.tvMonitoringTitle, getString(R.string.tut_monitoring_title), getString(R.string.tut_monitoring_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .titleTextColor(R.color.white)
                                .descriptionTextColor(R.color.white)
                                .drawShadow(true)
                                .cancelable(true)
                                .tintTarget(false)
                                .transparentTarget(true)
                                .targetRadius(45),
                        TapTarget.forView(binding.tvHistoryTitle, getString(R.string.tut_history_title), getString(R.string.tut_history_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .titleTextColor(R.color.white)
                                .descriptionTextColor(R.color.white)
                                .drawShadow(true)
                                .cancelable(true)
                                .tintTarget(false)
                                .transparentTarget(true)
                                .targetRadius(45)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Tutorial finished
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                    }
                })
                .start();
    }

    private void navigateToSecond() {
        NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment);
    }

    private void checkOnboarding() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        boolean firstRun = prefs.getBoolean("first_run_v2", true);
        if (firstRun) {
            showOnboardingStep1();
            prefs.edit().putBoolean("first_run_v2", false).apply();
        }
    }

    private void showOnboardingStep1() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_welcome_title)
                .setMessage(R.string.onboarding_welcome_msg)
                .setPositiveButton(R.string.next, (dialog, which) -> showOnboardingStep2())
                .setCancelable(false)
                .show();
    }

    private void showOnboardingStep2() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_scan_title)
                .setMessage(R.string.onboarding_scan_msg)
                .setPositiveButton(R.string.next, (dialog, which) -> showOnboardingStep3())
                .setNegativeButton(R.string.previous, (dialog, which) -> showOnboardingStep1())
                .setCancelable(false)
                .show();
    }

    private void showOnboardingStep3() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_monitoring_title)
                .setMessage(R.string.onboarding_monitoring_msg)
                .setPositiveButton(R.string.next, (dialog, which) -> showOnboardingStep4())
                .setNegativeButton(R.string.previous, (dialog, which) -> showOnboardingStep2())
                .setCancelable(false)
                .show();
    }

    private void showOnboardingStep4() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_history_title)
                .setMessage(R.string.onboarding_history_msg)
                .setPositiveButton(R.string.onboarding_finish, null)
                .setNegativeButton(R.string.previous, (dialog, which) -> showOnboardingStep3())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}