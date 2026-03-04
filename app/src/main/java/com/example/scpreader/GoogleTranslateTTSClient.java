package com.example.scpreader;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GoogleTranslateTTSClient implements TTSClient {
    private static final String TAG = "GoogleTTSClient";
    private static final int MAX_CHUNK_LENGTH = 200;
    private static final String TTS_URL_BASE = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ru&client=tw-ob&ttsspeed=1&q=";

    private MediaPlayer playerA;
    private MediaPlayer playerB;
    private MediaPlayer currentPlayer;
    private MediaPlayer nextPlayer;

    private List<String> chunks = new ArrayList<>();
    private int currentChunkIndex = -1;
    private int preparedChunkIndex = -1;
    private boolean isPlaying = false;
    private boolean isNextPlayerPrepared = false;
    private OnPlaybackEventListener playbackListener;
    private float currentSpeed = 1.0f;

    public GoogleTranslateTTSClient(OnPlaybackEventListener listener) {
        this.playbackListener = listener;
        playerA = createMediaPlayer();
        playerB = createMediaPlayer();
    }

    private MediaPlayer createMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());

        mp.setOnCompletionListener(this::onPlayerCompletion);
        mp.setOnErrorListener((player, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
            if (player == currentPlayer) {
                stop();
                if (playbackListener != null) {
                    playbackListener.onPlaybackError("Ошибка воспроизведения: " + what);
                }
            }
            return true;
        });
        return mp;
    }

    private void onPlayerCompletion(MediaPlayer mp) {
        if (!isPlaying) return;

        if (isNextPlayerPrepared && preparedChunkIndex == currentChunkIndex + 1) {
            MediaPlayer temp = currentPlayer;
            currentPlayer = nextPlayer;
            nextPlayer = temp;

            currentChunkIndex++;
            isNextPlayerPrepared = false;
            currentPlayer.start();
            
            prepareNextChunk();
        } else {
            currentChunkIndex++;
            if (currentChunkIndex < chunks.size()) {
                playChunkDirectly(currentChunkIndex);
            } else {
                stop();
            }
        }
    }

    @Override
    public void play(String text) {
        stop();
        chunks = splitText(text);
        if (chunks.isEmpty()) {
            if (playbackListener != null) {
                playbackListener.onPlaybackError("Нет текста для озвучки");
            }
            return;
        }

        isPlaying = true;
        currentChunkIndex = 0;
        preparedChunkIndex = -1;
        isNextPlayerPrepared = false;

        if (playbackListener != null) {
            playbackListener.onPlaybackStarted();
        }

        currentPlayer = playerA;
        nextPlayer = playerB;
        playChunkDirectly(currentChunkIndex);
    }

    private void playChunkDirectly(int index) {
        try {
            String url = TTS_URL_BASE + URLEncoder.encode(chunks.get(index), "UTF-8");
            currentPlayer.reset();
            currentPlayer.setDataSource(url);
            currentPlayer.setPlaybackParams(currentPlayer.getPlaybackParams().setSpeed(currentSpeed));
            currentPlayer.setOnPreparedListener(mp -> {
                if (isPlaying && currentChunkIndex == index) {
                    mp.start();
                    prepareNextChunk();
                }
            });
            currentPlayer.prepareAsync();
        } catch (IOException e) {
            handleError("Ошибка при запуске чанка", e);
        }
    }

    private void prepareNextChunk() {
        int nextIndex = currentChunkIndex + 1;
        if (nextIndex >= chunks.size()) return;

        try {
            preparedChunkIndex = nextIndex;
            isNextPlayerPrepared = false;
            String url = TTS_URL_BASE + URLEncoder.encode(chunks.get(nextIndex), "UTF-8");
            nextPlayer.reset();
            nextPlayer.setDataSource(url);
            nextPlayer.setPlaybackParams(nextPlayer.getPlaybackParams().setSpeed(currentSpeed));
            nextPlayer.setOnPreparedListener(mp -> {
                if (preparedChunkIndex == nextIndex) {
                    isNextPlayerPrepared = true;
                }
            });
            nextPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error preparing next chunk", e);
        }
    }

    @Override
    public void stop() {
        isPlaying = false;
        if (playerA != null) playerA.reset();
        if (playerB != null) playerB.reset();
        chunks.clear();
        currentChunkIndex = -1;
        preparedChunkIndex = -1;
        isNextPlayerPrepared = false;
        if (playbackListener != null) {
            playbackListener.onPlaybackStopped();
        }
    }

    @Override
    public void pause() {
        if (currentPlayer != null && currentPlayer.isPlaying()) {
            currentPlayer.pause();
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }

    @Override
    public void resume() {
        if (currentPlayer != null && !currentPlayer.isPlaying()) {
            currentPlayer.start();
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(true);
            }
        }
    }

    @Override
    public boolean isPlaying() {
        return currentPlayer != null && currentPlayer.isPlaying();
    }

    @Override
    public void seekTo(int position) {
        if (currentPlayer != null) {
            currentPlayer.seekTo(position);
        }
    }

    @Override
    public int getDuration() {
        if (currentPlayer != null) {
            try {
                return currentPlayer.getDuration();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (currentPlayer != null) {
            try {
                return currentPlayer.getCurrentPosition();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (playerA != null && playerA.isPlaying()) {
            playerA.setPlaybackParams(playerA.getPlaybackParams().setSpeed(speed));
        }
        if (playerB != null && playerB.isPlaying()) {
            playerB.setPlaybackParams(playerB.getPlaybackParams().setSpeed(speed));
        }
    }

    private void handleError(String message, Exception e) {
        Log.e(TAG, message, e);
        if (playbackListener != null) {
            playbackListener.onPlaybackError(message);
        }
        stop();
    }


    private List<String> splitText(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        text = text.replaceAll("\\s+", " ").trim();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());
            
            if (end < text.length()) {
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
        isPlaying = false;
        if (playerA != null) {
            playerA.release();
            playerA = null;
        }
        if (playerB != null) {
            playerB.release();
            playerB = null;
        }
    }
}
