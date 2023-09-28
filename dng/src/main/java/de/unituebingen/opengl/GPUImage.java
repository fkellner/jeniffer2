package de.unituebingen.opengl;

import java.awt.image.*;

import de.unituebingen.dng.processor.log.Timer;

public class GPUImage implements TransformableOnGPU {
    /**
     * textures that store this image on the GPU
     */
    private OpenGLTexture[] pTextures;

    private Timer pTimer;

    protected GPUImage(Timer timer, OpenGLTexture[] textures) {
        this.pTimer = timer;
        this.pTextures = textures;
    }

    protected OpenGLTexture[] getTextures() {
        return pTextures;
    }

    /**
     * Transform the image on the GPU using a fragment shader
     * @param fragmentShaderSource needs to use GLSL3.0 and manipulates single pixels only
     */
    public void applyShaderInPlace(String fragmentShaderSource) {
        applyShaderInPlace(fragmentShaderSource, false);
    }

    /**
     * Transform the image on the GPU using a fragment shader
     * @param fragmentShaderSource needs to use GLSL3.0
     * @param switchToRGB whether the shader transforms a monochrome (only red channel) image to an RGB image
     */
    public void applyShaderInPlace(String fragmentShaderSource, boolean switchToRGB) {
        for(int i = 0; i < pTextures.length; i++) {
            pTextures[i].applyShaderInPlace(fragmentShaderSource, switchToRGB);
        }
    }

    public GPUImage applyShader(String fragmentShaderSource) {
        return applyShader(fragmentShaderSource, false);
    }

    public GPUImage applyShader(String fragmentShaderSource, boolean switchToRGB) {
        OpenGLTexture[] newTex = new OpenGLTexture[pTextures.length];
        for(int i = 0; i < pTextures.length; i++) {
            newTex[i] = pTextures[i].applyShader(fragmentShaderSource, switchToRGB);
        }
        return new GPUImage(pTimer, newTex);
    }

    public GPUImage applyShader(String fragmentShaderSource, boolean switchToRGB, TransformableOnGPU t2, String tex2Name, TransformableOnGPU t3, String tex3Name) {
        // more general type needed for interface to work
        GPUImage img2 = (GPUImage) t2;
        GPUImage img3 = (GPUImage) t3;
        OpenGLTexture[] tex2;
        if(img2 != null) {
            tex2 = img2.getTextures();
        } else {
            tex2 = new OpenGLTexture[pTextures.length];
            for(int i = 0; i < pTextures.length; i++) {
                tex2[i] = null;
            }
        }
        OpenGLTexture[] tex3;
        if(img3!= null) {
            tex3 = img3.getTextures();
        } else {
            tex3 = new OpenGLTexture[pTextures.length];
            for(int i = 0; i < pTextures.length; i++) {
                tex3[i] = null;
            }
        }
        OpenGLTexture[] newTex = new OpenGLTexture[pTextures.length];
        for(int i = 0; i < pTextures.length; i++) {
            newTex[i] = pTextures[i].applyShader(fragmentShaderSource, switchToRGB, tex2[i], tex2Name, tex3[i], tex3Name);
        }
        return new GPUImage(pTimer, newTex);
    }

    /**
     * Download image data from GPU back to CPU memory
     * @param img the target BufferedImage, should use a DataBufferUShort in its WritableRaster
     */
    public void downloadTo(BufferedImage img) {
        String taskName = "Downloading Image";
        pTimer.startTask(taskName, "## Downloading Image from " + pTextures.length + " OpenGL Textures");
        for(int i = 0; i < pTextures.length; i++) {
            pTextures[i].downloadTo(img);
        }
        pTimer.endTask(taskName);
    }

    /**
     * Displays each texture holding a part of the GPUImage in its own window, 
     * one after the other
     */
    public void displayTextures() {
        for (int i = 0; i < pTextures.length; i++) {
            pTextures[i].display();
        }
    }

    /**
     * free up ressources
     */
    public void delete() {
        for (int i = 0; i < pTextures.length; i++) {
            pTextures[i].delete();
        }
    }
}
