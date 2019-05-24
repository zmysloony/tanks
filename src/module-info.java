module test1 {
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.media;
	requires javafx.swing;
	requires javafx.web;
	requires java.desktop;
	requires java.sql;
	exports test1;
	opens test1 to javafx.graphics, javafx.fxml;
}