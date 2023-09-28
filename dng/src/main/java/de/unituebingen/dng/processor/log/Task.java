package de.unituebingen.dng.processor.log;

public record Task(
    String name,
    String description,
    long start
) {}
