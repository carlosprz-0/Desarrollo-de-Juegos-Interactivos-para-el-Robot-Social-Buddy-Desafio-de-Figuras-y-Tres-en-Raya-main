package com.bfr.helloworld;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Clase principal que representa una actividad donde se selecciona un juego
public class GameSelectorActivity extends AppCompatActivity {

    private ListView lista;                     // Lista visual para mostrar juegos
    private ArrayAdapter<String> adapter;      // Adaptador para llenar la lista
    private File puzzlesRootDir;               // Carpeta raíz donde se guardan los juegos
    private File activeFile;                   // Archivo para registrar el juego activo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);  // Establece el layout de la actividad

        // Referencia al layout raíz para aplicar animaciones
        View rootLayout = findViewById(R.id.layoutSelector);
        if (rootLayout != null) {
            // Se aplica una animación de escala y opacidad para suavizar la entrada
            rootLayout.setScaleX(0.8f);
            rootLayout.setScaleY(0.8f);
            rootLayout.setAlpha(0f);
            rootLayout.setVisibility(View.VISIBLE);

            rootLayout.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        lista = findViewById(R.id.listaJuegos);  // Se obtiene la referencia a la lista de juegos
        puzzlesRootDir = new File(getFilesDir(), "puzzles");  // Carpeta donde se guardan los juegos
        activeFile = new File(puzzlesRootDir, "active.txt");  // Archivo que indica cuál juego está activo

        // Oculta la barra de navegación y hace la actividad en pantalla completa (modo inmersivo)
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // Crea el directorio si no existe
        if (!puzzlesRootDir.exists()) puzzlesRootDir.mkdirs();

        // Lista los subdirectorios dentro de "puzzles", que representan juegos
        File[] dirs = puzzlesRootDir.listFiles(File::isDirectory);
        List<String> juegos = new ArrayList<>();
        if (dirs != null) {
            for (File d : dirs) juegos.add(d.getName());  // Agrega los nombres de los juegos a la lista
        }

        // Llena la lista visual con los nombres de los juegos encontrados
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, juegos);
        lista.setAdapter(adapter);

        // Maneja el evento de clic en un juego de la lista
        lista.setOnItemClickListener((parent, view, position, id) -> {
            playSound(R.raw.click_button);  // Reproduce un sonido al hacer clic
            String seleccionado = juegos.get(position);  // Obtiene el nombre del juego seleccionado
            Intent result = new Intent();  // Crea un intent para devolver el resultado
            result.putExtra("selectedGame", seleccionado);  // Añade el nombre del juego como resultado
            setResult(RESULT_OK, result);  // Establece el resultado para la actividad que llamó
            finish();  // Finaliza esta actividad y vuelve a la anterior
        });
    }

    // Método para reproducir un sonido usando MediaPlayer
    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);  // Libera recursos tras reproducirse
            mediaPlayer.start();  // Inicia la reproducción
        }
    }
}

