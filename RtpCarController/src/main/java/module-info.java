module rtpcarcontroller {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;

    opens edu.ntnu.rtpcarcontroller.controller.view to javafx.fxml;
    exports edu.ntnu.rtpcarcontroller;
}