## 한글 버전 (Korean Version)
AR 줄자 앱 (SlideRulerApp) 📏
Android ARCore를 활용하여 실제 공간의 길이를 측정하는 간단한 줄자 애플리케이션입니다. 이 프로젝트는 AR 기능 구현의 기초를 학습하고, 실제 사용 가능한 도구를 만드는 것을 목표로 합니다.

(앱의 실제 작동 모습을 보여주는 GIF나 스크린샷을 여기에 추가하면 좋습니다.)

## ✨ 주요 기능 (초기 버전)
현재 버전은 다음과 같은 핵심 기능을 포함하고 있습니다.

실시간 AR 뷰: 카메라를 통해 현실 공간을 보면서 AR 기능을 사용할 수 있습니다.

평면 인식: ARCore를 이용해 바닥, 책상, 벽과 같은 수평/수직 평면을 자동으로 감지하고 시각적으로 표시합니다.

지점 생성 (앵커): 인식된 평면 위를 터치하여 측정을 위한 기준점(앵커)을 생성합니다.

거리 계산 및 표시: 생성된 두 지점 사이의 직선 거리를 cm 단위로 실시간 계산하여 화면에 표시합니다.

시각화: 측정된 두 지점을 잇는 선과 3D 포인터를 그려, 측정 중인 구간을 명확하게 보여줍니다.

측정 초기화: 측정이 완료된 후 화면을 다시 터치하면 기존 측정 내역이 초기화되고 새로운 측정을 시작할 수 있습니다.

## 🛠️ 사용한 기술
언어: Kotlin

플랫폼: Android SDK

핵심 기술: ARCore SDK

렌더링: OpenGL ES 2.0 (카메라 배경, 평면, 측정 지점 및 선 직접 렌더링)

## 📖 사용 방법
앱을 실행하고 '길이 측정 시작하기' 버튼을 누릅니다.

카메라를 바닥이나 벽 등 평평한 곳으로 향하고 천천히 움직여 평면을 인식시킵니다.

화면에 반투명한 격자무늬(인식된 평면)가 나타나면, 측정하고 싶은 시작점을 터치하여 첫 번째 점을 찍습니다.

측정하고 싶은 끝점을 터치하여 두 번째 점을 찍습니다.

두 점 사이에 선이 그려지고, 화면에 두 점 사이의 거리가 cm 단위로 표시됩니다.

다시 측정하려면 화면을 터치하여 초기화합니다.

## 🚀 향후 업데이트 계획
이 프로젝트는 다음과 같은 기능 개선을 목표로 하고 있습니다.

카메라 자동 초점 맞춤:

문제: 현재 고정 초점 방식으로 인해, 약 10cm 이내의 가까운 물체는 이미지가 흐려져 평면 인식이 어렵습니다.

개선 목표: ARCore 세션에 자동 초점(Auto Focus) 모드를 활성화하여, 가까운 거리에서도 선명한 이미지를 얻고 측정 정확도를 높입니다.

UI/UX 업데이트:

문제: 현재 UI는 기능 구현에만 초점이 맞춰져 있어 직관성이 다소 떨어집니다.

개선 목표:

'측정 시작', '측정 종료', '초기화' 버튼을 명확하게 분리하여 사용자 실수를 줄입니다.

측정된 길이를 표시하는 텍스트 디자인을 개선하고, 단위(cm/m/inch) 변환 기능을 추가합니다.

길이 측정 오차 조정:

문제: ARCore의 평면 인식은 완벽하지 않으며, 이로 인해 측정 시 약간의 오차가 발생할 수 있습니다.

개선 목표: 여러 프레임에 걸쳐 앵커의 위치를 평균 내어 안정성을 높이는 필터링 로직을 추가하여 측정값의 떨림 현상을 줄입니다.

## English Version
AR Tape Measure App (SlideRulerApp) 📏
A simple tape measure application that measures lengths in the real world using Android ARCore. This project aims to teach the fundamentals of implementing AR features while building a practical tool.

(It's recommended to add a GIF or screenshot of the app in action here.)

## ✨ Key Features (Initial Version)
The current version includes the following core features:

Real-time AR View: Utilizes the device's camera to display the real world and overlay AR content.

Plane Detection: Automatically detects and visualizes horizontal and vertical surfaces like floors, tables, and walls using ARCore.

Point Creation (Anchors): Allows users to create anchor points on detected planes by tapping the screen.

Distance Calculation & Display: Calculates the distance between two created points in real-time and displays it in centimeters (cm).

Visualization: Renders the placed points as 3D objects and draws a line between them to clearly show the measured segment.

Measurement Reset: Tapping the screen after a measurement is complete clears the existing points and line, allowing for a new measurement to begin.

## 🛠️ Tech Stack
Language: Kotlin

Platform: Android SDK

Core Technology: ARCore SDK

Rendering: OpenGL ES 2.0 (for rendering the camera feed, detected planes, points, and lines)

## 📖 How to Use
Launch the app and tap the "Start Measurement" button.

Point the camera towards a flat surface (like a floor or wall) and move the device slowly to allow ARCore to detect the plane.

Once a semi-transparent grid appears on a surface, tap the desired starting point to place the first anchor.

Tap the desired endpoint to place the second anchor.

A line will be drawn between the two points, and the distance in centimeters will be displayed on the screen.

To start a new measurement, simply tap the screen again to clear the previous one.

## 🚀 Future Updates
This project is planned to be improved with the following features:

Automatic Camera Focusing:

Issue: The current fixed-focus mode makes it difficult to recognize planes on objects closer than ~10cm because the image becomes blurry.

Goal: Enable Auto Focus mode in the ARCore session to get a clear image of nearby objects, thereby improving measurement accuracy at close range.

UI/UX Overhaul:

Issue: The current user interface is basic and focused solely on functionality.

Goal:

Introduce distinct buttons for "Start," "End," and "Reset" to make the measurement process more intuitive.

Improve the design of the distance display and add a unit conversion feature (e.g., cm / m / inch).

Measurement Accuracy Calibration:

Issue: ARCore's plane detection is not always perfect, which can lead to minor inaccuracies and jitter in measurements.

Goal: Implement a filtering logic that averages anchor positions over multiple frames to reduce jitter and enhance the stability of the measurement value.
