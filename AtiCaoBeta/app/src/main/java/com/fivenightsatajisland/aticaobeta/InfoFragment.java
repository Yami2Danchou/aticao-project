package com.fivenightsatajisland.aticaobeta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

public class InfoFragment extends Fragment implements TutorialHandler {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_info, container, false);
    }

    @Override
    public void showTutorial() {
        if (getView() == null) return;
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(getView().findViewById(R.id.black_pod_rot_title), getString(R.string.tut_disease_lib_title), getString(R.string.tut_disease_lib_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(60)
                )
                .start();
    }
}