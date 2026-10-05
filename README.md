# 🛡️ SurakshaKavach — Industrial Edge AI Lineman Safety App

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin)](https://kotlinlang.org/)
[![TensorFlow Lite](https://img.shields.io/badge/TFLite-INT8%20Quantized%20(3.3MB)-orange?logo=tensorflow)](https://www.tensorflow.org/lite)
[![YOLOv8](https://img.shields.io/badge/YOLOv8n-Object%20Detection-blue)](https://ultralytics.com)
[![CameraX](https://img.shields.io/badge/Android-CameraX-green?logo=android)](https://developer.android.com/training/camerax)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Developed for Tata Power Central Odisha Distribution Limited (TPCODL)**  
> *Project*: Automated PPE Compliance & Safety Verification System using AI/ML & Mobile Integration  
> *Author*: **Rohan Verma** (KIIT University)  
> *Mentor*: Mr. Anubhab Ray (TPCODL)

---

## 📌 Overview

Linemen working on high-voltage electrical distribution infrastructure face life-threatening electrical hazards daily. While safety helmets and ladder protocols are mandatory under the **Permit-To-Work (PTW)** system, visual verification of site photographs has historically suffered from manual inspection fatigue.

**SurakshaKavach** solves this by shifting safety intelligence directly to the electrical lineman's smartphone. The app runs an on-device, quantized **YOLOv8n** Computer Vision model through **TensorFlow Lite**, performing 100% offline real-time PPE detection with audible buzzer alarms and zero cloud dependencies.

---

## 🚀 Key Technical Features

- **Edge AI Real-Time Inference**: Processes live video feeds via Android **CameraX** with an INT8-quantized model executing on local CPU/NNAPI delegates.
- **Ultra-Compact Footprint**: Model size compressed from 47 MB (FP32) down to **3.32 MB (INT8)**, allowing seamless installation on low-spec field smartphones.
- **Safety-First IoU Resolution**: Spatial conflict resolution algorithm prioritizing helmet compliance whenever linemen are detected on elevated ladders.
- **Battery & Thermal Throttling Control**: Gated 3-FPS inference loop to ensure the phone does not overheat or drain battery during lengthy field inspections.
- **100% Offline Capability**: Linemen in remote rural substations with zero cellular connectivity receive instant safety feedback.

---

## 📊 Iterative Development History

| Release | Model Architecture | Target Classes | Size | Description |
| :--- | :--- | :--- | :--- | :--- |
| **v1.0.0** | YOLOv8s-seg | `with_helmet`, `without_helmet`, `ladder`, `worker` (4) | 98.4 MB (APK) | Initial segmentation prototype. Observed bounding-box overlap conflicts between worker and helmet classes. |
| **v2.0.0** | YOLOv8n Detection | `with_helmet`, `without_helmet`, `ladder` (3) | 93.7 MB (APK) | Removed worker class to eliminate overlap. Transitioned from segmentation to pure detection, cutting inference latency. |
| **v3.0.0** ⭐ | YOLOv8n (Field-Tuned) | `with_helmet`, `without_helmet`, `ladder` (3) | 97.0 MB (APK) | **Final Production Build**. Retrained with TPCODL field substation photographs; added CameraX thermal pacing. |

---

## 📥 Download App Releases

All official builds are hosted on [GitHub Releases](https://github.com/Rohanbhoff/SurakshaKavach/releases):

- 📱 [**Download SurakshaKavach v3.0.0 (Recommended Production Build)**](https://github.com/Rohanbhoff/SurakshaKavach/releases/download/v3.0.0/Suraksha_Kavach_v3.apk)
- 📦 [**Browse All Releases & Previous Builds (v2.0.0-beta, v1.0.0-alpha)**](https://github.com/Rohanbhoff/SurakshaKavach/releases)

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **Camera Pipeline**: Android Jetpack CameraX (ImageAnalysis & Preview)
- **ML Runtime**: TensorFlow Lite Android Support Library (`org.tensorflow:tensorflow-lite`)
- **Neural Model**: Ultralytics YOLOv8n (INT8 Post-Training Quantization)
- **UI Framework**: Custom dark-mode Material UI with live detection overlay canvas (`OverlayView.kt`)
