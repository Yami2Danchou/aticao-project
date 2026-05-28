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
    private static final String MODEL_FILE = "aticao_severity.tflite";
    private static final String LABEL_FILE = "labels_severity.txt";

    private final Interpreter interpreter;
    private final List<String> labels;
    private final ImageProcessor imageProcessor;

    public static class Recognition {
        public final String title;
        public final float confidence;

        public Recognition(String title, float confidence) {
            this.title = title;
            this.confidence = confidence;
        }
    }

    public CacaoClassifier(Context context) throws IOException {
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_FILE);
        interpreter = new Interpreter(tfliteModel);
        labels = FileUtil.loadLabels(context, LABEL_FILE);

        // Assuming model input size is 224x224. Adjust if necessary.
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0.0f, 255.0f))
                .build();
    }

    public Recognition classify(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        DataType outputDataType = interpreter.getOutputTensor(0).dataType();
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);

        interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer().rewind());

        Map<String, Float> labeledProbability = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
        
        String maxLabel = "";
        float maxProb = -1.0f;
        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue() > maxProb) {
                maxProb = entry.getValue();
                maxLabel = entry.getKey();
            }
        }

        return new Recognition(maxLabel, maxProb * 100);
    }

    public void close() {
        interpreter.close();
    }
}