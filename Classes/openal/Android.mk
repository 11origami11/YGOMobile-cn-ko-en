LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := openal

LOCAL_C_INCLUDES := $(LOCAL_PATH)/android				\
					$(LOCAL_PATH)/include				\
					$(LOCAL_PATH)/src/Alc				\
					$(LOCAL_PATH)/src/common			\
					$(LOCAL_PATH)/src/OpenAL32			\
					$(LOCAL_PATH)/src/OpenAL32/Include

LOCAL_CFLAGS     := -DAL_BUILD_LIBRARY -DAL_ALEXT_PROTOTYPES
LOCAL_CFLAGS_X64 := -DSIZEOF_LONG=8 -DHAVE_POSIX_MEMALIGN -DHAVE_STRTOF -DHAVE_PTHREAD_MUTEX_TIMEDLOCK
LOCAL_CFLAGS_SSE := -DHAVE_SSE -DHAVE_SSE2 -DHAVE_SSE3 -DHAVE_SSE4_1 -DHAVE_CPUID_H -DHAVE_GCC_GET_CPUID

DECODER_SRC_ARM  := src/Alc/mixer/mixer_neon.c
DECODER_SRC_SSE  := src/Alc/mixer/mixer_sse.c src/Alc/mixer/mixer_sse2.c src/Alc/mixer/mixer_sse3.c src/Alc/mixer/mixer_sse41.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_ARM_NEON   := true
LOCAL_CFLAGS     += -DHAVE_NEON
DECODER_SRC      := $(DECODER_SRC_ARM)
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_ARM_NEON   := true
LOCAL_CFLAGS     += $(LOCAL_CFLAGS_X64) -DHAVE_NEON
DECODER_SRC      := $(DECODER_SRC_ARM)
endif

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS     += $(LOCAL_CFLAGS_SSE) 
DECODER_SRC      := $(DECODER_SRC_SSE)
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_CFLAGS     += $(LOCAL_CFLAGS_SSE) $(LOCAL_CFLAGS_X64)
DECODER_SRC      := $(DECODER_SRC_SSE)
endif

LOCAL_SRC_FILES  := \
    src/Alc/ALc.c \
    src/Alc/ALu.c \
    src/Alc/alconfig.c \
    src/Alc/bs2b.c \
    src/Alc/converter.c \
    src/Alc/mastering.c \
    src/Alc/ringbuffer.c \
    src/Alc/helpers.c \
    src/Alc/hrtf.c \
    src/Alc/uhjfilter.c \
    src/Alc/ambdec.c \
    src/Alc/bformatdec.c \
    src/Alc/panning.c \
    src/Alc/mixvoice.c \
    \
    src/Alc/effects/autowah.c \
    src/Alc/effects/chorus.c \
    src/Alc/effects/compressor.c \
    src/Alc/effects/dedicated.c \
    src/Alc/effects/distortion.c \
    src/Alc/effects/echo.c \
    src/Alc/effects/equalizer.c \
    src/Alc/effects/fshifter.c \
    src/Alc/effects/modulator.c \
    src/Alc/effects/null.c \
    src/Alc/effects/pshifter.c \
    src/Alc/effects/reverb.c \
    \
    src/Alc/backends/base.c \
    src/Alc/backends/loopback.c \
    src/Alc/backends/null.c \
    src/Alc/backends/opensl.c \
    src/Alc/backends/wave.c \
    \
    src/Alc/filters/filter.c \
    src/Alc/filters/nfc.c \
    src/Alc/filters/splitter.c \
    \
    src/OpenAL32/alAuxEffectSlot.c \
    src/OpenAL32/alBuffer.c \
    src/OpenAL32/alEffect.c \
    src/OpenAL32/alError.c \
    src/OpenAL32/alExtension.c \
    src/OpenAL32/alFilter.c \
    src/OpenAL32/alListener.c \
    src/OpenAL32/alSource.c \
    src/OpenAL32/alState.c \
    src/OpenAL32/event.c \
    src/OpenAL32/sample_cvt.c \
    \
    src/common/alcomplex.c \
    src/common/almalloc.c \
    src/common/atomic.c \
    src/common/rwlock.c \
    src/common/threads.c \
    src/common/uintmap.c \
    \
    src/Alc/mixer/mixer_c.c \
    $(DECODER_SRC)

include $(BUILD_STATIC_LIBRARY)
