#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include "llama.h"

static void llama_batch_add(llama_batch &batch, llama_token token, llama_pos pos, std::initializer_list<llama_seq_id> ids, bool logits) {
    int idx = batch.n_tokens++;
    batch.token[idx] = token;
    batch.pos[idx] = pos;
    batch.n_seq_id[idx] = (int)ids.size();
    int j=0; for (auto id : ids) batch.seq_id[idx][j++] = id;
    batch.logits[idx] = logits ? 1 : 0;
}
static void llama_batch_clear(llama_batch &batch) { batch.n_tokens = 0; }

static std::mutex guard;
static llama_model * model = nullptr;
static bool backend_ready = false;
static std::string last_error;

static std::string utf8(JNIEnv *env, jstring value) {
    if (!value) return {};
    const char * s = env->GetStringUTFChars(value, nullptr);
    if (!s) return {};
    std::string result(s);
    env->ReleaseStringUTFChars(value, s);
    return result;
}
static jstring jstr(JNIEnv *env, const std::string &s) {
    return env->NewStringUTF(s.c_str());
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_scarface_hexstrikemobile_ModelBridge_load(JNIEnv *env, jobject, jstring file) {
    std::lock_guard<std::mutex> lock(guard);
    if (model) { llama_model_free(model); model = nullptr; }
    if (!backend_ready) { llama_backend_init(); backend_ready = true; }
    auto path = utf8(env, file);
    auto params = llama_model_default_params();
    params.n_gpu_layers = 0; // CPU-only for widest compatibility
    model = llama_model_load_from_file(path.c_str(), params);
    return model ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_scarface_hexstrikemobile_ModelBridge_unload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(guard);
    if (model) { llama_model_free(model); model = nullptr; }
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_scarface_hexstrikemobile_ModelBridge_generate(JNIEnv *env, jobject, jstring question, jint max_tokens) {
    __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                        "Generation requested");

    std::lock_guard<std::mutex> lock(guard);
    if (!model) return jstr(env, "ERROR: Import and load a GGUF model first.");
    const llama_vocab * vocab = llama_model_get_vocab(model);
    const std::string query = utf8(env, question);
    // Qwen2.5 instruct template. Select a compatible Qwen2.5-Instruct GGUF.
    const std::string input = "<|im_start|>system\nYou are HexStrike Mobile, an offline assistant. Explain clearly. You do not scan or execute commands.\n<|im_end|>\n<|im_start|>user\n" + query + "\n<|im_end|>\n<|im_start|>assistant\n";
    int n = -llama_tokenize(vocab, input.c_str(), (int)input.size(), nullptr, 0, true, true);
    if (n <= 0 || n > 3400) return jstr(env, "ERROR: Prompt too long or tokenization failed.");
    std::vector<llama_token> tokens(n);
    int actual = llama_tokenize(vocab, input.c_str(), (int)input.size(), tokens.data(), n, true, true);
    if (actual <= 0) return jstr(env, "ERROR: Could not tokenize input.");
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 1024;
    cp.n_batch = 256;
    cp.n_threads = 4;
    __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                         "Creating inference context");
    llama_context * ctx = llama_init_from_model(model, cp);
    __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                         "Context created: %s", ctx ? "YES" : "NO");
    if (!ctx) return jstr(env, "ERROR: Could not allocate model context. Try a smaller GGUF.");
    llama_sampler * sampler = llama_sampler_init_greedy();
    llama_batch batch = llama_batch_init(256, 0, 1);
    bool ok = true;
    int processed = 0;
    while (processed < actual && ok) {
        int count = std::min(256, actual - processed);
        llama_batch_clear(batch);
        for (int i=0; i<count; ++i) {
            llama_batch_add(batch, tokens[processed+i], processed+i, {0}, (processed+i==actual-1));
        }
        ok = llama_decode(ctx, batch) == 0;
        processed += count;
    }
    __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                         "Prompt processing finished: %s",
                         ok ? "YES" : "NO");
    std::string response;
    int limit = std::min(96, std::max(1, (int)max_tokens));
    for (int i=0; i<limit && ok; ++i) {
        if (i % 10 == 0)
            __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                                "Generating token %d", i);
        llama_token next = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;
        char piece[512];
        int len = llama_token_to_piece(vocab, next, piece, sizeof(piece), 0, true);
        if (len > 0 && len < (int)sizeof(piece)) response.append(piece, len);
        llama_batch_clear(batch);
        llama_batch_add(batch, next, actual+i, {0}, true);
        ok = llama_decode(ctx, batch) == 0;
    }
    __android_log_print(ANDROID_LOG_INFO, "HexStrikeAI",
                         "Generation loop finished");
    llama_batch_free(batch);
    llama_sampler_free(sampler);
    llama_free(ctx);
    if (!ok) return jstr(env, "ERROR: Inference failed or ran out of context.");
    return jstr(env, response.empty() ? "(No response generated)" : response);
}
