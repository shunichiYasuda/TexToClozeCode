module com.genSci.tools.TexToClozeCode {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.genSci.tools.TexToClozeCode to javafx.fxml;
    exports com.genSci.tools.TexToClozeCode;
}