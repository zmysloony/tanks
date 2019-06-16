module test1 {
	requires transitive javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	requires javafx.media;
	requires javafx.swing;
	requires javafx.web;
	requires java.desktop;
	requires java.sql;
	requires org.apache.logging.log4j;
	requires junit;
	exports main;
	opens main to javafx.graphics, javafx.fxml;
}