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
import androidx.appcompat.app.AlertDialog;
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
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.speech.tts.TextToSpeech;

public class SecondFragment extends Fragment implements TutorialHandler {
    private TextToSpeech tts;

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
            tts = new TextToSpeech(requireContext(), status -> {
                if (status != TextToSpeech.ERROR) {
                    Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
                    
                    // Try to set current locale
                    int result = tts.setLanguage(currentLocale);
                    
                    // If Bisaya (ceb) is not supported, use Filipino (fil) which has similar phonetics
                    if (currentLocale.getLanguage().equals("ceb") && 
                        (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                        tts.setLanguage(new Locale("fil", "PH"));
                    } else if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // General fallback to English
                        tts.setLanguage(Locale.US);
                    }
                }
            });
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error initializing classifiers", Toast.LENGTH_SHORT).show();
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        
        binding.btnCapture.setOnClickListener(v -> showScanTips());

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

    @Override
    public void showTutorial() {
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.btnCapture, getString(R.string.tut_camera_title), getString(R.string.tut_camera_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(binding.btnGallery, getString(R.string.tut_gallery_title), getString(R.string.tut_gallery_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(binding.toggleModel, getString(R.string.tut_model_toggle_title), getString(R.string.tut_model_toggle_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(50)
                )
                .start();
    }

    private void showScanTips() {
        String tips = getString(R.string.scan_tip_1) + "<br>" +
                getString(R.string.scan_tip_2) + "<br>" +
                getString(R.string.scan_tip_3);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.scan_tips_title)
                .setMessage(android.text.Html.fromHtml(tips, android.text.Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(R.string.btn_got_it, (dialog, which) -> {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        openCamera();
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
            Bitmap rawBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
                rawBitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setMutableRequired(true));
            } else {
                rawBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }
            
            // Fix orientation
            Bitmap rotatedBitmap = rotateImageIfRequired(rawBitmap, uri);
            Bitmap bitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true);

            if (validateImageQuality(bitmap, uri)) {
                performClassification(uri, bitmap);
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void performClassification(Uri uri, Bitmap bitmap) {
        if (classifierAlpha != null && classifierBeta != null) {
            if (bitmap == null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
                        bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setMutableRequired(true));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                    }
                    bitmap = rotateImageIfRequired(bitmap, uri);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error reloading image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            resultAlpha = classifierAlpha.classify(bitmap);
            resultBeta = classifierBeta.classify(bitmap);

            currentImagePath = saveImageToInternalStorage(uri);
            saveToHistory();
            updateUI();
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

    private boolean validateImageQuality(Bitmap bitmap, Uri uri) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Sampling for performance
        int step = 10; 
        long totalBrightness = 0;
        int count = 0;
        
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                
                // Using relative luminance formula
                totalBrightness += (long) (0.299 * r + 0.587 * g + 0.114 * b);
                count++;
            }
        }
        
        double avgBrightness = (double) totalBrightness / count;
        
        if (avgBrightness < 40) {
            showQualityWarning(getString(R.string.warning_too_dark), uri, bitmap);
            return false;
        } else if (avgBrightness > 230) {
            showQualityWarning(getString(R.string.warning_too_bright), uri, bitmap);
            return false;
        }
        
        return true;
    }

    private void showQualityWarning(String message, Uri uri, Bitmap bitmap) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.quality_warning_title)
                .setMessage(message + "\n\n" + getString(R.string.quality_warning_suggestion))
                .setPositiveButton(R.string.retry, (dialog, which) -> showScanTips())
                .setNeutralButton(R.string.btn_continue_anyway, (dialog, which) -> performClassification(uri, bitmap))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
        
        String reliability;
        int reliabilityColor;
        if (currentResult.confidence > 85) {
            reliability = getString(R.string.confidence_high);
            reliabilityColor = ContextCompat.getColor(requireContext(), R.color.pod_healthy);
        } else if (currentResult.confidence > 60) {
            reliability = getString(R.string.confidence_medium);
            reliabilityColor = ContextCompat.getColor(requireContext(), R.color.pod_warning);
        } else {
            reliability = getString(R.string.confidence_low);
            reliabilityColor = ContextCompat.getColor(requireContext(), R.color.pod_danger);
        }

        String confidenceText = String.format(Locale.getDefault(), "%s: %.2f%% (%s)", modelName, currentResult.confidence, reliability);
        binding.tvConfidence.setText(confidenceText);
        binding.tvConfidence.setTextColor(reliabilityColor);
        
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
            binding.btnReadAloud.setVisibility(View.VISIBLE);
            
            final String finalGoal = goal;
            final String[] finalItems = items;
            final String finalFooter = footer;
            
            binding.btnReadAloud.setOnClickListener(v -> tts.speak(finalGoal + ". " + String.join(". ", finalItems) + ". " + finalFooter, TextToSpeech.QUEUE_FLUSH, null, null));
        } else if (lowerResult.contains("healthy")) {
            String healthyMsg = getString(R.string.rec_healthy_msg);
            binding.tvRecommendation.setText(android.text.Html.fromHtml(healthyMsg, android.text.Html.FROM_HTML_MODE_LEGACY));
            binding.layoutRecImages.setVisibility(View.GONE);
            binding.btnReadAloud.setVisibility(View.VISIBLE);
            binding.btnReadAloud.setOnClickListener(v -> {
                tts.speak(android.text.Html.fromHtml(healthyMsg, android.text.Html.FROM_HTML_MODE_LEGACY).toString(), 
                        TextToSpeech.QUEUE_FLUSH, null, null);
            });
            iconRes = R.drawable.ic_healthy;
        } else {
            binding.tvRecommendation.setText(android.text.Html.fromHtml(getString(R.string.rec_unknown_msg), android.text.Html.FROM_HTML_MODE_LEGACY));
            binding.layoutRecImages.setVisibility(View.GONE);
            binding.btnReadAloud.setVisibility(View.GONE);
        }

        // Add disclaimer at the end
        android.widget.TextView disclaimerTv = new android.widget.TextView(requireContext());
        disclaimerTv.setText(R.string.disclaimer_expert);
        disclaimerTv.setTextSize(12);
        disclaimerTv.setPadding(0, 32, 0, 16);
        disclaimerTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.layoutRecImages.addView(disclaimerTv);

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