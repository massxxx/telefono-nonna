package it.massxxx.telefononna;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Schermata a tutto schermo col messaggio, mentre il telefono squilla. Si chiude solo da remoto. */
public class SquilloActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private LinearLayout radice;
    private TextView icona, messaggio, nota;
    private boolean rosso = true;
    private boolean registrato;

    private final BroadcastReceiver chiudi = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finish(); }
    };

    private final Runnable lampeggia = new Runnable() {
        @Override public void run() {
            rosso = !rosso;
            int sfondo = rosso ? Color.parseColor("#D32F2F") : Color.parseColor("#FFC107");
            int testo = rosso ? Color.WHITE : Color.BLACK;
            radice.setBackgroundColor(sfondo);
            messaggio.setTextColor(testo);
            nota.setTextColor(testo);
            icona.setTextColor(testo);
            h.postDelayed(this, 600);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        radice = new LinearLayout(this);
        radice.setOrientation(LinearLayout.VERTICAL);
        radice.setGravity(Gravity.CENTER);
        int pad = Ui.dp(this, 24);
        radice.setPadding(pad, pad, pad, pad);
        radice.setBackgroundColor(Color.parseColor("#D32F2F"));

        icona = new TextView(this);
        icona.setText("📞");
        icona.setTextSize(96);
        icona.setGravity(Gravity.CENTER);
        radice.addView(icona);

        messaggio = new TextView(this);
        messaggio.setTextSize(38);
        messaggio.setTypeface(Typeface.DEFAULT_BOLD);
        messaggio.setGravity(Gravity.CENTER);
        messaggio.setTextColor(Color.WHITE);
        messaggio.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        radice.addView(messaggio);

        nota = new TextView(this);
        nota.setText("Il suono si spegnerà quando lo fermeranno a distanza.");
        nota.setTextSize(18);
        nota.setGravity(Gravity.CENTER);
        nota.setTextColor(Color.WHITE);
        radice.addView(nota);

        setContentView(radice);
    }

    @Override
    protected void onResume() {
        super.onResume();
        messaggio.setText(new Prefs(this).messaggio());
        if (!registrato) {
            IntentFilter f = new IntentFilter(NonnaService.CHIUDI);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(chiudi, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(chiudi, f);
            registrato = true;
        }
        // Ripiego: se il servizio non era partito dallo sfondo, lo avvia ora che la schermata è visibile
        String avvia = getIntent().getStringExtra("avvia");
        if (avvia != null) {
            getIntent().removeExtra("avvia");
            new Prefs(this).annota("↪ Schermata aperta, avvio lo squillo da qui");
            NonnaService.comando(this, avvia, getIntent().getStringExtra("mittente"));
        } else if (!new Prefs(this).squillando()) {
            finish();
            return;
        }
        h.removeCallbacks(lampeggia);
        h.post(lampeggia);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(lampeggia);
    }

    @Override
    protected void onDestroy() {
        if (registrato) {
            try { unregisterReceiver(chiudi); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // i tasti del volume non abbassano lo squillo
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) return true;
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // il tasto indietro non chiude: lo squillo si ferma solo da remoto
    }
}
