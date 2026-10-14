package com.scarface.hexstrikemobile;

/** JNI bridge to llama.cpp; everything runs locally on Android. */
public final class ModelBridge {
    static { System.loadLibrary("hexstrike_llm"); }
    public native boolean load(String path);
    public native String generateStream(String question, int maxTokens, int threadCount);
    private StreamListener listener;
    public interface StreamListener { void onUpdate(String text); }
    public void setStreamListener(StreamListener listener) { this.listener = listener; }
    /** Invoked by native generation on the worker thread. */
    @SuppressWarnings("unused") private void onStreamUpdate(String text) {
        StreamListener current = listener;
        if (current != null) current.onUpdate(text);
    }
    public native void unload();
}
