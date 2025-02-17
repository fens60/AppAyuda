module com.example.appayuda {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jsobject;
    requires javafx.web;

    opens com.example.appayuda to javafx.fxml;
    exports com.example.appayuda;
}