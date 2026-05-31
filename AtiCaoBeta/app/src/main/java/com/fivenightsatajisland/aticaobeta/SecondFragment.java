package com.fivenightsatajisland.aticaobeta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

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
import java.util.Objects;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private Uri photoUri;
    private CacaoClassifier classifier;

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
            classifier = new CacaoClassifier(requireContext());
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error initializing classifier", Toast.LENGTH_SHORT).show();
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
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }
            // Need to make sure bitmap is ARGB_8888 for TFLite
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            if (classifier != null) {
                CacaoClassifier.Recognition result = classifier.classify(bitmap);
                
                // Save a permanent copy of the image for history
                String permanentPath = saveImageToInternalStorage(uri);
                
                updateUIWithResult(result.title, result.confidence, permanentPath);
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
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
            e.printStackTrace();
            return uri.toString();
        }
    }

    private void updateUIWithResult(String rawResult, float confidence, String imagePath) {
        String result = formatResult(rawResult);
        binding.tvResult.setText(result);
        binding.tvConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.2f%%", confidence));
        
        binding.tvRecommendationLabel.setVisibility(View.VISIBLE);
        binding.tvRecommendation.setVisibility(View.VISIBLE);
        
        StringBuilder recommendation = new StringBuilder();
        int colorRes = R.color.text_secondary;

        if (rawResult.contains("Black Pod Rot")) {
            colorRes = R.color.pod_danger;
            if (rawResult.contains("Mild")) {
                recommendation.append("<b>Severity: Mild</b><br><br>")
                        .append("1. <b>Identify & Remove:</b> Immediately pick infected pods from the tree.<br>")
                        .append("2. <b>Disposal:</b> Bury infected pods at least 50cm deep or burn them. Do not leave them on the ground.<br>")
                        .append("3. <b>Pruning:</b> Lightly prune branches to improve airflow and reduce humidity.");
            } else if (rawResult.contains("Moderate")) {
                recommendation.append("<b>Severity: Moderate</b><br><br>")
                        .append("1. <b>Sanitation:</b> Remove and bury all infected pods immediately.<br>")
                        .append("2. <b>Thinning:</b> Prune the canopy to allow 50-70% sunlight penetration.<br>")
                        .append("3. <b>Fungicide:</b> Apply copper-based fungicides (e.g., Bordeaux mixture) every 2-4 weeks during rainy periods.");
            } else if (rawResult.contains("Severe")) {
                recommendation.append("<b>Severity: Severe</b><br><br>")
                        .append("1. <b>Intensive Pruning:</b> Heavy pruning of the tree and surrounding shade trees to maximize sunlight.<br>")
                        .append("2. <b>Systemic Fungicide:</b> Use systematic fungicide applications as recommended by local agricultural offices.<br>")
                        .append("3. <b>Drainage:</b> Improve field drainage to prevent waterlogging around the tree base.<br>")
                        .append("4. <b>Monitoring:</b> Inspect the farm every 3 days to catch new infections.");
            }
        } else if (rawResult.contains("Pod Borer")) {
            colorRes = R.color.pod_warning;
            if (rawResult.contains("Mild")) {
                recommendation.append("<b>Severity: Mild</b><br><br>")
                        .append("1. <b>Rampasan:</b> Harvest all pods (ripe and near-ripe) to break the pest life cycle.<br>")
                        .append("2. <b>Pruning:</b> Maintain a low canopy height (3-4m) for easier pest management.");
            } else if (rawResult.contains("Moderate")) {
                recommendation.append("<b>Severity: Moderate</b><br><br>")
                        .append("1. <b>Pod Sleeving:</b> Wrap young pods (6-7cm) with transparent plastic bags to prevent egg-laying.<br>")
                        .append("2. <b>Weekly Harvest:</b> Harvest every 7 days to remove pods before larvae can emerge.<br>")
                        .append("3. <b>Husk Disposal:</b> Break open husks and bury them immediately after harvest.");
            } else if (rawResult.contains("Severe")) {
                recommendation.append("<b>Severity: Severe</b><br><br>")
                        .append("1. <b>IPM Strategy:</b> Combine rampasan, pruning, sleeving, and balanced fertilization.<br>")
                        .append("2. <b>Biological Control:</b> Introduce natural predators like black ants (Dolichoderus thoracicus) if possible.<br>")
                        .append("3. <b>Pheromone Traps:</b> Use pheromone traps to monitor and reduce adult moth populations.");
            }
        } else if (rawResult.contains("Healthy")) {
            colorRes = R.color.pod_healthy;
            recommendation.append("<b>Your cacao pod appears healthy!</b><br><br>")
                    .append("• Continue regular monitoring every 2 weeks.<br>")
                    .append("• Maintain good farm sanitation and remove any fallen pods.<br>")
                    .append("• Ensure balanced fertilization to boost tree immunity.");
        } else {
            recommendation.append("• No cacao pod detected or image is unclear.<br>")
                    .append("• Please ensure the cacao pod is centered and well-lit.<br>")
                    .append("• Try taking the photo from a different angle.");
        }

        binding.tvRecommendation.setText(android.text.Html.fromHtml(recommendation.toString(), android.text.Html.FROM_HTML_MODE_LEGACY));
        binding.tvResult.setTextColor(getResources().getColor(colorRes, null));

        // Determine severity for history
        String severity = "N/A";
        if (rawResult.contains("Mild")) severity = "Mild";
        else if (rawResult.contains("Moderate")) severity = "Moderate";
        else if (rawResult.contains("Severe")) severity = "Severe";
        else if (rawResult.contains("Healthy")) severity = "Healthy";

        // Save to history
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        AppDatabase.getDatabase(getContext()).scanHistoryDao().insert(
                new ScanHistory(result, confidence, date, imagePath, severity)
        );
    }

    private String formatResult(String raw) {
        String clean = raw.replaceAll("^\\d+:", "").replace("_", " ");
        // Capitalize first letters
        StringBuilder result = new StringBuilder();
        String[] words = clean.split(" ");
        for (String word : words) {
            if (word.length() > 0) {
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
        if (classifier != null) {
            classifier.close();
        }
        binding = null;
    }
}