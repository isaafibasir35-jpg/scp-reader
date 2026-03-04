package com.example.scpreader;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

public class EdgeLibraryTTSClient implements TTSClient {
    private static final String TAG = "EdgeTTSClient";
    private static final String VOICE_NAME = "ru-RU-SvetlanaNeural";

    private Context context;
    private MediaPlayer mediaPlayer;
    private OnPlaybackEventListener playbackListener;
    private boolean isPlaying = false;
    private float currentSpeed = 1.0f;
    private File cacheFile;

    public EdgeLibraryTTSClient(Context context, OnPlaybackEventListener listener) {
        this.context = context;
        this.playbackListener = listener;
        this.mediaPlayer = createMediaPlayer();
        this.cacheFile = new File(context.getCacheDir(), "edge_tts_cache.mp3");
    }

    private MediaPlayer createMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());

        mp.setOnCompletionListener(mp1 -> {
            isPlaying = false;
            if (playbackListener != null) {
                playbackListener.onPlaybackStopped();
            }
        });

        mp.setOnErrorListener((mp1, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what);
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Ошибка плеера: " + what);
            }
            return true;
        });

        return mp;
    }

    @Override
    public void play(String text) {
        stop();
        if (text == null || text.isEmpty()) {
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Нет текста");
            }
            return;
        }

        new DownloadTask(text).execute();
    }

    private class DownloadTask extends AsyncTask<Void, Void, Boolean> {
        private String text;

        DownloadTask(String text) {
            this.text = text;
        }

        @Override
        protected void onPreExecute() {
            if (playbackListener != null) {
                playbackListener.onPlaybackStarted(); // Условно считаем началом подготовки
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }

                Voice voice = TTSVoice.provides().stream()
                        .filter(v -> VOICE_NAME.equals(v.getShortName()))
                        .findFirst()
                        .orElse(TTSVoice.provides().get(0));

                TTS tts = new TTS(voice, text);
                try (java.io.ByteArrayOutputStream stream = tts.transToAudioStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                    stream.writeTo(fos);
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                startPlayback();
            } else {
                if (playbackListener != null) {
                    playbackListener.onPlaybackError("Ошибка загрузки Edge TTS");
                }
            }
        }
    }

    private void startPlayback() {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(cacheFile.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlaying = true;
                try {
                    mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(currentSpeed));
                } catch (Exception e) {
                    Log.e(TAG, "Error setting speed", e);
                }
                mp.start();
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(true);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Playback error", e);
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Ошибка при запуске файла");
            }
        }
    }

    @Override
    public void stop() {
        isPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        if (playbackListener != null) {
            playbackListener.onPlaybackStopped();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }

    @Override
    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(true);
            }
        }
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (mediaPlayer != null && isPlaying()) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            } catch (Exception e) {
                Log.e(TAG, "Error setting speed", e);
            }
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
