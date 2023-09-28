package de.unituebingen.jeniffer2.main.modules.navigation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PathSaver {

    static String latestFilePath;
    static String savedFilePath;

    static boolean folderSaveExists;


    public PathSaver() {

        //loads savedFilePath from file

        try {
            FileInputStream fis = new FileInputStream(".folderSave");
            ObjectInputStream ois = new ObjectInputStream(fis);

            savedFilePath = (String) ois.readObject();

            ois.close();
            fis.close();

            folderSaveExists = true;

        } catch (IOException ioe) {
            //ioe.printStackTrace();
            System.out.println("Could not find '.folderSave'. The file will be written after opening a folder in JENIFFERs folder-browser and then closing JENIFFER.");
            folderSaveExists = false;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }


        //a ShutdownHook gets executed before the program is shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                //saves latestFilePath String to folderSave file
                if (latestFilePath != null) {
                    try {
                        //makes the file visible on Windows Systems (FileOutputStream would otherwise throw exception)
                        if (System.getProperty("os.name").toLowerCase().contains("win") && folderSaveExists == true) {
                            Path file = Paths.get(".folderSave");
                            Files.setAttribute(file, "dos:hidden", false);
                        }

                        FileOutputStream fos = new FileOutputStream(".folderSave");
                        ObjectOutputStream oos = new ObjectOutputStream(fos);

                        oos.writeObject(latestFilePath);

                        oos.close();
                        fos.close();

                        //makes the file hidden on Windows Systems
                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            Path file = Paths.get(".folderSave");
                            Files.setAttribute(file, "dos:hidden", true);
                        }

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });

    }

    public static void setLatestFilePath(String path) {
        latestFilePath = path;
        latestFilePath = latestFilePath.concat(File.separator);
    }

    public static String getSavedFilePath() {
        return savedFilePath;
    }


}

