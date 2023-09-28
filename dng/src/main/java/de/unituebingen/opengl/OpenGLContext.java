package de.unituebingen.opengl;

// for constant values
import org.lwjgl.opengl.*;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import de.unituebingen.dng.processor.Pipeline;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.log.Timer;

import static org.lwjgl.system.MemoryUtil.*;

import java.awt.image.*;
import java.lang.Math;


/**
 * Wrapper class to use OpenGL from
 * Designed to work with de.unituebingen.jenniffer2.dng module, so assuming
 * BufferedImage s use DataBufferUShort in their WritableRaster
 */
public class OpenGLContext
{
    /**
     * Invisible window used for obtaining the OpenGL context
     */
    private long pWindow;

    protected long getWindow() {
        return pWindow;
    }

    /**
     * Maximum width of textures that can be created
     * Determined by maximum texture and viewport sizes
     */
    private int pMaxTextureWidth;

    /**
     * Maximum height of textures that can be created
     * Determined by maximum texture and viewport sizes
     */
    private int pMaxTextureHeight;

    /**
     * vidmode of current monitor
     */
    private GLFWVidMode pVidMode;

    private GraphicsCardSpecs pSpecs;

    /**
     * return the GLFWVidmode of the current context, containing information 
     * such as screen width and height
     * @return GLFWVidmode
     */
    protected GLFWVidMode getVidMode() {
      return pVidMode;
    }
    
    protected Timer pTimer;


    /** Creates a new OpenGL Wrapper
      * Checks system capabilities and creates an invisible window to obtain an
      * OpenGL Context
      */
    public OpenGLContext(Timer timer)
    {
      this.pTimer = timer;
      String taskName = "Initializing OpenGL Wrapper";
      pTimer.startTask(taskName, "");

      // Initialize GLFW. Most GLFW functions will not work before doing this.
      GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
      glfwSetErrorCallback(errorCallback);
      if ( !glfwInit() )
          throw new IllegalStateException("Unable to initialize GLFW");
      // Configure our window
      glfwDefaultWindowHints(); // optional, the current window hints are already the default
      glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
      glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // to be able to gracefully close debug windows
      // Create the window - needed for openGL, but we do not need to show it
      int WIDTH = 800;
      int HEIGHT = 800;
      pWindow = glfwCreateWindow(WIDTH, HEIGHT, "Hidden OpenGL Window", NULL, NULL);
      if ( pWindow == NULL )
          throw new IllegalStateException("Failed to create the GLFW window");

      pVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      // initialize context
      glfwMakeContextCurrent(pWindow);
      GL.createCapabilities();

      // put out some debugging info
      String cardVendor = GL11C.glGetString(GL11C.GL_VENDOR);
      String cardRenderer = GL11C.glGetString(GL11C.GL_RENDERER);
      String cardGLVersion =  GL11C.glGetString(GL11C.GL_VERSION);

      // check capabilities
      GLCapabilities caps = GL.getCapabilities();
      if (!caps.OpenGL30) {
          System.out.println("#### Your OpenGL Version is:");
          System.out.println(cardGLVersion);
          if (cardGLVersion.length() > 0 && Integer.parseInt(cardGLVersion.substring(0,1)) >= 3) {
            System.out.println("Although your vendor did not set the capability flag for OpenGL30, GPU functionality might still work since we are using the forward compatible subset of the API.");
          } else {
            System.out.println("If this is higher than OpenGL version 3.0, our code could not tell, but GPU functionality might still work. Otherwise, it will probably not work.");
          }
          //throw new IllegalStateException("OpenGLWrapper requires OpenGL 3.0 or higher.");
      }
      // enable using textures
      GL30C.glEnable(GL30C.GL_TEXTURE_2D);
      // get max texture size
      int maxTextureSize = GL30C.glGetInteger(GL30C.GL_MAX_TEXTURE_SIZE);
      if (!caps.GL_ARB_texture_non_power_of_two) {
        throw new IllegalStateException("Non-power-of-two textures not supported");
      }
      int cardMaxUniforms = GL30C.glGetInteger(GL30C.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS);
      // set viewport to window size
      GL30C.glViewport(0,0,WIDTH,HEIGHT);
      // get maximum viewport size
      float[] maxViewportDims = new float[2];
      GL30C.glGetFloatv(GL30C.GL_MAX_VIEWPORT_DIMS, maxViewportDims);
      int maxViewportWidth = (int) Math.floor(maxViewportDims[0]);
      int maxViewportHeight = (int) Math.floor(maxViewportDims[1]);

      // divide actual max value by two because of better experimental performance
      pMaxTextureHeight = Math.min(maxViewportHeight, maxTextureSize / 2);
      pMaxTextureWidth = Math.min(maxViewportWidth, maxTextureSize / 2);
      try {
        // allow setting texture width/height to be much smaller for testing
        int tileSize = Integer.parseInt(System.getenv("TILE_SIZE"));
        pMaxTextureHeight = Math.min(pMaxTextureHeight, tileSize);
        pMaxTextureWidth = Math.min(pMaxTextureWidth, tileSize);
      } catch(java.lang.NumberFormatException e) {
      }

      pSpecs = new GraphicsCardSpecs(
          cardVendor, cardRenderer, cardGLVersion,
          maxTextureSize, cardMaxUniforms, maxViewportWidth, maxViewportHeight
      );

      pTimer.endTask(taskName);
    }

    /**
     * Free up allocated ressources, i.e. OpenGL Context and Window
     */
    public void delete() {
        long oldContext = glfwGetCurrentContext();
        glfwMakeContextCurrent(pWindow);
        glfwDestroyWindow(pWindow);
        // no call to glfwTerminate: If we are running with GUI, this would destroy its context, too
        if(oldContext != NULL && oldContext != pWindow) {
          System.out.println("!! resetting OpenGL Context");
          glfwMakeContextCurrent(oldContext);
        }
    }

    public GraphicsCardSpecs getSpecs() {
      return pSpecs;
    }
    
    /**
     * Uploads part of an image to the GPU as a texture
     * @param image A BufferedImage with a WritableRaster using a DataBufferUShort.
     */
    private OpenGLTexture uploadImagePart(
      BufferedImage image, int originX, int originY, int width, int height, int overlap
    ) {
        String taskName = "Uploading image part";
        pTimer.startTask(taskName, "activating openGL context");
        // other code may have used other context
        glfwMakeContextCurrent(pWindow);
        GL.createCapabilities();
        pTimer.endTask(taskName);

        pTimer.startTask(taskName, "computing overlaps");
        // compute overlap to each side
        PositionInImage pos = new PositionInImage(
          originX, originY, 0, 0, 0, 0
        );
        final int transferOriginX = (int) Math.max(0, originX - overlap);
        pos.overlapLeft = originX - transferOriginX;
        final int transferOriginY = (int) Math.max(0, originY - overlap);
        pos.overlapTop = originY - transferOriginY;
        final int transferWidth = (int) Math.min(
            image.getWidth() - transferOriginX,
            originX + width + overlap);
        pos.overlapRight = transferWidth - width - pos.overlapLeft;
        final int transferHeight = (int) Math.min(
            image.getHeight() - transferOriginY,
            originY + height + overlap);
        pos.overlapBottom = transferHeight - height - pos.overlapTop;
        pTimer.endTask(taskName);

        pTimer.startTask(taskName, "getting raster and runtime type-checking");
        // extracting data to array - this is what we will upload
        Raster raster = image.getRaster();

        // runtime check data type
        int transferType = raster.getTransferType();
        if(transferType != DataBuffer.TYPE_USHORT) {
          String transferTypeTranslated = "unknown or undefined";
          switch(transferType) {
            case DataBuffer.TYPE_BYTE:
              transferTypeTranslated = "bytes";
              break;
            case DataBuffer.TYPE_SHORT:
              transferTypeTranslated = "shorts";
              break;
            case DataBuffer.TYPE_INT:
              transferTypeTranslated = "ints";
              break;
            case DataBuffer.TYPE_FLOAT:
              transferTypeTranslated = "floats";
              break;
            case DataBuffer.TYPE_DOUBLE:
              transferTypeTranslated = "doubles";
              break;
          }
          throw new IllegalStateException(
            "Trying to upload a BufferedImage with" +
            "an underlying DataBuffer of " + transferTypeTranslated +
            " to a texture expecting unsigned shorts");
        }
        pTimer.endTask(taskName);

        pTimer.startTask(taskName, "copying data into short array");
        // if data type is okay, get pixels
        short[] pixels = (short[]) raster.getDataElements(
          transferOriginX, transferOriginY, transferWidth, transferHeight, null);
        // getting info about data channels
        int channels = raster.getNumDataElements();
        if (channels != 1 && channels != 3) {
          throw new IllegalStateException("Unsupported number of color channels: " + channels);
        }
        boolean isMonochrome = channels == 1;
        pTimer.endTask(taskName);

        pTimer.startTask(taskName, "generating OpenGL Texture");
        //create a texture
        int textureId = GL30C.glGenTextures();

        //bind the texture
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, textureId);

        //tell opengl how to unpack bytes
        GL30C.glPixelStorei(GL30C.GL_UNPACK_ALIGNMENT, 1);

        //set the texture parameters, can be GL_LINEAR or GL_NEAREST
        GL30C.glTexParameterf(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GL30C.glTexParameterf(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);

        // needed by demosaicing operations
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);

        int error = GL30C.glGetError();
        if (error != GL30C.GL_NO_ERROR) {
          throw new IllegalStateException("Error before uploading texture");
        }
        pTimer.endTask(taskName);
        pTimer.startTask(taskName, "filling texture with data");
        //upload texture
        GL30C.glTexImage2D(
          GL30C.GL_TEXTURE_2D,
          0,
          isMonochrome ? GL30C.GL_R32F : GL30C.GL_RGB32F, // need to specify size, default is less bit and lossy
          transferWidth,
          transferHeight,
          0,
          isMonochrome ? GL30C.GL_RED : GL30C.GL_RGB,
          GL30C.GL_UNSIGNED_SHORT,
          pixels
        );
        pTimer.endTask(taskName);
        pTimer.startTask(taskName, "validating upload");
        error = GL30C.glGetError();
        if (error != GL30C.GL_NO_ERROR) {
          String translatedError = "GL Error Code: " + error;
          switch(error) {
            case GL30C.GL_INVALID_ENUM:
              translatedError = "GL_INVALID_ENUM";
              break;
            case GL30C.GL_INVALID_VALUE:
              translatedError = "GL_INVALID_VALUE";
              break;
            case GL30C.GL_INVALID_OPERATION:
              translatedError = "GL_INVALID_OPERATION";
              break;
          }
          throw new IllegalStateException("Error while uploading texture: " + translatedError);
        }

        // // Generate Mip Map (prob. not needed for our purposes)
        // glGenerateMipmap(GL_TEXTURE_2D);

        // get width and height
        int effectiveWidth = GL30C.glGetTexLevelParameteri(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_TEXTURE_WIDTH);
        int effectiveHeight = GL30C.glGetTexLevelParameteri(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_TEXTURE_HEIGHT);
        if(effectiveWidth != transferWidth || effectiveHeight != transferHeight) {
          throw new IllegalStateException("Upload failed: Width or height of created texture is wrong");
        }
        pTimer.endTask(taskName);
        return new OpenGLTexture(
            textureId, this, isMonochrome,
            pos, transferWidth, transferHeight
        );
    }

    /**
     * Uploads the contents of an image to the GPU as a texture
     * If the image exceeds the maximum texture size, it is split up into several textures
     * @param image A BufferedImage with a WritableRaster using a DataBufferUShort.
     * @return The GPUImage object which exposes methods to manipulate or download the image
     */
    public GPUImage uploadImage(BufferedImage image, int overlap) {
        int maxTileWidth = pMaxTextureWidth - 2 * overlap;
        int maxTileHeight = pMaxTextureHeight - 2 * overlap;
        int widthInTex = (int)Math.ceil((double)image.getWidth() / (double)maxTileWidth);
        int heightInTex = (int)Math.ceil((double)image.getHeight() / (double)maxTileHeight);
        int numTex = widthInTex * heightInTex;
        String taskName = "Upload image";
        pTimer.startTask(taskName, "## Uploading image as " + numTex + " textures to GPU");
        OpenGLTexture[] textures = new OpenGLTexture[numTex];

        for (int x = 0; x < widthInTex; x++) {            
            int originX = x * maxTileWidth;
            int width = Math.min(maxTileWidth, image.getWidth() - originX);
            for(int y = 0; y < heightInTex; y++) {
                int index = x + y * widthInTex;
                int originY = y * maxTileHeight;
                int height = Math.min(maxTileHeight, image.getHeight() - originY);
                textures[index] = uploadImagePart(image, originX, originY, width, height, overlap);
            }
        }
        pTimer.endTask(taskName);

        return new GPUImage(pTimer, textures);
    }

    /**
     * Uploads the contents of an image to the GPU,
     * modifies it using a list of fragment shaders,
     * and downloads it again. Mainly used for testing purposes.
     * @param image A BufferedImage with a WritableRaster using a DataBufferUShort.
     * @param fragmentShaders List of Source code for fragment shaders that will perform the transformation
     */
    public void applyShaders(BufferedImage image, String[] fragmentShaders, int overlap) {
      Pipeline<TransformableOnGPU> pipeline = new Pipeline<TransformableOnGPU>(
        pTimer);
      pipeline.add(new Processor<TransformableOnGPU>() {
          public TransformableOnGPU process(TransformableOnGPU img) {
            for(int i = 0; i < fragmentShaders.length; i++) {
                img.applyShaderInPlace(fragmentShaders[i]);
              }
            return img;
          }
        });
      transformTilewise(image, image, pipeline, overlap);
    }

    /**
     * Uploads the contents of an image to the GPU,
     * modifies it using a list of fragment shaders,
     * and downloads it again.
     * @param from A BufferedImage with a WritableRaster using a DataBufferUShort.
     * @param to A BufferedImage with a WritableRaster using a DataBufferUShort.
     * @param pipeline Pipeline that will transform the texture tiles
     * @param overlap how much overlap a possible demosaicing transformation needs
     */
    public void transformTilewise(BufferedImage from, BufferedImage to, Pipeline<TransformableOnGPU> pipeline, int overlap) {
      int maxTileWidth = pMaxTextureWidth - 2 * overlap;
      int maxTileHeight = pMaxTextureHeight - 2 * overlap;
      int widthInTex = (int)Math.ceil((double)from.getWidth() / (double)maxTileWidth);
      int heightInTex = (int)Math.ceil((double)from.getHeight() / (double)maxTileHeight);
      int numTex = widthInTex * heightInTex;
      String taskName = "Transforming image as " + numTex + "textures on GPU";
      pTimer.startTask(taskName, "");

      for (int x = 0; x < widthInTex; x++) {            
          int originX = x * maxTileWidth;
          int width = Math.min(maxTileWidth, from.getWidth() - originX);
          for(int y = 0; y < heightInTex; y++) {
              int originY = y * maxTileHeight;
              int height = Math.min(maxTileHeight, from.getHeight() - originY);
              int tile = x * heightInTex + y;
              pTimer.startTask("uploading tile " + tile, "");
              OpenGLTexture tex = uploadImagePart(from, originX, originY, width, height, overlap);
              pTimer.endTask("uploading tile " + tile);
              OpenGLTexture res = (OpenGLTexture) pipeline.process(tex);
              pTimer.startTask("downloading tile " + tile, "");
              res.downloadTo(to);
              pTimer.endTask("downloading tile " + tile);
              tex.delete();
              res.delete();
          }
      }
      pTimer.endTask(taskName);
    }

    /**
     * FOR TESTING ONLY: Allow artificial lowering of max texture sizes
     * to force splitting of a small test image
     */
    protected void setMaxTextureDimensions(int maxWidth, int maxHeight) {
        pMaxTextureWidth = Math.min(maxWidth, pMaxTextureWidth);
        pMaxTextureHeight = Math.min(maxHeight, pMaxTextureHeight);
    }
}
