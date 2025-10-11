package com.pbl4.cameraclient.ui.controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    // Annotation @FXML dùng để liên kết các biến này với các thành phần
    // có fx:id tương ứng trong file LoginView.fxml.
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label statusLabel;

    // Phương thức này sẽ được gọi khi người dùng nhấn nút Login.
    @FXML
    void handleLoginButtonAction(ActionEvent event) {
        System.out.println("Login button clicked!");
        String username = usernameField.getText();
        String password = passwordField.getText();

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        
        statusLabel.setText("Attempting to log in...");
    }
}