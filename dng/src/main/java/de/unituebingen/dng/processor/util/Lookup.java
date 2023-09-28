package de.unituebingen.dng.processor.util;

public interface Lookup<T, E> {

    E lookup(T value);
}
