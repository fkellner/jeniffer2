package de.unituebingen.jeniffer2.main.util.informationdialog;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.HashMap;
import java.util.Map;

public class InformationDialogHelper {

    public static void openErrorDialog(String titleKey, String messageKey) {
        Platform.runLater(() -> {
            Map<String, String> context = new HashMap<>();
            context.put("titleKey", titleKey);
            context.put("messageKey", messageKey);
            context.put("type", "error");
            InformationDialogView informationDialogView = new InformationDialogView(context::get);
            DialogPane dialog = (DialogPane) informationDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait();
        });
    }

    public static void openInfoDialog(String titleKey, String messageKey) {
        Platform.runLater(() -> {
            Map<String, String> context = new HashMap<>();
            context.put("titleKey", titleKey);
            context.put("messageKey", messageKey);
            context.put("type", "info");
            InformationDialogView informationDialogView = new InformationDialogView(context::get);
            DialogPane dialog = (DialogPane) informationDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait();
        });
    }
}
