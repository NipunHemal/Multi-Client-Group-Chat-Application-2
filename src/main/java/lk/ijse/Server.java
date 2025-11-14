package lk.ijse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Server {

    // Store all connected clients' output streams
    private static final Set<DataOutputStream> clientOutputs = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(3000)) {
            System.out.println("Group Chat Server started on port 3000...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                synchronized (clientOutputs) {
                    clientOutputs.add(dos);
                }

                // Handle each client in a separate thread
                new Thread(new ClientHandler(socket, dos)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast text or image to all connected clients
    public static void broadcast(String message, byte[] imageBytes,DataOutputStream dosSender) {
        synchronized (clientOutputs) {
            for (DataOutputStream dos : clientOutputs) {
                if (dos == dosSender) continue;

                try {
                    if (message != null) {
                        dos.writeUTF(message);
                    } else if (imageBytes != null) {
                        dos.writeUTF("IMAGE");
                        dos.writeInt(imageBytes.length);
                        dos.write(imageBytes);
                    }
                    dos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Inner class to handle each connected client
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public ClientHandler(Socket socket, DataOutputStream dos) throws IOException {
            this.socket = socket;
            this.dos = dos;
            this.dis = new DataInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = dis.readUTF();

                    if (message.equals("IMAGE")) {
                        int size = dis.readInt();
                        byte[] imageBytes = new byte[size];
                        dis.readFully(imageBytes);

                        System.out.println("Image received from client, broadcasting...");
                        Server.broadcast(null, imageBytes,dos);
                    } else {
                        System.out.println("Message received: " + message);
                        Server.broadcast(message, null,dos);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                try {
                    dis.close();
                    dos.close();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                synchronized (clientOutputs) {
                    clientOutputs.remove(dos);
                }
            }
        }
    }
}
