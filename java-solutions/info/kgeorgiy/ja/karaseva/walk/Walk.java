package info.kgeorgiy.ja.karaseva.walk;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Walk {
    private static final long HASH_INIT = 0x0000000000000000L;
    private static final long SEPARATE_8_BYTES = 0xff00000000000000L;

    public static void main(String[] args) {
        if(args == null) {
            System.err.println("Array of arguments shouldn't be null");
            return;
        }

        if (args.length != 2) {
            System.err.println("2 arguments are expected");
            return;
        }

        if (args[0] == null || args[1] == null) {
            System.err.println("Arguments shouldn't be null");
            return;
        }

        try {
            // :NOTE: unused variable
            Path input = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path of input file: " + e.getMessage());
            return;
        }

        try {
            Path output = Paths.get(args[1]);
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
        } catch (InvalidPathException e) {
            System.err.println("Invalid path of output file: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println("Can't create directories for output file: " + e.getMessage());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(args[0]), StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    Paths.get(args[1]), StandardCharsets.UTF_8)){
                String path;
                while ((path = reader.readLine()) != null) {
                    String hash = String.format("%016x", hashPJW(path));
                    // :NOTE: \n
                    writer.write(hash + " " + path + "\n");
                }
            } catch (IOException e) {
                System.err.println("Output exception: " + e.getMessage());
            }
        } catch (NoSuchFileException e) {
            System.err.println("Input file doesn't exist: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Input exception: " + e.getMessage());
        }
    }

    private static long hashPJW(String path) {
        long hash = HASH_INIT;
        try (BufferedInputStream read = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            long high;
            int ch;
            // :NOTE: add buffer
            while((ch = read.read()) != -1) {
                hash = (hash << 8) + ch;
                high = hash & SEPARATE_8_BYTES;
                if (high != 0) {
                    hash ^= high >> 48;
                }
                hash &= ~high;
            }
        } catch (IOException | InvalidPathException e) {
            // :NOTE: add information about error in logs
            hash = HASH_INIT;
        }
        return hash;
    }
}
