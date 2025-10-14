package com.rhsdalya.slideruler

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BackgroundRenderer {
    private val TAG = BackgroundRenderer::class.java.simpleName

    private lateinit var quadTexCoords: FloatBuffer

    private var program = 0
    private var positionHandle = -1
    private var texCoordHandle = -1
    private var textureHandle = -1

    var textureId = -1
        private set

    /**
     * OpenGL 스레드에서 카메라 텍스처를 초기화
     * 안전성: 내부 에러를 던지지 않고 로그만 남기고 실패 상태로 둠
     */
    fun createOnGlThread() {
        try {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            // Vertex shader
            val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
            GLES20.glShaderSource(vertexShader, VERTEX_SHADER)
            GLES20.glCompileShader(vertexShader)
            if (!checkShaderSafe(vertexShader, "Vertex shader")) {
                GLES20.glDeleteShader(vertexShader)
                // mark failure and return
                program = 0
                return
            }

            // Fragment shader
            val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
            GLES20.glShaderSource(fragmentShader, FRAGMENT_SHADER)
            GLES20.glCompileShader(fragmentShader)
            if (!checkShaderSafe(fragmentShader, "Fragment shader")) {
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                program = 0
                return
            }

            // Program
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            if (!checkProgramSafe(program, "Shader program")) {
                GLES20.glDeleteProgram(program)
                program = 0
                return
            }

            // attribute / uniform locations
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
            texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
            textureHandle = GLES20.glGetUniformLocation(program, "sTexture")

            // 초기화: quadTexCoords 버퍼는 여기서 만들어 둡니다 (draw에서 매번 생성하지 않음)
            val bbTexCoords = ByteBuffer.allocateDirect(QUAD_TEXCOORDS_TRANSFORMED.size * FLOAT_SIZE)
            bbTexCoords.order(ByteOrder.nativeOrder())
            quadTexCoords = bbTexCoords.asFloatBuffer()
            quadTexCoords.put(QUAD_TEXCOORDS_TRANSFORMED)
            quadTexCoords.position(0)

            Log.d(TAG, "BackgroundRenderer 초기화 완료, textureId=$textureId, program=$program")
        } catch (t: Throwable) {
            Log.e(TAG, "createOnGlThread 예외: ${t.message}", t)
            // 실패 상태로 표시
            program = 0
            textureId = -1
        }
    }

    /**
     * 카메라 프레임을 화면에 렌더링
     */
    fun draw(frame: Frame) {
        // 준비가 안되어 있으면 아무것도 하지 않음 (앱 크래시 방지)
        if (program == 0 || textureId == -1) {
            // 아직 셰이더나 텍스처가 준비되지 않았음
            return
        }

        // quadTexCoords는 createOnGlThread에서 초기화되므로 안전하게 사용
        try {
            frame.transformDisplayUvCoords(QUAD_TEXCOORDS_TRANSFORMED_BUFFER, quadTexCoords)
        } catch (t: Throwable) {
            Log.w(TAG, "transformDisplayUvCoords 실패: ${t.message}")
            // UV 변환 실패 시에도 안전하게 리턴
            return
        }

        // 렌더링 단계
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            QUAD_COORDS_BUFFER
        )
        GLES20.glEnableVertexAttribArray(positionHandle)

        GLES20.glVertexAttribPointer(
            texCoordHandle,
            TEXCOORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            quadTexCoords
        )
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    // ------------------------------
    // Shader / Program 체크 유틸 (예외 대신 boolean 리턴)
    // ------------------------------
    private fun checkShaderSafe(shader: Int, label: String): Boolean {
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "$label compile error: $info")
            return false
        }
        return true
    }

    private fun checkProgramSafe(program: Int, label: String): Boolean {
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val info = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "$label link error: $info")
            return false
        }
        return true
    }

    companion object {
        private const val COORDS_PER_VERTEX = 2
        private const val TEXCOORDS_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4

        // 전체 화면 렌더링용 정점 좌표 (OpenGL 좌표계)
        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
        )

        private val QUAD_COORDS_BUFFER: FloatBuffer =
            ByteBuffer.allocateDirect(QUAD_COORDS.size * FLOAT_SIZE).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(QUAD_COORDS)
                    position(0)
                }
            }

        // 기본 텍스처 좌표 (0~1)
        private val QUAD_TEXCOORDS_TRANSFORMED = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
        )

        private val QUAD_TEXCOORDS_TRANSFORMED_BUFFER: FloatBuffer =
            ByteBuffer.allocateDirect(QUAD_TEXCOORDS_TRANSFORMED.size * FLOAT_SIZE).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(QUAD_TEXCOORDS_TRANSFORMED)
                    position(0)
                }
            }

        // ------------------------------
        // GLSL 셰이더 코드
        // ------------------------------
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """
    }
}
