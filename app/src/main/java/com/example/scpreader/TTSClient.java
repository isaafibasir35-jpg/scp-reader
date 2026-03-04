package com.example.scpreader;

public interface TTSClient {
    void play(String text);
    void stop();
    void pause();
    void resume();
    boolean isPlaying();
    void seekTo(int position);
    int getDuration();
    int getCurrentPosition();
    void setSpeed(float speed);
    void release();
    
    interface OnPlaybackEventListener {
        void onPlaybackStarted();
        void onPlaybackStopped();
        void onPlaybackError(String message);
        void onPlaybackStateChanged(boolean isPlaying);
    }
}
