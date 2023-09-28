package de.unituebingen.jeniffer2.main.modules.navigation;


import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.Comparator;


public class NavigationTreeItem extends TreeItem<String> {

    private static final FontAwesomeIcon FOLDER_COLLAPSE_IMAGE = FontAwesomeIcon.FOLDER;
    private static final FontAwesomeIcon FOLDER_EXPAND_IMAGE = FontAwesomeIcon.FOLDER_OPEN;
    private static final FontAwesomeIcon FILE_IMAGE = FontAwesomeIcon.FILE_IMAGE_ALT;
    private final File file;
    String savedFilePath;
    NavigationPresenter n = new NavigationPresenter();
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;


    public NavigationTreeItem(String f, File file) {
        super(f);
        this.file = file;


        savedFilePath = PathSaver.getSavedFilePath();


        if (file.isDirectory()) {
            FontAwesomeIconView icon = new FontAwesomeIconView(FOLDER_COLLAPSE_IMAGE);
            icon.setStyleClass("folder-collapse-icon");
            this.setGraphic(icon);


            //checks if node should be expanded, because it was expanded before closing last time
            //backslash or slash (File.separator) is added to end of filepaths to prevent opening of wrong folders
            //example: if folder "unituebingen" was opened without backslashes or slash folder "uni" would also be opened
            if (savedFilePath != null) {
                if (file.toString().endsWith(File.separator)) {
                    if (savedFilePath.contains(file.toString())) {
                        this.setExpanded(true);
                    }
                } else if (savedFilePath.contains(file.toString().concat(File.separator))) {
                    this.setExpanded(true);
                }
            }

        } else {
            FontAwesomeIconView icon = new FontAwesomeIconView(FILE_IMAGE);
            icon.setStyleClass("file-icon");
            this.setGraphic(icon);
        }

        this.addEventHandler(TreeItem.branchExpandedEvent(), (EventHandler) e -> {
            NavigationTreeItem source = (NavigationTreeItem) e.getSource();
            FontAwesomeIconView icon = new FontAwesomeIconView(FOLDER_EXPAND_IMAGE);
            icon.setStyleClass("folder-icon");
            source.setGraphic(icon);


            PathSaver.setLatestFilePath(source.getFile().toString());


        });

        this.addEventHandler(TreeItem.branchCollapsedEvent(), (EventHandler) e -> {
            NavigationTreeItem source = (NavigationTreeItem) e.getSource();
            FontAwesomeIconView icon = new FontAwesomeIconView(FOLDER_COLLAPSE_IMAGE);
            icon.setStyleClass("folder-collapse-icon");
            source.setGraphic(icon);
        });


    }

    public File getFile() {
        return file;
    }

    @Override
    public ObservableList<TreeItem<String>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            File f = getFile();
            isLeaf = f.isFile();
        }

        return isLeaf;
    }

    private ObservableList<TreeItem<String>> buildChildren(TreeItem<String> treeItem) {
        File f = ((NavigationTreeItem) treeItem).getFile();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
                for (File childFile : files) {
                    if ((childFile.isFile() && childFile.getName().toLowerCase().endsWith(".dng"))
                            || childFile.isDirectory()) {
                        children.add(new NavigationTreeItem(childFile.getName(), childFile));
                    }
                }

                Comparator<TreeItem<String>> comparator = Comparator.comparing(o -> ((NavigationTreeItem) o).getFile().getName());
                FXCollections.sort(children, comparator);


                return children;
            }
        }

        return FXCollections.emptyObservableList();
    }
}
