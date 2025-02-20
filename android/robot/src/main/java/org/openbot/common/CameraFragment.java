package org.openbot.common;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openbot.R;
import org.openbot.env.ImageUtils;
import org.openbot.utils.Constants;
import org.openbot.utils.Enums;
import org.openbot.utils.PermissionUtils;
import org.openbot.utils.YuvToRgbConverter;
import timber.log.Timber;

public abstract class CameraFragment extends ControlsFragment {

  private ExecutorService cameraExecutor;
  private PreviewView previewView;
  private Preview preview;
  protected int lensFacing;
  private ProcessCameraProvider cameraProvider;
  private Size analyserResolution = Enums.Preview.HD.getValue();
  private YuvToRgbConverter converter;
  private Bitmap bitmapBuffer;
  private int rotationDegrees;

  protected View inflateFragment(int resId, LayoutInflater inflater, ViewGroup container) {
    return addCamera(inflater.inflate(resId, container, false), inflater, container);
  }

  protected View inflateFragment(
      ViewBinding viewBinding, LayoutInflater inflater, ViewGroup container) {
    return addCamera(viewBinding.getRoot(), inflater, container);
  }

  private View addCamera(View view, LayoutInflater inflater, ViewGroup container) {
    View cameraView = inflater.inflate(R.layout.fragment_camera, container, false);
    ViewGroup rootView = (ViewGroup) cameraView.getRootView();
    // Establezca lensFacing a partir de las preferencias del usuario (última configuración utilizada)
    lensFacing =
        preferencesManager.getCameraSwitch()
            ? CameraSelector.LENS_FACING_FRONT
            : CameraSelector.LENS_FACING_BACK;
    previewView = cameraView.findViewById(R.id.viewFinder);
    rootView.addView(view);

    if (!PermissionUtils.hasCameraPermission(requireActivity())) {
      requestPermissionLauncherCamera.launch(Constants.PERMISSION_CAMERA);
    } else if (PermissionUtils.shouldShowRational(requireActivity(), Constants.PERMISSION_CAMERA)) {
      PermissionUtils.showCameraPermissionsPreviewToast(requireActivity());
    } else {
      setupCamera();
    }
    return cameraView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    cameraExecutor = Executors.newSingleThreadExecutor();
  }

  //Configura y vincula los casos de uso de la cámara, como la vista previa y el análisis de imágenes.
  @SuppressLint("RestrictedApi")
  private void setupCamera() {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
        ProcessCameraProvider.getInstance(requireContext());

    cameraProviderFuture.addListener(
        () -> {
          try {
            cameraProvider = cameraProviderFuture.get();
            bindCameraUseCases();
          } catch (ExecutionException | InterruptedException e) {
            Timber.e("Camera setup failed: %s", e.toString());
          }
        },
        ContextCompat.getMainExecutor(requireContext()));
  }

  //Flujo de procesamiento de imágenes
  @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
  private void bindCameraUseCases() {
    converter = new YuvToRgbConverter(requireContext());
    bitmapBuffer = null;
    //Aspecto de 16:9.
    preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build();
    // Determina si la orientación de la pantalla es horizontal o vertical
    final boolean rotated = ImageUtils.getScreenOrientation(requireActivity()) % 180 == 90;
    //Se ajusta el PreviewView dependiendo de la orientacion
    final PreviewView.ScaleType scaleType =
        rotated ? PreviewView.ScaleType.FIT_CENTER : PreviewView.ScaleType.FIT_START;
    previewView.setScaleType(scaleType);
    preview.setSurfaceProvider(previewView.getSurfaceProvider());
    //Selecciona la camara frontal o trasera
    CameraSelector cameraSelector =
        new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    ImageAnalysis imageAnalysis;

    //Se define la resolucion, por defecto en un aspecto 16:9
    if (analyserResolution == null)
      imageAnalysis =
          new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build();
    else
      imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(analyserResolution).build();

    //Asigna un analizador de imágenes que se ejecuta
    imageAnalysis.setAnalyzer(
        cameraExecutor,
        image -> {
          if (bitmapBuffer == null)
            bitmapBuffer =
                Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

          rotationDegrees = image.getImageInfo().getRotationDegrees();  //Obtiene los grados de rotación de la imagen.
          converter.yuvToRgb(image.getImage(), bitmapBuffer);   //Convierte la imagen de formato YUV a RGB y la almacena en bitmapBuffer
          image.close(); //Libera la imagen después de procesarla.

          //Metodo que se llama para pintar el contornor de la imagen
          processFrame(bitmapBuffer, image);
        });
    try {
      if (cameraProvider != null) {
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
      }
    } catch (Exception e) {
      Timber.e("Use case binding failed: %s", e.toString());
    }
  }

  public int getRotationDegrees() {
    return rotationDegrees;
  }

// Lanza una solicitud de permisos para la cámara
  private final ActivityResultLauncher<String> requestPermissionLauncherCamera =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          isGranted -> {
            if (isGranted) {
              setupCamera();
            } else if (PermissionUtils.shouldShowRational(
                requireActivity(), Constants.PERMISSION_CAMERA)) {
              PermissionUtils.showCameraPermissionsPreviewToast(requireActivity());
            } else {

            }
          });

  @Override
  public void onDestroy() {
    super.onDestroy();
    cameraExecutor.shutdown();
  }
  //Devuelve el tamaño de la vista previa.
  @SuppressLint("RestrictedApi")
  public Size getPreviewSize() {
    return preview.getAttachedSurfaceResolution();
  }

  //Devuelve el tamaño máximo de la imagen analizada.
  public Size getMaxAnalyseImageSize() {

    return new Size(bitmapBuffer.getWidth(), bitmapBuffer.getHeight());
  }

  //Cambia entre la cámara frontal y la trasera.
  public void toggleCamera() {
    lensFacing =
        CameraSelector.LENS_FACING_FRONT == lensFacing
            ? CameraSelector.LENS_FACING_BACK
            : CameraSelector.LENS_FACING_FRONT;
    preferencesManager.setCameraSwitch(!preferencesManager.getCameraSwitch());
    bindCameraUseCases();
  }

  //Establece la resolución del analizador de imágenes.
  public void setAnalyserResolution(Size resolutionSize) {
    if (resolutionSize == null) analyserResolution = null;
    else {
      //Obtiene la orientacion de la pantalla y verifica si es horizontal
      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        this.analyserResolution = new Size(resolutionSize.getHeight(), resolutionSize.getWidth());
      else this.analyserResolution = resolutionSize;
    }
    bindCameraUseCases();
  }

  protected abstract void processFrame(Bitmap image, ImageProxy imageProxy);
}
