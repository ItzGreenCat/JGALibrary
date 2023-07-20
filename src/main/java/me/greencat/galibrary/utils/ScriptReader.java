package me.greencat.galibrary.utils;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScriptReader {
    public static void read(File file, Consumer<String> command){
        try {
            try(FileInputStream inputStream = new FileInputStream(file)){
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    command.accept(line);
                }
                reader.close();
            }

        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
