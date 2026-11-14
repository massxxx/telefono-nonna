package it.massxxx.telefononna;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Piccoli aiuti per costruire le schermate senza file XML. */
final class Ui {
    static final int ROSSO = Color.parseColor("#C62828");
    static final int VERDE = Color.parseColor("#2E7D32");
    static final int BLU = Color.parseColor("#1565C0");
    static final int GRIGIO = Color.parseColor("#616161");
    static final int ARANCIO = Color.parseColor("#EF6C00");

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout.LayoutParams riga(Context c, int sopra) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, sopra);
        return lp;
    }

    static TextView titolo(Context c, String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(26);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(Color.parseColor("#212121"));
        v.setLayoutParams(riga(c, 8));
        return v;
    }

    static TextView sezione(Context c, String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(19);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(ROSSO);
        v.setLayoutParams(riga(c, 22));
        return v;
    }

    static TextView testo(Context c, String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(16);
        v.setTextColor(Color.parseColor("#424242"));
        v.setLayoutParams(riga(c, 6));
        return v;
    }

    static Button bottone(Context c, String t, int colore, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(19);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setColor(colore);
        g.setCornerRadius(dp(c, 14));
        b.setBackground(g);
        b.setMinHeight(dp(c, 60));
        b.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        b.setLayoutParams(riga(c, 10));
        b.setOnClickListener(l);
        return b;
    }

    static Button piccolo(Context c, String t, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setOnClickListener(l);
        return b;
    }

    static EditText campo(Context c, String suggerimento, String valore, int tipo) {
        EditText e = new EditText(c);
        e.setHint(suggerimento);
        e.setText(valore);
        e.setTextSize(18);
        e.setInputType(tipo);
        e.setLayoutParams(riga(c, 4));
        return e;
    }

    static EditText campoTesto(Context c, String suggerimento, String valore) {
        EditText e = campo(c, suggerimento, valore,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setMinLines(2);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    static View riquadro(Context c, View... figli) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor("#FFF3E0"));
        g.setCornerRadius(dp(c, 12));
        box.setBackground(g);
        int p = dp(c, 12);
        box.setPadding(p, p, p, p);
        box.setLayoutParams(riga(c, 12));
        for (View f : figli) box.addView(f);
        return box;
    }
}
