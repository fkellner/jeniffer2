package de.unituebingen.jeniffer2.main.modules.navigation;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import javax.inject.Inject;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class NavigationPresenter implements Initializable {

    private static final String HOST_NAME = "Computer";

    @FXML
    private TreeView<String> directories;

    @Inject
    private WorkflowManager workflowManager;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            String hostName = HOST_NAME;

            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                //do nothing on purpose
            }


            Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();
            FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.DESKTOP);
            icon.setStyleClass("desktop-icon");
            TreeItem<String> rootNode = new TreeItem<>(hostName, icon);
            for (Path name : rootDirectories) {
                String path = name.toAbsolutePath().toString();
                NavigationTreeItem treeNode = new NavigationTreeItem(path, new File(path));
                workflowManager.selectedDirectoryProperty().bind(directories.getSelectionModel().selectedItemProperty());
                directories.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                rootNode.getChildren().add(treeNode);
            }

            rootNode.setExpanded(true);
            directories.setRoot(rootNode);
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
