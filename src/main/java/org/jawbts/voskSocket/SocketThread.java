package org.jawbts.voskSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketThread extends Thread {
    private Thread thread;
    private VoskThread voskThread;
    volatile private ServerSocket serverSocket;
    volatile private boolean shouldStop = false;
    volatile private boolean running = false;
    private final int port;
    private PrintWriter out;

    public SocketThread(int port) {
        this.port = port;
    }

    public void setShouldStop() {
        shouldStop = true;
    }

    public PrintWriter getOutWriter() {
        return out;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void run() {
        try {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                serverSocket = null;
                System.out.println("Can't create server. Maybe port have already been used.");
                return;
            }

            System.out.println("Socket server running on: " + serverSocket.getLocalPort());
            try (Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ) {
                running = true;
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                String inputLine;
                boolean clientThinkVoskThreadRunning = false, clientThinkVoskModelRunning = false;
                while (!shouldStop) {
                    if (in.ready() && (inputLine = in.readLine()) != null) {
                        if (inputLine.startsWith("C")) {
                            String command = inputLine.substring(1);

                            // 全参数启动
                            if (command.startsWith("B")) {
                                boot(command);
                            } else if (command.equals("shut")) {
                                if (voskThread != null) {
                                    voskThread.setShouldStop();
                                    while(voskThread.isRunning());
                                    voskThread = null;
                                }
                            } else if (command.equals("MO")) {
                                if (voskThread != null) voskThread.setMicrophoneOn(true);
                            } else if (command.equals("MF")) {
                                if (voskThread != null) voskThread.setMicrophoneOn(false);
                            } else if (command.equals("disconnect")) {
                                break;
                            } else {
                                throw new Exception("Client sent something wrong");
                            }
                        } else {
                            throw new Exception("Client sent something wrong");
                        }
                    }

                    if (voskThread != null) {
                        if (voskThread.isRunning() != clientThinkVoskThreadRunning) {
                            clientThinkVoskThreadRunning = voskThread.isRunning();
                            out.println("!_voskThreadRunning " + clientThinkVoskThreadRunning);
                        }
                        if (voskThread.isModelRunning() != clientThinkVoskModelRunning) {
                            clientThinkVoskModelRunning = voskThread.isModelRunning();
                            out.println("!_voskModelRunning " + clientThinkVoskModelRunning);
                        }
                    }
                }
            } finally {
                out.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            running = false;
            if (voskThread != null) voskThread.setShouldStop();
            voskThread = null;
            try {
                if (serverSocket != null) serverSocket.close();
                System.out.println("Socket server shut down.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void boot(String command) throws Exception {
        String content = command.substring(1);
        String[] strings = content.split("\\+");
        // 0: 模型路径 1: SampleRate 2: SampleSizeInBits 3: Channels 4: FrameSize 5: FrameRate
        if (strings.length != 6) {
            throw new Exception("Client sent something wrong");
        }
        String path = strings[0];
        int sampleRate = Integer.parseInt(strings[1]);
        int sampleSizeInBits = Integer.parseInt(strings[2]);
        int channels = Integer.parseInt(strings[3]);
        int frameSize = Integer.parseInt(strings[4]);
        int frameRate = Integer.parseInt(strings[5]);
        if (voskThread != null) voskThread.setShouldStop();
        voskThread = new VoskThread(path, sampleRate, sampleSizeInBits, channels, frameSize, frameRate);
        voskThread.start();
    }
}
