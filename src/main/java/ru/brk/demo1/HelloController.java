package ru.brk.demo1;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class HelloController {

    @FXML
    private Button preProcessButton;

    @FXML
    private Button processButton;

    @FXML
    private Button postButton;

    @FXML
    void initialize(){
        preProcessButton.setOnAction(actionEvent -> SceneSwitcher.openAnotherScene(preProcessButton,"preProcess.fxml"));
        processButton.setOnAction(actionEvent -> {SceneSwitcher.openAnotherScene(processButton,"process.fxml");});
        postButton.setOnAction(actionEvent -> {SceneSwitcher.openAnotherScene(postButton, "postProcess.fxml");});

    }

}