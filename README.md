# 📞 Telefono Nonna

[![Scarica l'APK per Android](https://img.shields.io/badge/Scarica-APK%20Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/massxxx/telefono-nonna/releases/latest/download/TelefonoNonna.apk)

App Android per far squillare **al massimo volume** il telefono della nonna con un comando a distanza, anche se è in silenzioso o in "Non disturbare". Funziona via **SMS**: non serve internet.

**Scarica l'APK:** https://github.com/massxxx/telefono-nonna/releases/latest/download/TelefonoNonna.apk

## Cosa fa

| Pulsante sul telecomando | Cosa succede sul telefono della nonna | Risposta via SMS |
|---|---|---|
| 🔔 Fai squillare | Suona al massimo (sirena, suoneria o sveglia), schermo acceso con il messaggio, i tasti volume non lo abbassano | "Sta squillando… Batteria 80%" |
| ⏹ Ferma lo squillo | Smette di suonare, rimette il volume e il "Non disturbare" come prima | "Squillo fermato" |
| 📍 Dove si trova? | Cerca la posizione GPS (fino a 1 minuto) | Link a Google Maps + precisione |
| ℹ️ È acceso? | — | "Telefono acceso… Batteria 80%" |

Lo squillo si ferma **solo dal telecomando** (c'è uno stop automatico di sicurezza facoltativo, spento di base).

## Installazione

Installa lo stesso APK su **due** telefoni Android.

### Telefono della nonna
1. Apri l'app → **"Questo è il telefono della NONNA"**.
2. Annota il **codice (PIN)** che appare (puoi cambiarlo), scegli suono e messaggio, premi **Salva**.
3. Concedi **tutti** i permessi della lista finché sono tutti ✅:
   SMS, Posizione + "Consenti sempre", Notifiche, Superare "Non disturbare", Mostra sopra le altre app, Batteria senza limitazioni, Schermata a tutto schermo.
4. Premi **Prova completa (10 secondi)**.
5. Su Xiaomi/Huawei/Oppo: attiva anche "Avvio automatico" per l'app nelle impostazioni del telefono.

### Telecomando (il tuo telefono)
1. Apri l'app → **"Questo è il TELECOMANDO"**.
2. Scrivi il **numero della nonna** e lo **stesso codice**, premi Salva, concedi i permessi SMS.
3. Premi **È ACCESO?** per provare il collegamento.

### Senza l'app (da qualsiasi telefono, anche iPhone)
Manda un SMS al numero della nonna: `NONNA 1234 SQUILLA` (oppure `STOP`, `DOVE`, `STATO`), con il codice giusto al posto di 1234.

## Note
- Se Android blocca il permesso SMS con "Impostazione con restrizioni": Info app → menu ⋮ → **Consenti impostazioni con restrizioni**.
- Play Protect può avvisare perché l'app usa gli SMS e non viene dal Play Store: scegli "Installa comunque".
- Ogni comando e ogni risposta è un SMS normale (di solito incluso nella tariffa). Gli SMS dei comandi restano visibili anche nei messaggi della nonna.
- Il telefono della nonna deve essere acceso e avere campo.

## Tecnica
Java nativo, nessuna libreria esterna, minSdk 26 (Android 8), targetSdk 34. Il suono va sul canale **allarme** (`USAGE_ALARM`) portato al massimo e riportato al massimo ogni secondo; con il permesso "Non disturbare" l'app spegne temporaneamente la modalità e poi la ripristina. GitHub Actions compila l'APK firmato con `chiave.keystore` e lo pubblica nella release `app` a ogni push.
