## GlitchLab Studio

An advanced, hardware-accelerated cyberpunk and digital art photo editing suite for Android. Built entirely in modern **Kotlin Jetpack Compose**, GlitchLab combines mathematical color-matrix manipulation with local, on-device machine learning to create striking, high-contrast visual layers.

---

##  Core Architectural Features

* **On-Device Neural Target Isolation** Utilizes localized Google ML Kit vision pipelines to scan images and generate high-fidelity foreground subject masks on the client-side with zero network latency.
* **The Inverter Protocol** A dynamic layer-routing engine that allows artists to selectively isolate effects. Toggle corruption matrices completely between the background environment (`WORLD`) or constrain the digital artifacts exclusively inside the subject's outline (`SUBJECT`).
* **The Sin City Protocol** An intelligent color-isolation filter that drops target environments into a gritty, high-contrast grayscale dimension while preserving the subject's authentic, hyper-saturated color depth.
* **Hollow Neon Edge Contours** Employs an offscreen canvas matrix rendering layout using a multi-pass stencil alpha mask process (`PorterDuff.Mode.DST_OUT`). This creates a crisp, glowing laser ring light contour mapped tightly to the subject's silhouette bounds without filling the core shape.
* **Adaptive Viewport Geometry** Features an aspect-ratio calculation engine that runtime-samples image data, automatically locking the editing viewport container to eradicate pointer coordinate drifting across varying image sizes.
* **Interactive Pro Studio Layers** A custom user-driven vector asset pipeline enabling full placement, fluid sizing handle constraints, and color-tinting overlays for glowing neon bars, custom typography, and hex-encrypted binary streams.
* **Hardware Matrix Shader Suite** 20 custom, high-performance preset configurations manipulating color vectors using mathematical color matrices with real-time intensity and overdrive multiplier curves.

---

##  Technical Stack

* **Language:** 100% Kotlin
* **UI Framework:** Jetpack Compose (Declarative UI State Layout Engine)
* **AI Engine:** Google ML Kit Subject Segmentation Pipeline
* **Asynchronous Processing:** Kotlin Coroutines & Architecture Lifecycle Scopes
* **Graphics Pipeline:** Android Native Canvas & Hardware Accelerated `ColorMatrixColorFilter`
* **Image Loading:** Coil (Coroutines Image Loader)

---

## Installation & Setup

1. **Clone the Repo:**
   ```bash
   git clone https://github.com/akhtarsaaeem-sys/GlitchLab-Studio.git


 Developed by Saaeem Akhtar.
