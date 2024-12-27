package ru.brk.demo1;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;



public class ProcessController {
    @FXML
    private TextField fileNameField;

    @FXML
    private Button saveButton;

    @FXML
    private Button backButton;

    private static List<List<Double>> nodeInfo;

    // Регулярное выражение для проверки допустимых символов в имени файла
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @FXML
    void initialize(){
        backButton.setOnAction(actionEvent -> {
            SceneSwitcher.openAnotherScene(backButton,"hello-view.fxml");
        });
        saveButton.setOnAction(actionEvent -> {

            String fileName = fileNameField.getText();

            if (!isValidFileName(fileName)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Предупреждение");
                alert.setHeaderText(null);
                alert.setContentText("Недопустимое имя файла. Используйте только буквы, цифры, точки, дефисы и подчеркивания.");
                alert.showAndWait();
                return; // Выход из метода, если условие не выполнено
            }
            File constructionFile = new File("src/main/resources/" + fileNameField.getText() + ".cn");
            File loadsFile = new File("src/main/resources/" + fileNameField.getText() + ".ld");
            File resultFile = new File("src/main/resources/" + fileNameField.getText() + ".md");

            try {
                constructionFile.createNewFile();
                loadsFile.createNewFile();
                resultFile.createNewFile();

                FileWriterController.writeToFile(constructionFile);
                FileWriterController.writeToFile(loadsFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }
    // Метод для проверки допустимости имени файла
    private boolean isValidFileName(String fileName) {
        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }
}
