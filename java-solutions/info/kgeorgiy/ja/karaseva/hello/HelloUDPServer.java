package info.kgeorgiy.ja.karaseva.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    final private static int TIMEOUT = 1000;
    final private static int CAPACITY = 1024;
    private ExecutorService workers;
    private DatagramSocket socket;
    private boolean started;

    public HelloUDPServer() {
        started = false;
    }

    @Override
    public void start(int port, int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1");
        }

        if (started) {
            throw new IllegalStateException("HelloUDPServer is already started");
        }

        started = true;

        workers = Executors.newFixedThreadPool(threads);

        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(TIMEOUT);
            for (int i = 0; i < threads; i++) {
                workers.execute(() -> {
                    while (started) {
                        try {
                            DatagramPacket receivedPacket = new DatagramPacket(new byte[CAPACITY], CAPACITY);
                            socket.receive(receivedPacket);
                            String receivedMessage = new String(
                                    receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
                            byte[] answerByteBuffer = ("Hello, " + receivedMessage).getBytes();
                            DatagramPacket answerPacket = new DatagramPacket(answerByteBuffer, answerByteBuffer.length,
                                    receivedPacket.getAddress(), receivedPacket.getPort());
                            socket.send(answerPacket);
                        } catch (SocketTimeoutException e) {
                            System.err.println("Timeout error for waiting request");
                        } catch (SocketException e) {
                            System.err.println("Socket was already closed");
                        } catch (IOException e) {
                            System.err.println("IOException: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (SocketException e) {
            System.err.println("Socket couldn't be opened: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (!started) {
            throw new IllegalStateException("HelloUDPServer isn't started");
        }
        started = false;
        socket.close();
        workers.shutdownNow();
    }

    public static void main(String[] args) {
        if(args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments mustn't be null");
            return;
        }
        if(args.length != 2) {
            System.err.println("expected 2 arguments: HelloUDPServer [port] [threads]");
            return;
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("[port] and [threads] must be numbers");
            return;
        }
        try (HelloUDPServer server = new HelloUDPServer()) {
            server.start(port, threads);
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }
}
