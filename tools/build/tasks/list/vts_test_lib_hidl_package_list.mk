#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

vts_hal_driver_libs := \
  android.hardware.audio@2.0-vts.driver \
  android.hardware.audio.common@2.0-vts.driver \
  android.hardware.audio.effect@2.0-vts.driver \
  android.hardware.authsecret@1.0-vts.driver \
  android.hardware.automotive.audiocontrol@1.0-vts.driver \
  android.hardware.automotive.evs@1.0-vts.driver \
  android.hardware.automotive.vehicle@2.0-vts.driver \
  android.hardware.biometrics.fingerprint@2.1-vts.driver \
  android.hardware.bluetooth@1.0-vts.driver \
  android.hardware.boot@1.0-vts.driver \
  android.hardware.broadcastradio@1.0-vts.driver \
  android.hardware.broadcastradio@1.1-vts.driver \
  android.hardware.broadcastradio@2.0-vts.driver \
  android.hardware.camera.common@1.0-vts.driver \
  android.hardware.camera.device@1.0-vts.driver \
  android.hardware.camera.device@3.2-vts.driver \
  android.hardware.camera.device@3.3-vts.driver \
  android.hardware.camera.device@3.4-vts.driver \
  android.hardware.camera.metadata@3.2-vts.driver \
  android.hardware.camera.metadata@3.3-vts.driver \
  android.hardware.camera.provider@2.4-vts.driver \
  android.hardware.cas@1.0-vts.driver \
  android.hardware.cas.native@1.0-vts.driver \
  android.hardware.configstore@1.0-vts.driver \
  android.hardware.confirmationui@1.0-vts.driver \
  android.hardware.contexthub@1.0-vts.driver \
  android.hardware.drm@1.0-vts.driver \
  android.hardware.drm@1.1-vts.driver \
  android.hardware.dumpstate@1.0-vts.driver \
  android.hardware.gatekeeper@1.0-vts.driver \
  android.hardware.gnss@1.0-vts.driver \
  android.hardware.gnss@1.1-vts.driver \
  android.hardware.graphics.allocator@2.0-vts.driver \
  android.hardware.graphics.bufferqueue@1.0-vts.driver \
  android.hardware.graphics.common@1.0-vts.driver \
  android.hardware.graphics.common@1.1-vts.driver \
  android.hardware.graphics.composer@2.1-vts.driver \
  android.hardware.graphics.composer@2.2-vts.driver \
  android.hardware.graphics.mapper@2.0-vts.driver \
  android.hardware.graphics.mapper@2.1-vts.driver \
  android.hardware.health@1.0-vts.driver \
  android.hardware.health@2.0-vts.driver \
  android.hardware.ir@1.0-vts.driver \
  android.hardware.keymaster@3.0-vts.driver \
  android.hardware.keymaster@4.0-vts.driver \
  android.hardware.light@2.0-vts.driver \
  android.hardware.media@1.0-vts.driver \
  android.hardware.media.omx@1.0-vts.driver \
  android.hardware.media.bufferpool@1.0-vts.driver \
  android.hardware.memtrack@1.0-vts.driver \
  android.hardware.neuralnetworks@1.0-vts.driver \
  android.hardware.neuralnetworks@1.1-vts.driver \
  android.hardware.nfc@1.0-vts.driver \
  android.hardware.nfc@1.1-vts.driver \
  android.hardware.oemlock@1.0-vts.driver \
  android.hardware.power@1.0-vts.driver \
  android.hardware.power@1.1-vts.driver \
  android.hardware.power@1.2-vts.driver \
  android.hardware.power@1.3-vts.driver \
  android.hardware.radio@1.0-vts.driver \
  android.hardware.radio@1.1-vts.driver \
  android.hardware.radio@1.2-vts.driver \
  android.hardware.radio.config@1.0-vts.driver \
  android.hardware.radio.deprecated@1.0-vts.driver \
  android.hardware.renderscript@1.0-vts.driver \
  android.hardware.secure_element@1.0-vts.driver \
  android.hardware.sensors@1.0-vts.driver \
  android.hardware.soundtrigger@2.0-vts.driver \
  android.hardware.soundtrigger@2.1-vts.driver \
  android.hardware.tests.memory@1.0-vts.driver \
  android.hardware.tests.msgq@1.0-vts.driver \
  android.hardware.tetheroffload.config@1.0-vts.driver \
  android.hardware.tetheroffload.control@1.0-vts.driver \
  android.hardware.thermal@1.0-vts.driver \
  android.hardware.thermal@1.1-vts.driver \
  android.hardware.tv.cec@1.0-vts.driver \
  android.hardware.tv.input@1.0-vts.driver \
  android.hardware.usb@1.0-vts.driver \
  android.hardware.usb@1.1-vts.driver \
  android.hardware.usb.gadget@1.0-vts.driver \
  android.hardware.vibrator@1.0-vts.driver \
  android.hardware.vibrator@1.1-vts.driver \
  android.hardware.vibrator@1.2-vts.driver \
  android.hardware.vr@1.0-vts.driver \
  android.hardware.weaver@1.0-vts.driver \
  android.hardware.wifi@1.0-vts.driver \
  android.hardware.wifi@1.1-vts.driver \
  android.hardware.wifi@1.2-vts.driver \
  android.hardware.wifi.hostapd@1.0-vts.driver \
  android.hardware.wifi.offload@1.0-vts.driver \
  android.hardware.wifi.supplicant@1.0-vts.driver \
  android.hardware.wifi.supplicant@1.1-vts.driver \
  android.hidl.memory.block@1.0-vts.driver \
  android.hidl.memory.token@1.0-vts.driver \
  android.system.net.netd@1.0-vts.driver \
  android.system.net.netd@1.1-vts.driver \

vts_hal_profiler_libs := \
  android.hardware.audio@2.0-vts.profiler \
  android.hardware.audio.common@2.0-vts.profiler \
  android.hardware.audio.effect@2.0-vts.profiler \
  android.hardware.authsecret@1.0-vts.profiler \
  android.hardware.automotive.audiocontrol@1.0-vts.profiler \
  android.hardware.automotive.evs@1.0-vts.profiler \
  android.hardware.automotive.vehicle@2.0-vts.profiler \
  android.hardware.biometrics.fingerprint@2.1-vts.profiler \
  android.hardware.bluetooth@1.0-vts.profiler \
  android.hardware.boot@1.0-vts.profiler \
  android.hardware.broadcastradio@1.0-vts.profiler \
  android.hardware.broadcastradio@1.1-vts.profiler \
  android.hardware.broadcastradio@2.0-vts.profiler \
  android.hardware.camera.common@1.0-vts.profiler \
  android.hardware.camera.device@1.0-vts.profiler \
  android.hardware.camera.device@3.2-vts.profiler \
  android.hardware.camera.device@3.3-vts.profiler \
  android.hardware.camera.device@3.4-vts.profiler \
  android.hardware.camera.metadata@3.2-vts.profiler \
  android.hardware.camera.metadata@3.3-vts.profiler \
  android.hardware.camera.provider@2.4-vts.profiler \
  android.hardware.cas@1.0-vts.profiler \
  android.hardware.cas.native@1.0-vts.profiler \
  android.hardware.configstore@1.0-vts.profiler \
  android.hardware.confirmationui@1.0-vts.profiler \
  android.hardware.contexthub@1.0-vts.profiler \
  android.hardware.drm@1.0-vts.profiler \
  android.hardware.drm@1.1-vts.profiler \
  android.hardware.dumpstate@1.0-vts.profiler \
  android.hardware.gatekeeper@1.0-vts.profiler \
  android.hardware.gnss@1.0-vts.profiler \
  android.hardware.gnss@1.1-vts.profiler \
  android.hardware.graphics.allocator@2.0-vts.profiler \
  android.hardware.graphics.bufferqueue@1.0-vts.profiler \
  android.hardware.graphics.common@1.0-vts.profiler \
  android.hardware.graphics.common@1.1-vts.profiler \
  android.hardware.graphics.composer@2.1-vts.profiler \
  android.hardware.graphics.composer@2.2-vts.profiler \
  android.hardware.graphics.mapper@2.0-vts.profiler \
  android.hardware.graphics.mapper@2.1-vts.profiler \
  android.hardware.health@1.0-vts.profiler \
  android.hardware.health@2.0-vts.profiler \
  android.hardware.ir@1.0-vts.profiler \
  android.hardware.keymaster@3.0-vts.profiler \
  android.hardware.keymaster@4.0-vts.profiler \
  android.hardware.light@2.0-vts.profiler \
  android.hardware.media@1.0-vts.profiler \
  android.hardware.media.omx@1.0-vts.profiler \
  android.hardware.media.bufferpool@1.0-vts.profiler \
  android.hardware.memtrack@1.0-vts.profiler \
  android.hardware.neuralnetworks@1.0-vts.profiler \
  android.hardware.neuralnetworks@1.1-vts.profiler \
  android.hardware.nfc@1.0-vts.profiler \
  android.hardware.nfc@1.1-vts.profiler \
  android.hardware.oemlock@1.0-vts.profiler \
  android.hardware.power@1.0-vts.profiler \
  android.hardware.power@1.1-vts.profiler \
  android.hardware.power@1.2-vts.profiler \
  android.hardware.power@1.3-vts.profiler \
  android.hardware.radio@1.0-vts.profiler \
  android.hardware.radio@1.1-vts.profiler \
  android.hardware.radio@1.2-vts.profiler \
  android.hardware.radio.config@1.0-vts.profiler \
  android.hardware.radio.deprecated@1.0-vts.profiler \
  android.hardware.renderscript@1.0-vts.profiler \
  android.hardware.secure_element@1.0-vts.profiler \
  android.hardware.sensors@1.0-vts.profiler \
  android.hardware.soundtrigger@2.0-vts.profiler \
  android.hardware.soundtrigger@2.1-vts.profiler \
  android.hardware.tests.memory@1.0-vts.profiler \
  android.hardware.tests.msgq@1.0-vts.profiler \
  android.hardware.tetheroffload.config@1.0-vts.profiler \
  android.hardware.tetheroffload.control@1.0-vts.profiler \
  android.hardware.thermal@1.0-vts.profiler \
  android.hardware.thermal@1.1-vts.profiler \
  android.hardware.tv.cec@1.0-vts.profiler \
  android.hardware.tv.input@1.0-vts.profiler \
  android.hardware.usb@1.0-vts.profiler \
  android.hardware.usb@1.1-vts.profiler \
  android.hardware.usb.gadget@1.0-vts.profiler \
  android.hardware.vibrator@1.0-vts.profiler \
  android.hardware.vibrator@1.1-vts.profiler \
  android.hardware.vibrator@1.2-vts.profiler \
  android.hardware.vr@1.0-vts.profiler \
  android.hardware.weaver@1.0-vts.profiler \
  android.hardware.wifi@1.0-vts.profiler \
  android.hardware.wifi@1.1-vts.profiler \
  android.hardware.wifi@1.2-vts.profiler \
  android.hardware.wifi.hostapd@1.0-vts.profiler \
  android.hardware.wifi.offload@1.0-vts.profiler \
  android.hardware.wifi.supplicant@1.0-vts.profiler \
  android.hardware.wifi.supplicant@1.1-vts.profiler \
  android.hidl.memory.block@1.0-vts.profiler \
  android.hidl.memory.token@1.0-vts.profiler \
  android.system.net.netd@1.0-vts.profiler \
  android.system.net.netd@1.1-vts.profiler \

vts_hal_test_bins := \
  VtsHalAudioV2_0TargetTest \
  VtsHalAudioV4_0TargetTest \
  VtsHalAudioEffectV2_0TargetTest \
  VtsHalAudioEffectV4_0TargetTest \
  VtsHalAuthSecretV1_0TargetTest \
  VtsHalBiometricsFingerprintV2_1TargetTest \
  VtsHalBluetoothV1_0TargetTest \
  VtsHalBootV1_0TargetTest \
  VtsHalBroadcastradioV1_0TargetTest \
  VtsHalBroadcastradioV1_1TargetTest \
  VtsHalBroadcastradioV2_0TargetTest \
  VtsHalCameraProviderV2_4TargetTest \
  VtsHalCasV1_0TargetTest \
  VtsHalConfigstoreV1_0TargetTest \
  VtsHalContexthubV1_0TargetTest \
  VtsHalDrmV1_0TargetTest \
  VtsHalDrmV1_1TargetTest \
  VtsHalDumpstateV1_0TargetTest \
  VtsHalEvsV1_0TargetTest \
  VtsHalGatekeeperV1_0TargetTest \
  VtsHalGnssV1_0TargetTest \
  VtsHalGnssV1_1TargetTest \
  VtsHalGraphicsComposerV2_1TargetTest \
  VtsHalGraphicsComposerV2_2TargetTest \
  VtsHalGraphicsMapperV2_0TargetTest \
  VtsHalGraphicsMapperV2_1TargetTest \
  VtsHalHealthV1_0TargetTest \
  VtsHalHealthV2_0TargetTest \
  VtsHalIrV1_0TargetTest \
  VtsHalKeymasterV3_0TargetTest \
  VtsHalKeymasterV4_0TargetTest \
  VtsHalLightV2_0TargetTest \
  VtsHalMediaOmxV1_0TargetComponentTest \
  VtsHalMediaOmxV1_0TargetAudioEncTest \
  VtsHalMediaOmxV1_0TargetAudioDecTest \
  VtsHalMediaOmxV1_0TargetVideoEncTest \
  VtsHalMediaOmxV1_0TargetVideoDecTest \
  VtsHalMemtrackV1_0TargetTest \
  VtsHalNetNetdV1_0TargetTest \
  VtsHalNetNetdV1_1TargetTest \
  VtsHalNeuralnetworksV1_0TargetTest \
  VtsHalNeuralnetworksV1_1TargetTest \
  VtsHalNfcV1_0TargetTest \
  VtsHalNfcV1_1TargetTest \
  VtsHalOemLockV1_0TargetTest \
  VtsHalPowerV1_0TargetTest \
  VtsHalPowerV1_1TargetTest \
  VtsHalPowerV1_2TargetTest \
  VtsHalPowerV1_3TargetTest \
  VtsHalRadioV1_0TargetTest \
  VtsHalRadioV1_1TargetTest \
  VtsHalRadioV1_2TargetTest \
  VtsHalRenderscriptV1_0TargetTest \
  VtsHalSapV1_0TargetTest \
  VtsHalSecureElementV1_0TargetTest \
  VtsHalSensorsV1_0TargetTest \
  VtsHalSoundtriggerV2_0TargetTest \
  VtsHalSoundtriggerV2_1TargetTest \
  VtsHalTetheroffloadConfigV1_0TargetTest \
  VtsHalTetheroffloadControlV1_0TargetTest \
  VtsHalThermalV1_0TargetTest \
  VtsHalThermalV1_1TargetTest \
  thermal_hidl_stress_test \
  VtsHalTvInputV1_0TargetTest \
  VtsHalUsbV1_0TargetTest \
  VtsHalUsbV1_1TargetTest \
  VtsHalVibratorV1_0TargetTest \
  VtsHalVibratorV1_1TargetTest \
  VtsHalVibratorV1_2TargetTest \
  VtsHalVrV1_0TargetTest \
  VtsHalWeaverV1_0TargetTest \
  VtsHalWifiV1_0TargetTest \
  VtsHalWifiV1_1TargetTest \
  VtsHalWifiV1_2TargetTest \
  VtsHalWifiNanV1_0TargetTest \
  VtsHalWifiNanV1_2TargetTest \
  VtsHalWifiOffloadV1_0TargetTest \
  VtsHalWifiSupplicantV1_0TargetTest \
  VtsHalWifiSupplicantV1_1TargetTest \
  VtsHidlAllocatorV1_0TargetTest \

# Binaries which are part of VNDK but in the form of HIDL.
vts_hal_test_bins += \
  VtsVndkHidlBufferpoolV1_0TargetSingleTest \
  VtsVndkHidlBufferpoolV1_0TargetMultiTest \

vts_test_lib_hidl_packages := \
  $(vts_hal_driver_libs) \
  $(vts_hal_profiler_libs) \
  $(vts_hal_test_bins) \
  libhwbinder \
  libhidlbase \
  libhidltransport \
  libvtswidevine_prebuilt \
