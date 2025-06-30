package com.example.anti_vol.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class CameraManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: CameraManager? = null

        fun getInstance(context: Context): CameraManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    interface CaptureCallback {
        fun onCaptureSuccess(photoFile: File)
        fun onCaptureError(error: String)
    }

    fun captureSelfieSilently(callback: CaptureCallback) {
        if (!hasCameraPermission()) {
            callback.onCaptureError("No camera permission")
            return
        }

        Thread {
            try {
                takePictureWithCamera2(callback)
            } catch (e: Exception) {
                callback.onCaptureError("Camera thread error: ${e.message}")
            }
        }.start()
    }

    private fun takePictureWithCamera2(callback: CaptureCallback) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        var backgroundThread: HandlerThread? = null
        var backgroundHandler: Handler? = null
        var cameraDevice: CameraDevice? = null
        var imageReader: ImageReader? = null

        try {
            backgroundThread = HandlerThread("CameraBackground")
            backgroundThread.start()
            backgroundHandler = Handler(backgroundThread.looper)

            var frontCameraId: String? = null
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId
                    break
                }
            }

            if (frontCameraId == null) {
                frontCameraId = cameraManager.cameraIdList.firstOrNull()
            }

            if (frontCameraId == null) {
                callback.onCaptureError("No cameras available")
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.JPEG)

            val size = jpegSizes?.minByOrNull { it.width * it.height } ?: Size(640, 480)
            imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)

            var imageSaved = false
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null && !imageSaved) {
                    imageSaved = true
                    backgroundHandler?.post {
                        val success = saveImage(image)
                        if (success != null) {
                            callback.onCaptureSuccess(success)
                        } else {
                            callback.onCaptureError("Failed to save image")
                        }

                        image.close()
                        cameraDevice?.close()
                        imageReader?.close()
                        backgroundThread?.quitSafely()
                    }
                }
            }, backgroundHandler)

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    try {
                        val surface = imageReader!!.surface
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                                        captureBuilder.addTarget(surface)
                                        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                                        session.capture(captureBuilder.build(), null, backgroundHandler)
                                    } catch (e: Exception) {
                                        callback.onCaptureError("Capture failed: ${e.message}")
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    callback.onCaptureError("Session configuration failed")
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        callback.onCaptureError("Session creation failed: ${e.message}")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    callback.onCaptureError("Camera error: $error")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            callback.onCaptureError("Camera2 setup error: ${e.message}")
        }
    }

    private fun saveImage(image: Image): File? {
        return try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)

            val photoFile = File(context.cacheDir, "theftselfie${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(photoFile)
            outputStream.write(bytes)
            outputStream.close()

            if (photoFile.exists() && photoFile.length() > 0) {
                photoFile
            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}