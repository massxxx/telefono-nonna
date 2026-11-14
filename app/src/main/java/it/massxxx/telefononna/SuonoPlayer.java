package it.massxxx.telefononna;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

/** Suona sirena, suoneria o sveglia sul canale "allarme", che suona anche in silenzioso. */
public final class SuonoPlayer {
    private final Context ctx;
    private AudioTrack track;
    private MediaPlayer mp;

    public SuonoPlayer(Context c) {
        ctx = c.getApplicationContext();
    }

    public static AudioAttributes attributi() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    public synchronized void avvia(String tipo) {
        ferma();
        if ("suoneria".equals(tipo) || "sveglia".equals(tipo)) {
            int t = "suoneria".equals(tipo) ? RingtoneManager.TYPE_RINGTONE : RingtoneManager.TYPE_ALARM;
            try {
                Uri u = RingtoneManager.getActualDefaultRingtoneUri(ctx, t);
                if (u == null) u = RingtoneManager.getDefaultUri(t);
                if (u == null) u = Settings.System.DEFAULT_RINGTONE_URI;
                mp = new MediaPlayer();
                mp.setAudioAttributes(attributi());
                mp.setDataSource(ctx, u);
                mp.setLooping(true);
                mp.setVolume(1f, 1f);
                mp.prepare();
                mp.start();
                return;
            } catch (Exception e) {
                if (mp != null) {
                    try { mp.release(); } catch (Exception ignored) {}
                    mp = null;
                }
                // se la suoneria non si apre, si usa la sirena
            }
        }
        sirena();
    }

    /** Sirena "uuu-iii": sale da 650 a 1500 Hz e riscende, in 2 secondi, ripetuta. */
    private void sirena() {
        int sr = 44100;
        int n = sr * 2;
        short[] buf = new short[n];
        double fase = 0;
        double norm = Math.tanh(3);
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double f = t < 1 ? 650 + 850 * t : 1500 - 850 * (t - 1);
            fase += 2 * Math.PI * f / sr;
            double s = Math.tanh(3 * Math.sin(fase)) / norm; // forma quasi quadra: più forte
            buf[i] = (short) (s * 0.95 * 32767);
        }
        // chiude il giro su un passaggio per lo zero, per non sentire il "clic"
        int fine = n;
        for (int i = n - 1; i > n - 2000; i--) {
            if (buf[i - 1] < 0 && buf[i] >= 0) { fine = i; break; }
        }
        track = new AudioTrack.Builder()
                .setAudioAttributes(attributi())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(n * 2)
                .build();
        track.write(buf, 0, n);
        track.setLoopPoints(0, fine, -1);
        track.setVolume(1f);
        track.play();
    }

    public synchronized void ferma() {
        if (track != null) {
            try { track.stop(); } catch (Exception ignored) {}
            try { track.release(); } catch (Exception ignored) {}
            track = null;
        }
        if (mp != null) {
            try { mp.stop(); } catch (Exception ignored) {}
            try { mp.release(); } catch (Exception ignored) {}
            mp = null;
        }
    }
}
