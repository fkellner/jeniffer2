package de.unituebingen.dng.processor.log;

import oshi.util.FormatUtil;

public record SystemInformation(
    String systemId,
    String cpuVendor,
    String cpuName,
    String cpuMicroarchitecture,
    long cpuFrequency,
    int cpuPhysicalCores,
    int cpuLogicalCores,
    long totalRam,
    String gpuVendor,
    String gpuRenderer,
    String openGLVersion,
    int maxTextureSize,
    int maxFragmentUniformComponents,
    int maxViewportWidth,
    int maxViewportHeight,
    String gpuError    
) {
    public static String getHeader() {
        return "systemId, cpuVendor,cpuName,cpuMicroarchitecture,cpuFrequency,"
                + "cpuPhysicalCores,cpuLogicalCores,totalRam,gpuVendor,gpuRenderer,openGLVersion,"
                + "maxTextureSize,maxFragmentUniformComponents,maxViewportWidth,maxViewportHeight,"
                + "gpuError";
    }
    
    public String getData() {
        return "\"" + systemId + "\",\"" + cpuVendor + "\",\"" + cpuName + "\",\""
                + cpuMicroarchitecture + "\","
                + cpuFrequency + "," + cpuPhysicalCores + "," + cpuLogicalCores + "," + totalRam + ",\""
                + gpuVendor + "\",\"" + gpuRenderer + "\",\"" + openGLVersion + "\","
                + maxTextureSize + "," + maxFragmentUniformComponents + ","
                + maxViewportWidth + "," + maxViewportHeight + ",\"" + gpuError + "\"";
    }
    
    public void print() {
        System.out.println("systemId: " + systemId);
        System.out.println("cpuVendor: " + cpuVendor);
        System.out.println("cpuName: " + cpuName);
        System.out.println("cpuMicroarchitecture: " + cpuMicroarchitecture);
        System.out.println("cpuFrequency (GHz): " + cpuFrequency / 1000000000.0);
        System.out.println("cpuPhysicalCores: " + cpuPhysicalCores);
        System.out.println("cpuLogicalCores: " + cpuLogicalCores);
        System.out.println("totalRam: " + FormatUtil.formatBytes(totalRam));
        System.out.println("gpuVendor: " + gpuVendor);
        System.out.println("gpuRenderer: " + gpuRenderer);
        System.out.println("openGLVersion: " + openGLVersion);
        System.out.println("maxTextureSize: " + maxTextureSize);
        System.out.println("maxFragmentUniformComponents: " + maxFragmentUniformComponents);
        System.out.println("maxViewportWidth: " + maxViewportWidth);
        System.out.println("maxViewportHeight: " + maxViewportHeight);
        System.out.println("gpuError: " + gpuError);
    }
}
