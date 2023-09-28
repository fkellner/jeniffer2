package de.unituebingen.dng.processor;

public interface Processor<T> {

    T process(T samples);
}
