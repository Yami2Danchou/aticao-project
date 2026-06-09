package com.fivenightsatajisland.aticaobeta;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.exifinterface.media.ExifInterface;

import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.fivenightsatajisland.aticaobeta.database.ScanHistory;
import com.fivenightsatajisland.aticaobeta.databinding.FragmentSecondBinding;
import com.fivenightsatajisland.aticaobeta.tflite.CacaoClassifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private Uri photoUri;
    private CacaoClassifier classifierAlpha;
    private CacaoClassifier classifierBeta;
    private CacaoClassifier.Recognition resultAlpha;
    private CacaoClassifier.Recognition resultBeta;
    private String currentImagePath;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    binding.ivPod.setImageURI(uri);
                    processImage(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    binding.ivPod.setImageURI(photoUri);
                    processImage(photoUri);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to scan pods", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        try {
            classifierAlpha = new CacaoClassifier(requireContext(), "aticao_severity_alpha.tflite", "labels_severity_alpha.txt");
            classifierBeta = new CacaoClassifier(requireContext(), "aticao_severity_beta.tflite", "labels_severity_beta.txt");
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error initializing classifiers", Toast.LENGTH_SHORT).show();
        }
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        
        binding.btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.toggleModel.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateUI();
            }
        });

        binding.buttonSecond.setOnClickListener(v ->
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment)
        );
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void processImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setMutableRequired(true));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }
            
            // Fix orientation
            bitmap = rotateImageIfRequired(bitmap, uri);
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            if (classifierAlpha != null && classifierBeta != null) {
                resultAlpha = classifierAlpha.classify(bitmap);
                resultBeta = classifierBeta.classify(bitmap);
                
                currentImagePath = saveImageToInternalStorage(uri);
                saveToHistory();
                updateUI();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        try (InputStream input = requireContext().getContentResolver().openInputStream(selectedImage)) {
            if (input == null) return img;
            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return uri.toString();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
            String fileName = "SCAN_" + timeStamp + ".jpg";
            File file = new File(requireContext().getFilesDir(), fileName);
            
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            inputStream.close();
            
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            return uri.toString();
        }
    }

    private void updateUI() {
        if (resultAlpha == null || resultBeta == null) return;

        boolean isAlpha = binding.toggleModel.getCheckedButtonId() == R.id.btn_alpha;
        CacaoClassifier.Recognition currentResult = isAlpha ? resultAlpha : resultBeta;

        String formattedResult = formatResult(currentResult.title);
        String modelName = isAlpha ? getString(R.string.alpha_model_label) : getString(R.string.beta_model_label);
        
        binding.tvResult.setText(formattedResult);
        binding.tvConfidence.setText(String.format(Locale.getDefault(), "%s Confidence: %.2f%%", modelName, currentResult.confidence));
        
        binding.tvRecommendationLabel.setVisibility(View.VISIBLE);
        binding.tvRecommendation.setVisibility(View.VISIBLE);
        
        showRecommendation(currentResult.title);
    }

    private void showRecommendation(String rawResult) {
        String lowerResult = rawResult.toLowerCase();
        int colorRes = R.color.text_secondary;
        int iconRes = 0;
        
        String goal = null;
        String[] items = null;
        String footer = null;
        String folderPath = null;

        if (lowerResult.contains("black pod rot")) {
            colorRes = R.color.pod_danger;
            if (lowerResult.contains("mild")) {
                iconRes = R.drawable.ic_mild;
                goal = getString(R.string.goal_bpr_mild);
                items = getResources().getStringArray(R.array.items_bpr_mild);
                footer = getString(R.string.rec_bpr_mild_footer);
                folderPath = "images/Black Pod Rot/Mild";
            } else if (lowerResult.contains("moderate")) {
                iconRes = R.drawable.ic_moderate;
                goal = getString(R.string.goal_bpr_mod);
                items = getResources().getStringArray(R.array.items_bpr_mod);
                footer = getString(R.string.rec_bpr_mod_footer);
                folderPath = "images/Black Pod Rot/Moderate";
            } else if (lowerResult.contains("severe")) {
                iconRes = R.drawable.ic_severe;
                goal = getString(R.string.goal_bpr_sev);
                items = getResources().getStringArray(R.array.items_bpr_sev);
                footer = getString(R.string.rec_bpr_sev_footer);
                folderPath = "images/Black Pod Rot/Severe";
            }
        } else if (lowerResult.contains("pod borer")) {
            colorRes = R.color.pod_warning;
            if (lowerResult.contains("mild")) {
                iconRes = R.drawable.ic_mild;
                goal = getString(R.string.goal_cpb_mild);
                items = getResources().getStringArray(R.array.items_cpb_mild);
                footer = getString(R.string.rec_cpb_mild_footer);
                folderPath = "images/Pod Boarer/Mild";
            } else if (lowerResult.contains("moderate")) {
                iconRes = R.drawable.ic_moderate;
                goal = getString(R.string.goal_cpb_mod);
                items = getResources().getStringArray(R.array.items_cpb_mod);
                footer = getString(R.string.rec_cpb_mod_footer);
                folderPath = "images/Pod Boarer/Moderate";
            } else if (lowerResult.contains("severe")) {
                iconRes = R.drawable.ic_severe;
                goal = getString(R.string.goal_cpb_sev);
                items = getResources().getStringArray(R.array.items_cpb_sev);
                footer = getString(R.string.rec_cpb_sev_footer);
                folderPath = "images/Pod Boarer/Severe";
            }
        } else if (lowerResult.contains("healthy")) {
            colorRes = R.color.pod_healthy;
        }

        binding.layoutRecImages.removeAllViews();
        binding.tvResult.setTextColor(ContextCompat.getColor(requireContext(), colorRes));

        if (goal != null) {
            String severity = "";
            if (lowerResult.contains("mild")) severity = getString(R.string.severity_mild);
            else if (lowerResult.contains("moderate")) severity = getString(R.string.severity_moderate);
            else if (lowerResult.contains("severe")) severity = getString(R.string.severity_severe);

            String header = severity + "<br><br><b>Goal:</b> " + goal;
            binding.tvRecommendation.setText(android.text.Html.fromHtml(header, android.text.Html.FROM_HTML_MODE_LEGACY));

            SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
            boolean showImages = prefs.getBoolean("show_rec_images", true);

            try {
                String[] assetFiles = showImages ? requireContext().getAssets().list(folderPath) : null;
                for (int i = 0; i < items.length; i++) {
                    // Add Text for the line
                    android.widget.TextView itemTv = new android.widget.TextView(requireContext());
                    String itemText = "• " + items[i];
                    itemTv.setText(android.text.Html.fromHtml(itemText, android.text.Html.FROM_HTML_MODE_LEGACY));
                    itemTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                    itemTv.setPadding(0, 8, 0, 8);
                    binding.layoutRecImages.addView(itemTv);

                    // Add corresponding Image if available and enabled
                    if (showImages && assetFiles != null && i < assetFiles.length) {
                        ImageView iv = new ImageView(requireContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 600);
                        params.setMargins(0, 8, 0, 16);
                        iv.setLayoutParams(params);
                        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        try (InputStream is = requireContext().getAssets().open(folderPath + "/" + assetFiles[i])) {
                            Drawable d = Drawable.createFromStream(is, null);
                            iv.setImageDrawable(d);
                            binding.layoutRecImages.addView(iv);
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException e) {
                // Fallback if assets fail
                for (String item : items) {
                    android.widget.TextView itemTv = new android.widget.TextView(requireContext());
                    String itemText = "• " + item;
                    itemTv.setText(itemText);
                    itemTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                    binding.layoutRecImages.addView(itemTv);
                }
            }

            if (footer != null) {
                android.widget.TextView footerTv = new android.widget.TextView(requireContext());
                String footerText = "<br><b>Recommendation:</b> " + footer;
                footerTv.setText(android.text.Html.fromHtml(footerText, android.text.Html.FROM_HTML_MODE_LEGACY));
                footerTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                binding.layoutRecImages.addView(footerTv);
            }
            binding.layoutRecImages.setVisibility(View.VISIBLE);
        } else if (lowerResult.contains("healthy")) {
            binding.tvRecommendation.setText(android.text.Html.fromHtml(getString(R.string.rec_healthy_msg), android.text.Html.FROM_HTML_MODE_LEGACY));
            binding.layoutRecImages.setVisibility(View.GONE);
            iconRes = R.drawable.ic_healthy;
        } else {
            binding.tvRecommendation.setText(android.text.Html.fromHtml(getString(R.string.rec_unknown_msg), android.text.Html.FROM_HTML_MODE_LEGACY));
            binding.layoutRecImages.setVisibility(View.GONE);
        }

        if (iconRes != 0) {
            binding.tvResult.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(binding.tvResult, 
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes)));
        } else {
            binding.tvResult.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void saveToHistory() {
        String primaryResult = formatResult(resultAlpha.title);
        String severity = "N/A";
        String lowerAlpha = resultAlpha.title.toLowerCase();
        
        if (lowerAlpha.contains("mild")) severity = "Mild";
        else if (lowerAlpha.contains("moderate")) severity = "Moderate";
        else if (lowerAlpha.contains("severe")) severity = "Severe";
        else if (lowerAlpha.contains("healthy")) severity = "Healthy";

        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        
        AppDatabase.getDatabase(getContext()).scanHistoryDao().insert(
                new ScanHistory(primaryResult, resultAlpha.confidence, date, currentImagePath, severity,
                        resultAlpha.confidence, resultBeta.confidence, resultAlpha.title, resultBeta.title)
        );
    }

    private String formatResult(String raw) {
        String clean = raw.replaceAll("^\\d+:", "").replace("_", " ");
        StringBuilder result = new StringBuilder();
        String[] words = clean.split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (classifierAlpha != null) classifierAlpha.close();
        if (classifierBeta != null) classifierBeta.close();
        binding = null;
    }
}