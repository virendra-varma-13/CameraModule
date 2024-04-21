package com.virendra.cameramodule.screens.recording

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.ListenableFuture
import com.virendra.cameramodule.R
import com.virendra.cameramodule.constants.SharedPrefConstants
import com.virendra.cameramodule.databinding.FragmentRecordingBinding
import com.virendra.cameramodule.utils.SharedPrefsManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class RecordingFragment : Fragment() {

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private var service: ExecutorService? = null
    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var scaleFactor = 1f

    private var isPause = false

    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private val activityResultLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean? ->
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera(cameraFacing)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireActivity(),R.color.teal_700);

        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.capture.setOnClickListener { view: View? ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.CAMERA)
            } else if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                captureVideo()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }

        binding.flipCamera.setOnClickListener {
            if (recording != null) {
                Toast.makeText(
                    requireContext(),
                    "Stop the recording, then try to flip the camera",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera(cameraFacing)
        }

        binding.playPauseButton.setOnClickListener{
            if(!isPause && recording != null){
                isPause = true
                recording!!.pause()
                binding.playPauseButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_play))
            } else if(recording != null){
                recording!!.resume()
                isPause = false
                binding.playPauseButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_pause))
            }
        }

        binding.imgBack.setOnClickListener {
            if(recording != null)
                captureVideo()
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        service = Executors.newSingleThreadExecutor()
    }

    private fun captureVideo() {
        binding.capture.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.round_stop_circle
            )
        )

        val recording1 = recording
        if (recording1 != null) {
            recording1.stop()
            recording = null
            binding.playPauseButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_pause))
            binding.playPauseButton.visibility = View.GONE
            return
        }
        val name: String = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(
            System.currentTimeMillis()
        )
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        val options = MediaStoreOutputOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues).build()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        recording =
            videoCapture!!.output.prepareRecording(requireContext(), options).withAudioEnabled()
                .start(
                    ContextCompat.getMainExecutor(requireContext())
                ) { videoRecordEvent: VideoRecordEvent? ->
                    if (videoRecordEvent is VideoRecordEvent.Start) {
                        binding.capture.isEnabled = true
                        binding.playPauseButton.visibility = View.VISIBLE
                    } else if (videoRecordEvent is VideoRecordEvent.Finalize) {
                        if (!(videoRecordEvent as VideoRecordEvent.Finalize).hasError()) {
                            val uri = (videoRecordEvent as VideoRecordEvent.Finalize).outputResults
                                .outputUri
                            val msg = "Video capture succeeded: $uri"
                            updateRecordingList(uri)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        } else {
                            recording!!.close()
                            recording = null
                            val msg =
                                "Error: " + (videoRecordEvent as VideoRecordEvent.Finalize).error
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                        binding.capture.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.round_fiber_manual_record_24
                            )
                        )
                    }
                }
    }

    private fun updateRecordingList(uri: Uri) {
        var existingList = SharedPrefsManager.newInstance(requireContext())
            .getStringSet(SharedPrefConstants.VIDEO_LIST_KEY)
        if (existingList == null)
            existingList = mutableSetOf()


        val resolver: ContentResolver = requireActivity().contentResolver
        val cursor = resolver.query(uri, null, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                existingList.add(uri.toString())
                SharedPrefsManager.newInstance(requireContext())
                    .storeVideoList(SharedPrefConstants.VIDEO_LIST_KEY, existingList)
            } else{
                Toast.makeText(
                    requireContext(),
                    "Unable to getPath From URI, Try again later",
                    Toast.LENGTH_SHORT
                ).show()
            }
            cursor.close()
        } else{
            Toast.makeText(
                requireContext(),
                "Unable to getPath From URI, Try again later",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        service!!.shutdown()
        camera = null
    }

    private fun startCamera(cameraFacing: Int) {
        val processCameraProvider: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(requireContext())
        processCameraProvider.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = processCameraProvider.get()
                val preview =
                    Preview.Builder().build()
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider())
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing).build()


                camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)

                binding.toggleFlash.setOnClickListener { view: View? ->
                    toggleFlash(
                        camera!!
                    )
                }

                scaleGestureDetector = ScaleGestureDetector(requireContext(), ScaleListener())

                binding.previewView.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                        if (p1 != null)
                            scaleGestureDetector!!.onTouchEvent(p1)
                        return true
                    }

                })
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleFlash(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                binding.toggleFlash.setImageResource(R.drawable.round_flash_off_24)
            } else {
                camera.cameraControl.enableTorch(false)
                binding.toggleFlash.setImageResource(R.drawable.round_flash_on_24)
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Flash is not available currently",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 1.0f))

            // Adjust zoom level
            setZoom(scaleFactor)
            return true
        }
    }

    private fun setZoom(zoomRatio: Float) {
        if (camera != null) {
            camera!!.cameraControl.setZoomRatio(zoomRatio)
        }
    }
}