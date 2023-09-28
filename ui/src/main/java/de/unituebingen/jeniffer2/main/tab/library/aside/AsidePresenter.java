package de.unituebingen.jeniffer2.main.tab.library.aside;

import de.unituebingen.jeniffer2.main.modules.metadata.MetadataView;
import de.unituebingen.jeniffer2.main.modules.navigation.NavigationView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AsidePresenter implements Initializable {

    @FXML
    private SplitPane content;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            NavigationView navigationView = new NavigationView();
            content.getItems().add(navigationView.getView());
            MetadataView metadataView = new MetadataView();
            metadataView.getViewAsync(content.getItems()::add);
        } catch (Exception e) {
            // catch-all to get stacktrace
            System.out.println(e);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // show error message
            InformationDialogHelper.openErrorDialog(
                "error.title.dngprocessing", "error.message.dngprocessing");
        }
    }


}
