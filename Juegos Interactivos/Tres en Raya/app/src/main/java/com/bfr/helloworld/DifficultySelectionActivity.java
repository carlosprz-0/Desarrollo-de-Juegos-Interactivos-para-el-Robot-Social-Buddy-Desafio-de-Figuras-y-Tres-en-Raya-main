package com.bfr.helloworld;

// Importaciones necesarias para funcionalidades básicas de Android, SDK de Buddy y multimedia
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;

// Actividad que muestra opciones de dificultad (fácil o difícil)
public class DifficultySelectionActivity extends BuddyActivity {
    private Button buttonNormal;
    private Button buttonDificil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Carga el layout con los botones
        setContentView(R.layout.options_buttons_layout);

        // Referencia a los botones desde el XML
        buttonNormal = findViewById(R.id.buttonReplayYes);
        buttonDificil = findViewById(R.id.buttonReplayNo);

        // Asignación de texto a los botones
        buttonNormal.setText("Fácil");
        buttonDificil.setText("Difícil");

        // Oculta los botones hasta que Buddy termine de hablar
        buttonNormal.setVisibility(View.GONE);
        buttonDificil.setVisibility(View.GONE);

        // Acción cuando el usuario elige "Fácil"
        buttonNormal.setOnClickListener(v -> {
            playSound(R.raw.button_selection);  // Sonido al pulsar
            saveDifficulty("facil");            // Guardar dificultad
            launchGame();                       // Lanzar el juego
        });

        // Acción cuando el usuario elige "Difícil"
        buttonDificil.setOnClickListener(v -> {
            playSound(R.raw.button_selection);  // Sonido al pulsar
            saveDifficulty("dificil");          // Guardar dificultad
            launchGame();                       // Lanzar el juego
        });
    }

    // Método que se llama cuando el SDK de Buddy está listo
    @Override
    public void onSDKReady() {
        // Buddy habla preguntando la dificultad
        BuddySDK.Speech.startSpeaking("¿Qué dificultad quieres?", new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) {
                // Muestra los botones con animación después de que Buddy hable
                runOnUiThread(() -> {
                    animateShow(buttonNormal);
                    animateShow(buttonDificil);
                });
            }

            @Override public void onPause() {}
            @Override public void onResume() {}
            @Override public void onError(String s) throws RemoteException {}
            @Override public IBinder asBinder() { return null; }
        });
    }

    // Guarda la dificultad elegida en preferencias compartidas
    private void saveDifficulty(String level) {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        prefs.edit().putString("difficulty", level).apply();
    }

    // Inicia la actividad del juego principal y marca que se jugará contra la IA
    private void launchGame() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("playAgainstAI", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish(); // Cierra esta actividad para que no se pueda volver con el botón "Atrás"
    }

    // Reproduce un sonido usando MediaPlayer
    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(MediaPlayer::release); // Libera el recurso al terminar
            mediaPlayer.start();
        }
    }

    // Animación para mostrar suavemente un botón
    public void animateShow(View view) {
        view.setVisibility(View.VISIBLE);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start();
    }
}

