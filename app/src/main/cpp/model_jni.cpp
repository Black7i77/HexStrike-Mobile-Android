#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include <chrono>
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
Java_com_scarface_hexstrikemobile_ModelBridge_generateStream(JNIEnv *env, jobject bridge, jstring question, jint max_tokens, jint thread_count) {
    std::lock_guard<std::mutex> lock(guard);
    if (!model) return jstr(env, "ERROR: Import and load a GGUF model first.");
    const llama_vocab * vocab = llama_model_get_vocab(model);
    const std::string query = utf8(env, question);
    // Qwen2.5 instruct template. Select a compatible Qwen2.5-Instruct GGUF.
    const std::string input = "<|im_start|>system\nYou are HexStrike Mobile AI, powered by WolfCore. Be friendly, professional and accurate. Answer in clear, natural English. Keep answers concise unless more detail is requested. Use occasional relevant emojis such as 🐺 🐧 💻 🛡️ 📱. Do not overuse emojis. Support other languages when requested. If you are uncertain, say so rather than inventing facts. WormGPT refers to an AI chatbot marketed for cybercrime, not an email-spreading computer worm. GPT stands for Generative Pre-trained Transformer. You do not scan devices or execute commands. Focus on ethical defensive cybersecurity.\n<|im_end|>\n<|im_start|>user\n" + query + "\n<|im_end|>\n<|im_start|>assistant\n";
    int n = -llama_tokenize(vocab, input.c_str(), (int)input.size(), nullptr, 0, true, true);
    if (n <= 0 || n > 1536) return jstr(env, "ERROR: Prompt too long or tokenization failed.");
    std::vector<llama_token> tokens(n);
    int actual = llama_tokenize(vocab, input.c_str(), (int)input.size(), tokens.data(), n, true, true);
    if (actual <= 0) return jstr(env, "ERROR: Could not tokenize input.");
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 256;
    cp.n_threads = std::max(1, std::min(8, (int)thread_count));
    cp.n_threads_batch = cp.n_threads;
    llama_context * ctx = llama_init_from_model(model, cp);
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
    std::string response;
    jclass bridgeClass = env->GetObjectClass(bridge);
    jmethodID streamMethod = env->GetMethodID(bridgeClass, "onStreamUpdate", "(Ljava/lang/String;)V");
    int lastReportedLength = 0;
    int limit = std::min(512, std::max(1, (int)max_tokens));
    for (int i=0; i<limit && ok; ++i) {
        llama_token next = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;
        char piece[512];
        int len = llama_token_to_piece(vocab, next, piece, sizeof(piece), 0, true);
        if (len > 0 && len < (int)sizeof(piece)) response.append(piece, len);
        // Update the existing console every ~48 bytes, not per-token.
        // Full-text updates avoid splitting UTF-8 code points at JNI boundaries.
        if (streamMethod && (int)response.size() - lastReportedLength >= 48) {
            jstring update = jstr(env, response);
            env->CallVoidMethod(bridge, streamMethod, update);
            env->DeleteLocalRef(update);
            if (env->ExceptionCheck()) { env->ExceptionClear(); streamMethod = nullptr; }
            lastReportedLength = (int)response.size();
        }
        llama_batch_clear(batch);
        llama_batch_add(batch, next, actual+i, {0}, true);
        ok = llama_decode(ctx, batch) == 0;
    }
    if (streamMethod && !response.empty()) {
        jstring update = jstr(env, response);
        env->CallVoidMethod(bridge, streamMethod, update);
        env->DeleteLocalRef(update);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    env->DeleteLocalRef(bridgeClass);
    llama_batch_free(batch);
    llama_sampler_free(sampler);
    llama_free(ctx);
    if (!ok) return jstr(env, "ERROR: Inference failed or ran out of context.");
    return jstr(env, response.empty() ? "(No response generated)" : response);
}
