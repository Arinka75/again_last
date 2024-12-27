module ru.brk.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
//    requires javafx.web;
//
//    requires org.controlsfx.controls;
//    requires com.dlsc.formsfx;
//    requires org.kordamp.ikonli.javafx;
//    requires com.almasb.fxgl.all;


    opens ru.brk.demo1 to javafx.fxml;
    exports ru.brk.demo1;
}