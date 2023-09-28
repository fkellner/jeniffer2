package de.unituebingen.dng.processor.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;

import de.unituebingen.opengl.GraphicsCardSpecs;
import de.unituebingen.opengl.OpenGLContext;

public class CsvLogger implements Timer {
    // singleton
    private static CsvLogger logger;

    private CsvLogger() {
        System.out.println("#### Starting CSV logger");
        File logDirectory = new File("jeniffer2-logs");
        if (!logDirectory.exists()) {
            logDirectory.mkdir();
        }
        System.out.println("Logs can be found at " + logDirectory.getAbsolutePath());
        File systemInfoLog = new File(logDirectory.getPath(), "system-info.csv");
        File timingLog = new File(logDirectory.getPath(), "timing.csv");
        try {
            if (!systemInfoLog.exists())
                systemInfoLog.createNewFile();
            if (!timingLog.exists())
                timingLog.createNewFile();
            pTimingWriter = new FileWriter(timingLog, true);
            if(timingLog.length() == 0)
                pTimingWriter.append(TimeMeasurement.getHeader() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // instantiating hardware info wrappers
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        GlobalMemory globalMemory = hardware.getMemory();
        CentralProcessor processor = hardware.getProcessor();
        CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
        ComputerSystem computerSystem = hardware.getComputerSystem();

        GraphicsCardSpecs cardSpecs;
        String gpuError = "none";
        // Graphics Card
        try {
            OpenGLContext ctx = new OpenGLContext(new NopLogger());
            cardSpecs = ctx.getSpecs();
            ctx.delete();
            // System.out.println(cardSpecs);
        } catch (IllegalStateException e) {
            gpuError = e.getMessage();
            cardSpecs = new GraphicsCardSpecs(
                    "unknown", "unknown", "unknown",
                    0, 0,
                    0, 0);
        }
        pSystemId = computerSystem.getHardwareUUID();
        if (pSystemId == "unknown")
            pSystemId = processorIdentifier.getProcessorID();

        SystemInformation info = new SystemInformation(
            pSystemId,
            processorIdentifier.getVendor(),
            processorIdentifier.getName(),
            processorIdentifier.getMicroarchitecture(),
            processorIdentifier.getVendorFreq(),
            processor.getPhysicalProcessorCount(),
            processor.getLogicalProcessorCount(),
            globalMemory.getTotal(),
            cardSpecs.vendor(),
            cardSpecs.renderer(),
            cardSpecs.openGLVersion(),
            cardSpecs.maxTextureSize(),
            cardSpecs.maxFragmentUniformComponents(),
            cardSpecs.maxViewportWidth(),
            cardSpecs.maxViewportHeight(),
            gpuError
        );
        try {
            FileWriter systemInfoWriter = new FileWriter(systemInfoLog, true);
            if (systemInfoLog.length() == 0)
                systemInfoWriter.append(SystemInformation.getHeader() + "\n");
            systemInfoWriter.append(info.getData() + "\n");
            systemInfoWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }    
    }

    public static CsvLogger getInstance() {
        if (logger == null)
            logger = new CsvLogger();
        return logger;
    }

    private FileWriter pTimingWriter;
    
    private String pSystemId;

    private String pFile = "";
    private int pWidth = 0;
    private int pLength = 0;
    private long pRunStart = System.currentTimeMillis();
    private String pAccelerationStrategy = "";

    private HashMap<String, Task> pTasks = new HashMap<String, Task>();

    public void startRun(String filename, int width, int length, String accStr) {
        // reset everything
        pFile = filename;
        pWidth = width;
        pLength = length;
        pAccelerationStrategy = accStr;
        pRunStart = System.currentTimeMillis();
        pTasks = new HashMap<String, Task>();
        // start a task to track current run
        startTask("Total", "");
        System.out.println("\n### Starting Run: " + pFile);
    }

    public void endRun() {
        endTask("Total");
        // clean up
        String[] keys = new String[pTasks.keySet().size()];
        int kPos = 0;
        for (String k : pTasks.keySet()) {
            keys[kPos++] = k;
        }
        for (int i = 0; i < keys.length; i++) {
            endTask(keys[i]);
        }
    }

    public void startTask(String name, String desc) {
        Task t;
        if (pTasks.get(name) != null) {
            endTask(name);
            System.out.println("!! Restarting task " + name);
            t = new Task(name, desc + "(!!Restarted!!)", System.currentTimeMillis());
        } else {
            t = new Task(name, desc, System.currentTimeMillis());
        }
        pTasks.put(name, t);
    }

    public void endTask(String name) {
        long end = System.currentTimeMillis();
        Task t = pTasks.remove(name);
        if (t == null) {
            System.out.println("!! task not started: " + name);
            return;
        }
        TimeMeasurement m = new TimeMeasurement(
            pSystemId,
            pRunStart,
            pFile,
            pWidth,
            pLength,
            pAccelerationStrategy,
            t.name(),
            t.description(),
            t.start(),
            end,
            end - t.start()
        );
        try {
            if (pTimingWriter == null) {
                throw new IllegalStateException("Timing log writer not initialized");
            }
            pTimingWriter.append(m.getData() + "\n");
            pTimingWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
