package org.jawbts.voskSocket;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String helpInfo = """
            Welcome.
            Type exit to exit.
            Type password [new password] to set password
            Type port [new port] to set port
            Type reboot to reboot the socket server""";
    private static final String configDir = "./config.txt";

    public static List<String> getConfigs() throws IOException {
        File configFile = new File(configDir);
        if (!configFile.exists()) {
            boolean ignored = configFile.createNewFile();
        }

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(configFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<String> configs = new ArrayList<>();
        String line;
        while((line = br.readLine()) != null) {
            configs.add(line);
        }
        br.close();

        return configs;
    }

    public static void setConfigs(List<String> configs) throws IOException {
        File configFile = new File(configDir);
        boolean ignored;
        if (configFile.exists()) {
            ignored = configFile.delete();
        }
        ignored = configFile.createNewFile();

        BufferedWriter bw = new BufferedWriter(new FileWriter(configFile));
        for (String config : configs) {
            bw.write(config + "\n");
        }
        bw.flush();
        bw.close();
    }

    private static String password;
    private static int port;
    private static String getPasswordInConfig() {
        try {
            List<String> configs = getConfigs();
            if (configs.isEmpty()) return "";
            return configs.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getPortInConfig() {
        try {
            List<String> configs = getConfigs();
            if (configs.size() < 2) return -1;
            return Integer.parseInt(configs.get(1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setPasswordInConfig(String password) {
        try {
            List<String> configs = List.of(password, Integer.toString(port));
            setConfigs(configs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setPortInConfig(int port) {
        try {
            List<String> configs = List.of(password, Integer.toString(port));
            setConfigs(configs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPassword() {
        return password;
    }

    private static SocketThread socketThread;
    public static SocketThread getSocketThread() {
        return socketThread;
    }

    public static void main(String[] args) {
        System.out.println(helpInfo);

        password = getPasswordInConfig();
        port = getPortInConfig();
        boolean newPasswordFlag = false;
        if (port == -1) {
            setPortInConfig(25550);
            port = 25550;
            System.out.println("Set port to 25550 (By default).");
        }
        if (password.isEmpty()) {
            System.out.println("Please input a new password: ");
            newPasswordFlag = true;
        } else {
            EncryptUtils.getInstance().init(password);
        }

        socketThread = new SocketThread(port);
        socketThread.start();

        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String s = input.nextLine();
            if (s.equals("exit")) break;
            if (newPasswordFlag) {
                newPasswordFlag = false;
                setPasswordInConfig(s);
                password = s;
                EncryptUtils.getInstance().init(password);
                System.out.println("Done. " + s);
            } else if (s.startsWith("password ")) {
                password = s.substring(9);
                setPasswordInConfig(password);
                EncryptUtils.getInstance().init(password);
                System.out.println("Done. " + password);
            } else if (s.startsWith("port ")) {
                try {
                    int portTemp = Integer.parseInt(s.substring(5));
                    if (portTemp < 0) throw new UnsupportedOperationException("port must bigger than 0");
                    setPortInConfig(portTemp);
                    port = portTemp;
                    System.out.println("Done. " + port);
                } catch (NumberFormatException e) {
                    System.out.println("Input Number Error");
                }
            } else if (s.equals("reboot")) {
                if (socketThread != null) {
                    socketThread.setShouldStop();
                    while(socketThread.isRunning());
                }
                socketThread = new SocketThread(port);
                socketThread.start();
            } else {
                System.out.println("Input Error");
            }
        }
        input.close();
        socketThread.setShouldStop();
    }
}
