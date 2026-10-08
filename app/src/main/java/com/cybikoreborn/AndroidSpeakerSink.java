package com.cybikoreborn;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import com.github.daberkow.SpeakerOutput;

/** Plays the Cybiko 1-bit speaker (8-bit unsigned PCM, 48 kHz mono) through AudioTrack without blocking emulation. */
final class AndroidSpeakerSink implements SpeakerOutput.PcmSink {
  private AudioTrack track;

  AndroidSpeakerSink() {
    try {
      int min = AudioTrack.getMinBufferSize(SpeakerOutput.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT);
      track = new AudioTrack.Builder()
          .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
          .setAudioFormat(new AudioFormat.Builder().setSampleRate(SpeakerOutput.SAMPLE_RATE)
              .setEncoding(AudioFormat.ENCODING_PCM_8BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
          .setBufferSizeInBytes(Math.max(min, 8192) * 2)
          .setTransferMode(AudioTrack.MODE_STREAM).build();
      track.play();
    } catch (Exception e) { track = null; }
  }

  @Override public void write(byte[] data, int offset, int length) {
    AudioTrack t = track;
    if (t == null) return;
    // Never block or throw into the emulation loop (e.g. track released during shutdown).
    try { t.write(data, offset, length, AudioTrack.WRITE_NON_BLOCKING); } catch (RuntimeException ignored) { }
  }

  @Override public void close() {
    AudioTrack t = track; track = null;
    if (t != null) { try { t.stop(); } catch (Exception ignored) {} t.release(); }
  }
}
