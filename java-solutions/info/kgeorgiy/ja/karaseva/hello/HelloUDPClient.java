package info.kgeorgiy.ja.karaseva.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class HelloUDPClient implements HelloClient {
    final private static int TIMEOUT = 20; // :NOTE: _IN_MILLIS
    final private static int CAPACITY = 1024; // :NOTE: capacity of what

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1");
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + host);
            return;
        }

        ExecutorService workers = Executors.newFixedThreadPool(threads);
        Phaser phaser = new Phaser(1);

        for (int i = 0; i < threads; i++) {
            final int threadNumber = i;
            phaser.register();
            workers.execute(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);
                    for (int requestNumber = 0; requestNumber < requests; requestNumber++) {

                        String message = prefix + threadNumber + "_" + requestNumber;
                        byte[] messageByteBuffer = message.getBytes();
                        DatagramPacket requestPacket = new DatagramPacket(
                                messageByteBuffer, messageByteBuffer.length, ip, port);

                        String answer = "";
                        String expected = "Hello, " + message;
                        while (!answer.equals(expected)) {
                            try {
                                socket.send(requestPacket);
                                DatagramPacket receivedPacket = new DatagramPacket(new byte[CAPACITY], CAPACITY);
                                socket.receive(receivedPacket);
                                answer = new String(receivedPacket.getData(),
                                        receivedPacket.getOffset(), receivedPacket.getLength());
                            } catch (SocketTimeoutException e) {
                                System.err.println("Timeout error for request: " + message);
                            } catch (IOException e) {
                                System.err.println("IOException: " + e.getMessage());
                            }
                        }

                        System.out.println("request: " + message + "; answer: " + answer);
                    }
                } catch (SocketException e) {
                    System.err.println("Socket couldn't be opened: " + e.getMessage());
                } finally {
                    phaser.arrive();
                }
            });
        }

        phaser.arriveAndAwaitAdvance();
        // :NOTE: wait for termination
        workers.shutdown();
    }

    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments mustn't be null");
            return;
        }
        if(args.length != 5) {
            System.err.println("expected 5 arguments: HelloUDClient [host] [port] [prefix] [threads] [requests]");
            return;
        }
        int port, threads, requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("[port], [threads] and [request] must be numbers");
            return;
        }
        HelloUDPClient client = new HelloUDPClient();
        client.run(args[0], port, args[2], threads, requests);
    }
}
