/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.views;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import org.ros.exception.RosRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class CameraPreviewView extends ViewGroup {

  private final static double ASPECT_TOLERANCE = 0.1;

  private final ByteArrayOutputStream stream = new ByteArrayOutputStream(512);

  private SurfaceHolder surfaceHolder;
  private Size previewSize;
  private Camera camera;
  private PreviewCallback previewCallback;

  private final class BufferingPreviewCallback implements PreviewCallback {

    private final byte[] previewBuffer;
    private final YuvImage yuvImage;
    private final Rect rect;

    public BufferingPreviewCallback(byte[] previewBuffer) {
      this.previewBuffer = previewBuffer;
      yuvImage =
          new YuvImage(previewBuffer, ImageFormat.NV21, previewSize.width, previewSize.height, null);
      rect = new Rect(0, 0, previewSize.width, previewSize.height);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera unused) {
      Preconditions.checkNotNull(camera);
      Preconditions.checkArgument(data == previewBuffer);
      Preconditions.checkState(yuvImage.compressToJpeg(rect, 80, stream));
      if (previewCallback != null) {
        previewCallback.onPreviewFrame(stream.toByteArray(), camera);
        stream.reset();
      }
      camera.addCallbackBuffer(previewBuffer);
    }
  }

  private final class SurfaceHolderCallback implements SurfaceHolder.Callback {
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      try {
        if (camera != null) {
          camera.setPreviewDisplay(holder);
        }
      } catch (IOException e) {
        throw new RosRuntimeException(e);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      releaseCamera();
    }
  }

  private void init(Context context) {
    SurfaceView surfaceView = new SurfaceView(context);
    addView(surfaceView);
    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(new SurfaceHolderCallback());
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  public CameraPreviewView(Context context) {
    super(context);
    init(context);
  }

  public CameraPreviewView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public void releaseCamera() {
    if (camera == null) {
      return;
    }
    camera.setPreviewCallbackWithBuffer(null);
    camera.stopPreview();
    camera.release();
    camera = null;
  }

  public void setPreviewCallback(PreviewCallback previewCallback) {
    this.previewCallback = previewCallback;
  }

  public void setCamera(Camera camera) {
    Preconditions.checkNotNull(camera);
    this.camera = camera;
    setupCameraParameters();
    setupBufferingPreviewCallback();
    camera.startPreview();
    try {
      // This may have no effect if the SurfaceHolder is not yet created.
      camera.setPreviewDisplay(surfaceHolder);
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
  }

  private void setupCameraParameters() {
    Camera.Parameters parameters = camera.getParameters();
    List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
    previewSize = getOptimalPreviewSize(supportedPreviewSizes, getWidth(), getHeight());
    parameters.setPreviewSize(previewSize.width, previewSize.height);
    parameters.setPreviewFormat(ImageFormat.NV21);
    camera.setParameters(parameters);
  }

  private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
    Preconditions.checkNotNull(sizes);
    double targetRatio = (double) width / height;
    double minimumDifference = Double.MAX_VALUE;
    Size optimalSize = null;

    // Try to find a size that matches the aspect ratio and size.
    for (Size size : sizes) {
      double ratio = (double) size.width / size.height;
      if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
        continue;
      }
      if (Math.abs(size.height - height) < minimumDifference) {
        optimalSize = size;
        minimumDifference = Math.abs(size.height - height);
      }
    }

    // Cannot find one that matches the aspect ratio, ignore the requirement.
    if (optimalSize == null) {
      minimumDifference = Double.MAX_VALUE;
      for (Size size : sizes) {
        if (Math.abs(size.height - height) < minimumDifference) {
          optimalSize = size;
          minimumDifference = Math.abs(size.height - height);
        }
      }
    }

    Preconditions.checkNotNull(optimalSize);
    return optimalSize;
  }

  private void setupBufferingPreviewCallback() {
    int format = camera.getParameters().getPreviewFormat();
    int bits_per_pixel = ImageFormat.getBitsPerPixel(format);
    byte[] previewBuffer = new byte[previewSize.height * previewSize.width * bits_per_pixel / 8];
    camera.addCallbackBuffer(previewBuffer);
    camera.setPreviewCallbackWithBuffer(new BufferingPreviewCallback(previewBuffer));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (changed && getChildCount() > 0) {
      final View child = getChildAt(0);
      final int width = r - l;
      final int height = b - t;

      int previewWidth = width;
      int previewHeight = height;
      if (previewSize != null) {
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;
      }

      // Center the child SurfaceView within the parent.
      if (width * previewHeight > height * previewWidth) {
        final int scaledChildWidth = previewWidth * height / previewHeight;
        child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
      } else {
        final int scaledChildHeight = previewHeight * width / previewWidth;
        child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
      }
    }
  }
}
