package de.unituebingen.opengl;

import java.awt.image.BufferedImage;

public interface TransformableOnGPU {
  /**
   * Applies a GLSL fragment shader to the wrapped image data and discards the old image data.
   * @param fragmentShader the program code for the fragment shader - see {@link FragmentShaderExamples} for examples
   */
  void applyShaderInPlace(String fragmentShader);

  /**
   * Applies a GLSL fragment shader to the wrapped image data and discards the old image data.
   * @param fragmentShader the program code for the fragment shader - see {@link FragmentShaderExamples} for examples
   * @param switchToRGB whether to allocate RGB data as target, if the source data are monochrome
   */
  void applyShaderInPlace(String fragmentShader, boolean switchToRGB);

  /**
   * Applies a GLSL fragment shader to the wrapped image data without modifying them.
   * @param fragmentShader the program code for the fragment shader - see {@link FragmentShaderExamples} for examples
   * @return an object of the same type wrapping the result
   */
  TransformableOnGPU applyShader(String fragmentShader);

  /**
   * Applies a GLSL fragment shader to the wrapped image data without modifying them.
   * @param fragmentShader the program code for the fragment shader - see {@link FragmentShaderExamples} for examples
   * @param switchToRGB whether to allocate RGB data as target, if the source data are monochrome
   * @return an object of the same type wrapping the result
   */
  TransformableOnGPU applyShader(String fragmentShader, boolean switchToRGB);

  /**
   * Applies a GLSL fragment shader to the wrapped image data without modifying them.
   * @param fragmentShader the program code for the fragment shader - see {@link FragmentShaderExamples} for examples
   * @param switchToRGB whether to allocate RGB data as target, if the source data are monochrome
   * @param t2 second wrapper object of the same type containing image data to be used by the shader. Set null if unused.
   * @param t2Name name of the {@code uniform sampler2D} variable in the shader to which the data are mapped. Set null if unused.
   * @param t3 third wrapper object of the same type containing image data to be used by the shader. Set null if unused.
   * @param t3Name name of the {@code uniform sampler2D} variable in the shader to which the data are mapped. Set null if unused. 
   * @return an object of the same type wrapping the result
   */
  // the type of t2 and t3 needs to be the same as the object on which it is called
  TransformableOnGPU applyShader(String fragmentShader, boolean switchToRGB, TransformableOnGPU t2, String t2Name, TransformableOnGPU t3, String t3Name);
  
  /**
   * Write image data to a BufferedImage. of the same size (and color mode)
   * @param target BufferedImage of the same size (and color mode)
   */
  public void downloadTo(BufferedImage target);

  /**
   * Free up OpenGL resources storing image data wrapped by this object.
   */
  void delete();
}
