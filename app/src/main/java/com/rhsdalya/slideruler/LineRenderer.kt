package com.rhsdalya.slideruler

import android.content.Context
import android.opengl.GLES20
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class LineRenderer {

    private var program = 0
    private var positionHandle = 0
    private var mvpMatrixHandle = 0
    private var colorHandle = 0

    private lateinit var vertexBuffer: FloatBuffer

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
    }

    fun draw(startPose: Pose, endPose: Pose, viewMatrix: FloatArray, projectionMatrix: FloatArray, color: FloatArray) {
        val lineVertices = floatArrayOf(
            startPose.tx(), startPose.ty(), startPose.tz(),
            endPose.tx(), endPose.ty(), endPose.tz()
        )

        val bb = ByteBuffer.allocateDirect(lineVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(lineVertices)
        vertexBuffer.position(0)

        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glLineWidth(10.0f) // 선의 두께를 설정
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2) // 두 점을 잇는 선을 그림

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """
    }
}