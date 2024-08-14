package org.jawbts.voskSocket;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoskThread extends Thread {
    private Thread thread;
    volatile private boolean shouldStop = false;
    volatile private boolean running = false;
    volatile private boolean modelRunning = false;
    volatile private boolean microphoneOn = false;
    private boolean microphoneRealOn = false;

    private final String path;
    private final int sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final int frameSize;
    private final int frameRate;

    public VoskThread(String path, int sampleRate, int sampleSizeInBits, int channels, int frameSize, int frameRate) {
        this.path = path;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
    }

    @Override
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void setShouldStop() {
        shouldStop = true;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isModelRunning() {
        return modelRunning;
    }

    public void setMicrophoneOn(boolean microphoneOn) {
        this.microphoneOn = microphoneOn;
    }

    private final Pattern pattern = Pattern.compile("\"([^\"]+)\"");
    // json中提取字符串
    private String match(String s) {
        Matcher matcher = pattern.matcher(s);
        boolean flag = true;
        while (matcher.find()) {
            if (!flag) {
                return matcher.group(1);
            }
            flag = false;
        }
        return "";
    }

    @Override
    public void run() {
        running = true;
        LibVosk.setLogLevel(LogLevel.DEBUG);

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine microphone;
        SourceDataLine speakers;

        try (Model model = new Model(path);
             Recognizer recognizer = new Recognizer(model, 120000)) {
            try {
                modelRunning = true;

                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                int numBytesRead;
                int CHUNK_SIZE = 1024;

                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                speakers.open(format);
                speakers.start();
                byte[] b = new byte[4096];

                while (!shouldStop) {
                    numBytesRead = microphone.read(b, 0, CHUNK_SIZE);

                    speakers.write(b, 0, numBytesRead);

                    if ((microphoneOn || microphoneRealOn) && recognizer.acceptWaveForm(b, numBytesRead)) {
                        String result = match(microphoneOn ? recognizer.getResult() : recognizer.getFinalResult());
                        Main.getSocketThread().getOutWriter().println("!_C" + EncryptUtils.getInstance().encrypt(result));
                    }

                    if (microphoneOn != microphoneRealOn) {
                        microphoneRealOn = microphoneOn;
                    }
                }
                speakers.drain();
                speakers.close();
                microphone.close();
            } catch (Exception e) {
                e.printStackTrace(Main.getSocketThread().getOutWriter());
            }
        } catch (Exception e) {
            e.printStackTrace(Main.getSocketThread().getOutWriter());
        } finally {
            running = false;
            modelRunning = false;
        }
    }
}
