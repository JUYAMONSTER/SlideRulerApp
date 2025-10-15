package com.rhsdalya.slideruler

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.pow
import kotlin.math.sqrt

class ARMeasureActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private val tag = "ARMeasureActivity"

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var distanceText: TextView

    private var arSession: Session? = null
    private var userRequestedInstall = true

    private val backgroundRenderer = BackgroundRenderer()
    private val objectRenderer = ObjectRenderer()
    private val planeRenderer = PlaneRenderer()
    private val lineRenderer = LineRenderer()
    // ▼▼▼ 1. TextRenderer 변수 완전 삭제 ▼▼▼
    // private val textRenderer = TextRenderer()

    private val anchors = ArrayList<Anchor>()
    private val queuedTaps = ConcurrentLinkedQueue<MotionEvent>()

    private var viewportChanged = false
    private var viewportWidth = 0
    private var viewportHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_measure)
        surfaceView = findViewById(R.id.surfaceview)
        distanceText = findViewById(R.id.distance_text)
        setupSurfaceView()
    }

    private fun setupSurfaceView() {
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        surfaceView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                queuedTaps.offer(MotionEvent.obtain(event))
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()

        if (arSession == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }

            try {
                when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        arSession = Session(this)
                        val config = Config(arSession)
                        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                        arSession!!.configure(config)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        userRequestedInstall = false
                        return
                    }
                    else -> { showError("이 기기는 ARCore를 지원하지 않습니다."); return }
                }
            } catch (e: Exception) {
                showError("AR 세션 생성에 실패했습니다.", e); return
            }
        }

        try {
            val config = Config(arSession)
            if (arSession!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            if (config.focusMode != Config.FocusMode.AUTO) {
                config.focusMode = Config.FocusMode.AUTO
            }
            arSession!!.configure(config)

            val cameraConfigs = arSession!!.supportedCameraConfigs
            val bestConfig = cameraConfigs.filter { it.facingDirection == CameraConfig.FacingDirection.BACK }
                .maxByOrNull { it.imageSize.width * it.imageSize.height }
            bestConfig?.let { arSession!!.cameraConfig = it }

            arSession!!.resume()
        } catch (e: CameraNotAvailableException) {
            showError("카메라를 사용할 수 없습니다.", e); return
        } catch (e: Exception) {
            showError("AR 세션 설정 또는 재개 중 오류 발생", e); return
        }

        surfaceView.onResume()
        viewportChanged = true
    }

    override fun onPause() {
        super.onPause()
        arSession?.let {
            surfaceView.onPause()
            it.pause()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        objectRenderer.createOnGlThread(this)
        planeRenderer.createOnGlThread(this)
        lineRenderer.createOnGlThread(this)
        // ▼▼▼ 2. TextRenderer 초기화 코드 완전 삭제 ▼▼▼
        // textRenderer.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val session = arSession ?: return

        if (viewportChanged) {
            val displayRotation = windowManager.defaultDisplay.rotation
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }

        try {
            session.setCameraTextureName(backgroundRenderer.textureId)
            val frame = session.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)
            handleTap(frame)

            if (camera.trackingState == TrackingState.TRACKING) {
                val projectionMatrix = FloatArray(16)
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
                val viewMatrix = FloatArray(16)
                camera.getViewMatrix(viewMatrix, 0)

                planeRenderer.drawPlanes(session.getAllTrackables(Plane::class.java), viewMatrix, projectionMatrix)

                for (anchor in anchors) {
                    if (anchor.trackingState == TrackingState.TRACKING) {
                        objectRenderer.draw(anchor, viewMatrix, projectionMatrix, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f))
                    }
                }

                // ▼▼▼ 3. TextRenderer 관련 로직을 모두 2D UI 로직으로 변경 ▼▼▼
                if (anchors.size == 2) {
                    val start = anchors[0]
                    val end = anchors[1]

                    lineRenderer.draw(start.pose, end.pose, viewMatrix, projectionMatrix, floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f))

                    val distance = calculateDistance(start.pose, end.pose)
                    val distanceTextValue = "%.1f cm".format(distance * 100)

                    val midPose = getMiddlePose(start.pose, end.pose)
                    val screenPoint = worldToScreen(midPose, camera, viewportWidth, viewportHeight)

                    runOnUiThread {
                        distanceText.visibility = View.VISIBLE
                        distanceText.text = distanceTextValue
                        distanceText.x = screenPoint[0]
                        distanceText.y = screenPoint[1]
                    }
                } else {
                    runOnUiThread {
                        distanceText.visibility = View.GONE
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(tag, "onDrawFrame에서 예외 발생", t)
        }
    }

    private fun handleTap(frame: Frame) {
        val tap = queuedTaps.poll() ?: return
        try {
            if (frame.camera.trackingState == TrackingState.TRACKING) {
                for (hit in frame.hitTest(tap)) {
                    val trackable = hit.trackable
                    if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                        (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

                        if (anchors.size >= 2) {
                            anchors.forEach { it.detach() }
                            anchors.clear()
                        }
                        anchors.add(hit.createAnchor())
                        break
                    }
                }
            }
        } finally {
            tap.recycle()
        }
    }

    private fun getMiddlePose(p1: Pose, p2: Pose): Pose {
        val midX = (p1.tx() + p2.tx()) / 2
        val midY = (p1.ty() + p2.ty()) / 2
        val midZ = (p1.tz() + p2.tz()) / 2
        return Pose.makeTranslation(midX, midY, midZ)
    }

    private fun calculateDistance(pose1: Pose, pose2: Pose): Float {
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return sqrt(dx.pow(2) + dy.pow(2) + dz.pow(2))
    }

    // ▼▼▼ 4. 누락되었던 worldToScreen 함수 추가 ▼▼▼
    private fun worldToScreen(worldPose: Pose, camera: Camera, width: Int, height: Int): FloatArray {
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        val viewMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)

        val worldCoord = floatArrayOf(worldPose.tx(), worldPose.ty(), worldPose.tz(), 1f)
        val ndcCoord = FloatArray(4)
        Matrix.multiplyMV(ndcCoord, 0, viewMatrix, 0, worldCoord, 0)
        Matrix.multiplyMV(ndcCoord, 0, projectionMatrix, 0, ndcCoord, 0)

        val w = ndcCoord[3]
        ndcCoord[0] /= w
        ndcCoord[1] /= w

        return floatArrayOf(
            (ndcCoord[0] + 1.0f) * 0.5f * width,
            (1.0f - ndcCoord[1]) * 0.5f * height
        )
    }

    // ▼▼▼ 5. 누락되었던 함수들의 전체 내용 복원 ▼▼▼
    private fun requestCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showError(message: String, exception: Exception? = null) {
        val toastMessage = if (exception != null) "$message: ${exception.message}" else message
        Log.e(tag, toastMessage, exception)
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        finish()
    }
}