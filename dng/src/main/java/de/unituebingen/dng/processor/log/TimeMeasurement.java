package de.unituebingen.dng.processor.log;

public record TimeMeasurement(
    String systemId,
    long startOfRun,
    String fileName,
    int imageWidth,
    int imageLength,
    String accelerationStrategy,
    String task,
    String description,
    long startOfTask,
    long endOfTask,
    long taskDuration
) {
    public static String getHeader() {
        return "systemId,startOfRun,fileName,imageWidth,imageLength,"
                + "accelerationStrategy,task,"
                + "description,startOfTask,endOfTask,taskDuration";
    }

    public String getData() {
        return "\"" + systemId + "\"," + startOfRun + ",\"" + fileName + "\","
                + imageWidth + "," + imageLength + ",\"" + accelerationStrategy + "\",\""
                + task + "\",\""
                + description + "\"," + startOfTask + "," + endOfTask + ","
                + taskDuration;
    }
    
    public void printShort() {
        System.out.println(taskDuration + "ms (" + task + (description == "" ? "" : ", " + description ) + ")");
    }
}
