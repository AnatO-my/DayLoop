#include <jni.h>

#include <stdint.h>
#include <stdlib.h>

uint32_t dayloop_ffi_apply_task_transition(
    const uint8_t* input,
    size_t input_length,
    uint8_t* output,
    size_t output_capacity
);

#define RESPONSE_BYTES 13
#define STATUS_BYTES 4

static void write_le_status(uint8_t* destination, uint32_t status) {
    destination[0] = (uint8_t)status;
    destination[1] = (uint8_t)(status >> 8);
    destination[2] = (uint8_t)(status >> 16);
    destination[3] = (uint8_t)(status >> 24);
}

JNIEXPORT jbyteArray JNICALL Java_com_dayloop_app_rust_RustFfiBridge_applyTaskTransition(
    JNIEnv* env,
    jobject receiver,
    jbyteArray input
) {
    (void)receiver;
    if (input == NULL) {
        return NULL;
    }

    const jsize length = (*env)->GetArrayLength(env, input);
    uint8_t empty_request = 0;
    uint8_t* request = length == 0 ? &empty_request : malloc((size_t)length);
    if (request == NULL) {
        return NULL;
    }
    if (length > 0) {
        (*env)->GetByteArrayRegion(env, input, 0, length, (jbyte*)request);
        if ((*env)->ExceptionCheck(env)) {
            free(request);
            return NULL;
        }
    }

    uint8_t response[RESPONSE_BYTES] = {0};
    const uint32_t status = dayloop_ffi_apply_task_transition(
        request,
        (size_t)length,
        response,
        RESPONSE_BYTES
    );
    if (length > 0) {
        free(request);
    }

    jbyteArray result = (*env)->NewByteArray(env, STATUS_BYTES + RESPONSE_BYTES);
    if (result == NULL) {
        return NULL;
    }
    uint8_t payload[STATUS_BYTES + RESPONSE_BYTES] = {0};
    write_le_status(payload, status);
    if (status == 0) {
        for (size_t index = 0; index < RESPONSE_BYTES; index++) {
            payload[STATUS_BYTES + index] = response[index];
        }
    }
    (*env)->SetByteArrayRegion(
        env,
        result,
        0,
        STATUS_BYTES + RESPONSE_BYTES,
        (const jbyte*)payload
    );
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
