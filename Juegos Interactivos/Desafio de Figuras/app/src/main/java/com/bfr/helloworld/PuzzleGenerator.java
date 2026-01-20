package com.bfr.helloworld;

import android.content.Context;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Clase auxiliar pensada para representar puzzles clasificados por dificultad,

public class PuzzleGenerator {

    // Método estático que genera una lista de puzzles a partir de un archivo JSON
    public static List<Puzzle> generate(Context context, String gameName) {
        List<Puzzle> result = new ArrayList<>();  // Lista que contendrá los puzzles finales

        // Construye la ruta al archivo JSON donde están definidos los puzzles
        File puzzlesFile = new File(context.getFilesDir(), "puzzles/" + gameName + "/puzzles_detailed.json");

        // Si el archivo no existe, registra un error y devuelve una lista vacía
        if (!puzzlesFile.exists()) {
            Log.e("PUZZLE_GEN", "❌ Archivo puzzles_detailed.json no encontrado en: " + puzzlesFile.getAbsolutePath());
            return result;
        }

        try {
            // Abre el archivo para lectura con codificación UTF-8
            InputStreamReader reader = new InputStreamReader(new FileInputStream(puzzlesFile), StandardCharsets.UTF_8);

            // Define el tipo de dato esperado: una lista de objetos PuzzleJson
            Type listType = new TypeToken<List<PuzzleJson>>() {}.getType();

            // Usa la librería Gson para convertir el JSON en una lista de objetos PuzzleJson
            List<PuzzleJson> puzzleJsonList = new Gson().fromJson(reader, listType);

            // Verifica si el contenido del JSON es nulo o inválido
            if (puzzleJsonList == null) {
                Log.e("PUZZLE_GEN", "❌ El JSON es nulo o malformado.");
                return result;
            }

            // Ordena la lista de puzzles por dificultad: fácil, luego normal, luego difícil
            Collections.sort(puzzleJsonList, (p1, p2) -> getDifficultyOrder(p1.difficulty) - getDifficultyOrder(p2.difficulty));

            // Recorre cada PuzzleJson y lo convierte en un objeto Puzzle, agregándolo a la lista final
            for (PuzzleJson pj : puzzleJsonList) {
                Puzzle p = new Puzzle(pj, context, gameName);
                result.add(p);
            }

        } catch (Exception e) {
            // En caso de error al leer el archivo o parsear el JSON, lo registra en el log
            Log.e("PUZZLE_GEN", "❌ Error cargando el archivo JSON: " + e.getMessage(), e);
        }

        // Muestra en el log cuántos puzzles se cargaron correctamente
        Log.d("PUZZLE_GEN", "✅ Total puzzles cargados: " + result.size());
        return result;  // Devuelve la lista de puzzles
    }

    // Método auxiliar que convierte la dificultad textual en un valor numérico para ordenamiento
    private static int getDifficultyOrder(String difficulty) {
        if (difficulty == null) return 3;  // Si no se especifica dificultad, se pone al final
        switch (difficulty.toLowerCase()) {
            case "facil": return 0;    // Primero los fáciles
            case "normal": return 1;   // Luego los normales
            case "dificil": return 2;  // Después los difíciles
            default: return 3;         // Cualquier otra dificultad va al final
        }
    }
}
