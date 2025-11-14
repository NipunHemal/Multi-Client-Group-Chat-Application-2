package lk.ijse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.Scanner;

public class ClientController implements Initializable {
    public Button sendButton;
    public TextField messageField;
    public VBox messageContainer;

    Scanner scanner = new Scanner(System.in);
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;
    Socket socket;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try{
            // Connect to the server on localhost at port 3000
            socket =new Socket("localhost",3000);
            System.out.println("Connected to server: " + socket.getInetAddress().getHostAddress());

            // Get the output and input streams from the socket
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            // Start a new thread to listen for messages from the server
            new Thread(() -> {
                try {
                    while (true) {
                        // Receive and print the server's message
                        String message = dataInputStream.readUTF();
                        System.out.println("Client :"+message);

                        // reserve for images
                        if (message.equals("IMAGE")) {
                            int imageSize = dataInputStream.readInt();
                            byte[] imageBytes = new byte[imageSize];
                            dataInputStream.readFully(imageBytes);
                            Platform.runLater(() -> {
                                // This code block will now be executed on the correct thread.
                                loadImage(imageBytes,false);
                            });
                        } else {
                            Platform.runLater(() -> {
                                // This code block will now be executed on the correct thread.
                                loadMessage(message, false);
                            });
                        }

                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void btnOnfileUpload(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);
        java.io.File file = fileChooser.showOpenDialog(messageField.getScene().getWindow());
        System.out.println(file.toPath());

        if (file != null) {
            try {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                dataOutputStream.writeUTF("IMAGE");
                dataOutputStream.writeInt(imageBytes.length);
                dataOutputStream.write(imageBytes);
                dataOutputStream.flush();
                loadImage(imageBytes, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void btnSendOnAction(ActionEvent actionEvent) {
        try {
            // Send a message to the server
            String message = messageField.getText();
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush(); // Flush the stream to ensure all data is sent
            loadMessage(message, true);

            // Clear the message field
            messageField.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadMessage(String text, boolean isSent) {
        System.out.println(text);
        // execute cmd code to run a cmd command


//        // Create the main container for the message row
        HBox messageRow = new HBox(10); // 10 is the spacing between elements

        // Create the label for the message bubble
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true); // Ensures long messages wrap to the next line
        messageLabel.getStyleClass().add("message-bubble");


        if (isSent) {
            // Style for a "sent" message (blue bubble on the right)
            messageLabel.getStyleClass().add("sent");
            messageRow.setAlignment(Pos.TOP_RIGHT);
            messageRow.getChildren().addAll(messageLabel);
        } else {
            // Style for a "received" message (white bubble on the left)
            messageLabel.getStyleClass().add("received");
            messageRow.setAlignment(Pos.TOP_LEFT);
            messageRow.getChildren().addAll(messageLabel);
        }

        // Add the completed message row to the main container
        messageContainer.getChildren().add(messageRow);
    }

    public void loadImage(byte[] imageBytes , boolean isSent) {
        // Create the main container for the message row
        HBox messageRow = new HBox(10); // 10 is the spacing between elements

        ImageView imageView = new ImageView();
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        imageView.setImage(image);
        imageView.setImage(image);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        if (isSent) {
            // Style for a "sent" message (blue bubble on the right)
            imageView.getStyleClass().add("sent");
            messageRow.setAlignment(Pos.TOP_RIGHT);
            messageRow.getChildren().addAll(imageView);
        } else {
            // Style for a "received" message (white bubble on the left)
            imageView.getStyleClass().add("received");
            messageRow.setAlignment(Pos.TOP_LEFT);
            messageRow.getChildren().addAll(imageView);
        }

        messageContainer.getChildren().add(messageRow);
    }
}
