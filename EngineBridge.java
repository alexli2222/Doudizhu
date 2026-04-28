import java.io.BufferedReader;
import java.io.IO;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EngineBridge {
    private Process engineProcess;
    private PrintWriter writer;
    private BufferedReader reader;
    private final BlockingQueue<String> moves = new LinkedBlockingQueue<String>();

    public EngineBridge (String executablePath) {
        try {
            engineProcess = new ProcessBuilder(executablePath).start();

            writer = new PrintWriter(new OutputStreamWriter(engineProcess.getOutputStream()), true);
            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        } catch (IOException ex) {
            System.out.println("Somting wong: " + ex.getMessage());
        }
    }

    public void sendCommand(String command) {
        if (writer != null) {
            writer.println(command);
            writer.flush();

            if (!command.equals("go")) System.out.println("Java sent: " + command);
        }
    }

    public void startListening(boolean first) {
        if (!first) return;

        Thread listenerThread = new Thread(() -> {
            try {
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println("C++ says: " + line);

                    if (line.startsWith("bestmove")) {
                        String[] parts = line.split(" ");

                        if (parts.length > 1) moves.offer(parts[1]);
                    }
                }
            } catch (IOException ex) {
                System.out.println("something wrong listener: " + ex.getMessage());
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public String waitForBestMove() {
        try {
            return moves.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
