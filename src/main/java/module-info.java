module com.amremad719.mosaicify {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires opencv;

    opens com.amremad719.mosaicify to javafx.fxml;
    exports com.amremad719.mosaicify;
}