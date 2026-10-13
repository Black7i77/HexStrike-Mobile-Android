package com.scarface.hexstrikemobile;

/** JNI bridge to llama.cpp; everything runs locally on Android. */
public final class ModelBridge {
    static { System.loadLibrary("hexstrike_llm"); }
    public native boolean load(String path);
    public native String generate(String question, int maxTokens);
    public native void unload();
}
