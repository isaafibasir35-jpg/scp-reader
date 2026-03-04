package com.example.scpreader;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GoogleTranslateTTSClient {
    private static final String TAG = "GoogleTTSClient";
    private static final int MAX_CHUNK_LENGTH = 150;
    private static final String TTS_URL_BASE = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ru&client=tw-ob&q=";

    private MediaPlayer mediaPlayer;
    private List<String> chunks = new ArrayList<>();
    private int currentChunkIndex = -1;
    private boolean isPlaying = false;
    private OnPlaybackEventListener playbackListener;

    public interface OnPlaybackEventListener {
        void onPlaybackStarted();
        void onPlaybackStopped();
        void onPlaybackError(String message);
    }

    public GoogleTranslateTTSClient(OnPlaybackEventListener listener) {
        this.playbackListener = listener;
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());

        mediaPlayer.setOnCompletionListener(mp -> {
            playNextChunk();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
            stop();
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Ошибка воспроизведения: " + what);
            }
            return true;
        });
    }

    public void play(String text) {
        stop();
        chunks = splitText(text);
        if (chunks.isEmpty()) {
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Нет текста для озвучки");
            }
            return;
        }
        currentChunkIndex = 0;
        isPlaying = true;
        if (playbackListener != null) {
            playbackListener.onPlaybackStarted();
        }
        playChunk(chunks.get(currentChunkIndex));
    }

    public void stop() {
        isPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
        chunks.clear();
        currentChunkIndex = -1;
        if (playbackListener != null) {
            playbackListener.onPlaybackStopped();
        }
    }

    private void playNextChunk() {
        if (!isPlaying) return;

        currentChunkIndex++;
        if (currentChunkIndex < chunks.size()) {
            playChunk(chunks.get(currentChunkIndex));
        } else {
            stop();
        }
    }

    private void playChunk(String chunk) {
        try {
            String url = TTS_URL_BASE + URLEncoder.encode(chunk, "UTF-8");
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (isPlaying) {
                    mp.start();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error playing chunk", e);
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Ошибка при подготовке аудио");
            }
            stop();
        }
    }

    private List<String> splitText(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        // Очистка от лишних пробелов
        text = text.replaceAll("\\s+", " ").trim();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());
            
            if (end < text.length()) {
                // Ищем место для разрыва (точку, восклицательный знак, вопросительный знак или пробел)
                int lastPunctuation = -1;
                char[] punctuationMarks = {'.', '!', '?', ';', ',', ':'};
                
                for (char mark : punctuationMarks) {
                    int pos = text.lastIndexOf(mark, end);
                    if (pos > start && pos > lastPunctuation) {
                        lastPunctuation = pos;
                    }
                }

                if (lastPunctuation != -1) {
                    end = lastPunctuation + 1;
                } else {
                    // Если знаков препинания не нашли, пробуем по пробелу
                    int lastSpace = text.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace;
                    }
                }
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
            start = end;
        }

        return result;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
