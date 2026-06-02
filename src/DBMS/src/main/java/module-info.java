module dbms {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires jdk.jsobject;
    requires java.desktop;

    opens dbms to javafx.graphics, javafx.fxml;
    opens dbms.models to com.fasterxml.jackson.databind;
    
    exports dbms;
    exports dbms.services;
    exports dbms.models;
    exports dbms.repository;
}
