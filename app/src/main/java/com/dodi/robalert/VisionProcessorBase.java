package com.dodi.robalert;

import android.graphics.Bitmap;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dodi.robalert.common.VisionImageProcessor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.dodi.robalert.common.BitmapUtils;
import com.dodi.robalert.common.FrameMetadata;
import com.dodi.robalert.common.GraphicOverlay;

import java.nio.ByteBuffer;

public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private ByteBuffer latestImage;

    @GuardedBy("this")
    private FrameMetadata latestImageMetaData;

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private ByteBuffer processingImage;

    @GuardedBy("this")

    private FrameMetadata processingMetaData;

    public VisionProcessorBase() {
    }

    @Override
    public synchronized void process(
            ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay
            graphicOverlay) {
        latestImage = data;
        latestImageMetaData = frameMetadata;
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay);
        }
    }

    // Bitmap version
    @Override
    public void process(Bitmap bitmap, final GraphicOverlay
            graphicOverlay) {
        detectInVisionImage(null /* bitmap */, FirebaseVisionImage.fromBitmap(bitmap), null,
                graphicOverlay);
    }

    private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {
        processingImage = latestImage;
        processingMetaData = latestImageMetaData;
        latestImage = null;
        latestImageMetaData = null;
        if (processingImage != null && processingMetaData != null) {
            processImage(processingImage, processingMetaData, graphicOverlay);
        }
    }

    private void processImage(
            ByteBuffer data, final FrameMetadata frameMetadata,
            final GraphicOverlay graphicOverlay) {
        FirebaseVisionImageMetadata metadata =
                new FirebaseVisionImageMetadata.Builder()
                        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                        .setWidth(frameMetadata.getWidth())
                        .setHeight(frameMetadata.getHeight())
                        .setRotation(frameMetadata.getRotation())
                        .build();

        Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
        detectInVisionImage(
                bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata,
                graphicOverlay);
    }

    private void detectInVisionImage(
            final Bitmap originalCameraImage,
            FirebaseVisionImage image,
            final FrameMetadata metadata,
            final GraphicOverlay graphicOverlay) {
        detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<T>() {
                            @Override
                            public void onSuccess(T results) {
                                VisionProcessorBase.this.onSuccess(originalCameraImage, results,
                                        metadata,
                                        graphicOverlay);
                                processLatestImage(graphicOverlay);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                VisionProcessorBase.this.onFailure(e);
                            }
                        });
    }

    @Override
    public void stop() {
    }

    protected abstract Task<T> detectInImage(FirebaseVisionImage image);

    /**
     * Callback that executes with a successful detection result.
     *
     * @param originalCameraImage hold the original image from camera, used to draw the background
     *                            image.
     */
    protected abstract void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull T results,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay);

    protected abstract void onFailure(@NonNull Exception e);
}
