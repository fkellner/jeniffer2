package de.unituebingen.opengl;

public record GraphicsCardSpecs(
    String vendor,
    String renderer,
    String openGLVersion,
    int maxTextureSize,
    int maxFragmentUniformComponents,
    int maxViewportWidth,
    int maxViewportHeight
) {}
