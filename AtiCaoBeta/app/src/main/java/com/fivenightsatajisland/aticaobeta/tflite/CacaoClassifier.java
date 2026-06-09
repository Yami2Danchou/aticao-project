package com.fivenightsatajisland.aticaobeta.tflite;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;

public class CacaoClassifier {
    private final Interpreter interpreter;
    private final List<String> labels;
    private ImageProcessor imageProcessor;
    private final int inputSize;
    private final boolean isQuantized;

    public static class Recognition {
        public final String title;
        public final float confidence;

        public Recognition(String title, float confidence) {
            this.title = title;
            this.confidence = confidence;
        }
    }

    public CacaoClassifier(Context context, String modelFile, String labelFile) throws IOException {
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, modelFile);
        Interpreter.Options options = new Interpreter.Options();
        interpreter = new Interpreter(tfliteModel, options);
        labels = FileUtil.loadLabels(context, labelFile);

        // Dynamically get input shape
        int[] inputShape = interpreter.getInputTensor(0).shape(); // {1, height, width, 3}
        inputSize = inputShape[1];
        
        // Check if model is quantized
        isQuantized = interpreter.getInputTensor(0).dataType() == DataType.UINT8;

        setupImageProcessor();
    }

    private void setupImageProcessor() {
        ImageProcessor.Builder builder = new ImageProcessor.Builder()
                .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR));

        if (!isQuantized) {
            // Standard normalization for many TFLite classification models (maps 0-255 to -1 to 1)
            // If the model was trained with 0-1, this might need to be (0.0f, 255.0f)
            // But 127.5f is more common for MobileNet-based transfer learning
            builder.add(new NormalizeOp(127.5f, 127.5f));
        }

        imageProcessor = builder.build();
    }

    public Recognition classify(Bitmap bitmap) {
        DataType inputDataType = interpreter.getInputTensor(0).dataType();
        TensorImage tensorImage = new TensorImage(inputDataType);
        tensorImage.load(bitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        DataType outputDataType = interpreter.getOutputTensor(0).dataType();
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);

        interpreter.run(processedImage.getBuffer(), outputBuffer.getBuffer().rewind());

        Map<String, Float> labeledProbability = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();

        String maxLabel = "";
        float maxProb = -1.0f;
        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue() > maxProb) {
                maxProb = entry.getValue();
                maxLabel = entry.getKey();
            }
        }

        // Return probability as percentage
        float confidence = maxProb * 100;
        
        // If the model is quantized and getMapWithFloatValue didn't dequantize (unlikely with support library),
        // we would see values up to 255.
        if (isQuantized && maxProb > 1.0f) {
            confidence = (maxProb / 255.0f) * 100;
        }

        return new Recognition(maxLabel, confidence);
    }

    public void close() {
        interpreter.close();
    }
}