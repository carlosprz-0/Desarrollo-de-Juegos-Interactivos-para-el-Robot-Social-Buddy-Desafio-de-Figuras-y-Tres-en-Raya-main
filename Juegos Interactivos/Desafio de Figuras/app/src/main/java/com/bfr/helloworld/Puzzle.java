package com.bfr.helloworld;

import android.content.Context;
import java.io.File;

// Clase que representa un puzzle individual del juego
public class Puzzle {
    public String mainImagePath;              // Ruta absoluta a la imagen principal del puzzle
    public String[] optionPaths = new String[3]; // Rutas absolutas a las imágenes de opciones (3 opciones)
    public int correctOptionIndex;            // Índice de la opción correcta (0, 1 o 2)
    public String prompt;                     // Enunciado o descripción del puzzle
    public String difficulty;                 // Dificultad del puzzle

    // Constructor que toma un objeto JSON, el contexto y el nombre del juego
    public Puzzle(PuzzleJson json, Context context, String gameName) {
        this.prompt = json.prompt;                            // Copia el enunciado
        this.correctOptionIndex = json.correctIndex;          // Copia el índice correcto
        this.difficulty = json.difficulty;                    // Copia la dificultad

        // Construye la ruta del directorio del juego dentro de los archivos internos de la app
        File gameDir = new File(context.getFilesDir(), "puzzles/" + gameName);

        // Asigna la ruta absoluta a la imagen principal del puzzle
        this.mainImagePath = new File(gameDir, json.puzzleImage + ".png").getAbsolutePath();

        // Asigna las rutas absolutas a las imágenes de las tres opciones
        for (int i = 0; i < 3; i++) {
            this.optionPaths[i] = new File(gameDir, json.options[i] + ".png").getAbsolutePath();
        }
    }
}

// Clase auxiliar para mapear datos JSON de un puzzle
class PuzzleJson {
    public String puzzleImage;     // Nombre del archivo de la imagen del puzzle (sin extensión)
    public String[] options;       // Nombres de archivos de las opciones (sin extensión)
    public int correctIndex;       // Índice de la opción correcta
    public String prompt;          // Enunciado o texto del puzzle
    public String difficulty;      // Dificultad del puzzle
}
