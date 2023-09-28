package de.unituebingen.dng.processor.log;

import java.util.HashMap;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;

import oshi.util.FormatUtil;

import de.unituebingen.opengl.GraphicsCardSpecs;
import de.unituebingen.opengl.OpenGLContext;

public class ConsoleLogger implements Timer {
    // singleton
    private static ConsoleLogger consoleLogger;

    private ConsoleLogger() {
        System.out.println("#### Starting Console logger");
        // ### display hardware information
        // RAM
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        GlobalMemory globalMemory = hardware.getMemory();
        System.out.println("Total memory: " + FormatUtil.formatBytes(globalMemory.getTotal()));
        // processor
        CentralProcessor processor = hardware.getProcessor();

        CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

        System.out.println("Processor Vendor: " + processorIdentifier.getVendor());
        System.out.println("Processor Name: " + processorIdentifier.getName());
        System.out.println("Processor ID: " + processorIdentifier.getProcessorID());
        System.out.println("Identifier: " + processorIdentifier.getIdentifier());
        System.out.println("Microarchitecture: " + processorIdentifier.getMicroarchitecture());
        System.out.println("Frequency (Hz): " + processorIdentifier.getVendorFreq());
        System.out.println("Frequency (GHz): " + processorIdentifier.getVendorFreq() / 1000000000.0);
        System.out.println("Number of physical packages: " + processor.getPhysicalPackageCount());
        System.out.println("Number of physical CPUs: " + processor.getPhysicalProcessorCount());
        System.out.println("Number of logical CPUs: " + processor.getLogicalProcessorCount());

        // Graphics Card
        try {
            OpenGLContext ctx = new OpenGLContext(new NopLogger());
            GraphicsCardSpecs cardSpecs = ctx.getSpecs();
            ctx.delete();
            System.out.println(cardSpecs);
        } catch (IllegalStateException e) {
            System.out.println("Error getting graphics Card information:");
            System.out.println(e.getMessage());
        }
        
        
    }

    public static ConsoleLogger getInstance() {
        if (consoleLogger == null)
            consoleLogger = new ConsoleLogger();
        return consoleLogger;
    }

    private String pRun = "";

    private HashMap<String, Task> pTasks = new HashMap<String, Task>();

    public void startRun(String filename, int width, int length, String accStr) {
        // reset everything
        pRun = System.currentTimeMillis() + filename;
        pTasks = new HashMap<String, Task>();
        // start a task to track current run
        startTask("Total", "");
        System.out.println("\n### Starting Run: " + pRun);
        System.out.println("Image width: " + width + ", length: " + length);
        System.out.println("Acceleration Strategy: " + accStr);
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
        long duration = end - t.start();
        System.out.println(duration + "ms " + t.name() + " " + t.description());
    }

}
