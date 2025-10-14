package com.rhsdalya.slideruler

import android.content.Context
import android.opengl.GLES20
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PlaneRenderer {
    private var program = 0
    private var positionHandle = 0
    private var mvpMatrixHandle = 0
    private var colorHandle = 0
    private var dotPositionHandle = 0

    fun createOnGlThread(context: Context) {
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also { shader ->
            GLES20.glShaderSource(shader, VERTEX_SHADER)
            GLES20.glCompileShader(shader)
        }
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also { shader ->
            GLES20.glShaderSource(shader, FRAGMENT_SHADER)
            GLES20.glCompileShader(shader)
        }

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MvpMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
        dotPositionHandle = GLES20.glGetUniformLocation(program, "u_DotPosition")
    }

    fun drawPlanes(planes: Collection<Plane>, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        for (plane in planes) {
            if (plane.trackingState != com.google.ar.core.TrackingState.TRACKING || plane.subsumedBy != null) {
                continue
            }

            val pose = plane.centerPose
            val modelMatrix = FloatArray(16)
            pose.toMatrix(modelMatrix, 0)

            val mvpMatrix = FloatArray(16)
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            val center = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
            GLES20.glUniform3fv(dotPositionHandle, 1, center, 0)

            GLES20.glUniform4fv(colorHandle, 1, GRID_COLOR, 0)

            // 더 이상 사용하지 않음: GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    companion object {
        private val GRID_COLOR = floatArrayOf(1.0f, 1.0f, 1.0f, 0.5f) // 흰색, 반투명

        private const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            varying vec3 v_WorldPosition;
            void main() {
                v_WorldPosition = a_Position.xyz;
                gl_Position = u_MvpMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            uniform vec3 u_DotPosition;
            varying vec3 v_WorldPosition;
            void main() {
                float dist = length(v_WorldPosition - u_DotPosition);
                float alpha = 0.5 * (1.0 - smoothstep(0.4, 0.5, dist));
                gl_FragColor = vec4(u_Color.rgb, alpha);
            }
        """
    }
}