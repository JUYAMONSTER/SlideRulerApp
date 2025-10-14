package com.rhsdalya.slideruler

import android.content.Context
import android.opengl.GLES20
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ObjectRenderer {
    private var program = 0
    private var positionHandle = 0
    private var mvpMatrixHandle = 0
    private var colorHandle = 0
    private var pointSizeHandle = 0 // 점 크기를 조절하기 위한 핸들

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
        pointSizeHandle = GLES20.glGetUniformLocation(program, "u_PointSize")
    }

    fun draw(anchor: Anchor, viewMatrix: FloatArray, projectionMatrix: FloatArray, color: FloatArray) {
        val modelMatrix = FloatArray(16)
        anchor.pose.toMatrix(modelMatrix, 0)

        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glUniform1f(pointSizeHandle, 50.0f) // 점 크기를 50.0f로 설정

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, POINT_VERTEX_BUFFER)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1) // 점 하나를 그림

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    companion object {
        // 점 하나를 그리기 위한 셰이더 코드
        private const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
                gl_PointSize = u_PointSize;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        // 원점(0,0,0)에 있는 점 하나의 좌표
        private val POINT_VERTICES = floatArrayOf(0.0f, 0.0f, 0.0f)
        private val POINT_VERTEX_BUFFER: FloatBuffer =
            ByteBuffer.allocateDirect(POINT_VERTICES.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(POINT_VERTICES)
                    position(0)
                }
            }
    }
}