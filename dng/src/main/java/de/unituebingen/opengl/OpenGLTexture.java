package de.unituebingen.opengl;

import org.lwjgl.opengl.*;

import java.awt.image.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWVidMode;

/*
 * Wrapper class for keeping track of an OpenGL texture uploaded to an OpenGLWrapper
 */
public class OpenGLTexture implements TransformableOnGPU {
    
    /*
     * Id of the texture
     */
    private int pTextureId;
    /*
     * Prevend trying to delete same texture twice
     */
    private boolean pDeleted = false;

    /*
     * Whether the texture is monochrome
     */
    private boolean pIsMonochrome;

    /*
     * Reference to OpenGLContext that created this texture
     * Needed for rendering to window and also to keep the Context from being garbage collected
     */
    private OpenGLContext pContext;

    /**
     * position in the databuffer of the image this texture belongs to
     */
    private PositionInImage pPos;
    /**
     * texture width and height so that we do not always need to ask the GPU for it
     */
    private int pWidth;
    private int pHeight;

    /**
     * A texture on the GPU and its metadata concerning the source image
     * @param id of OpenGL texture object
     * @param context OpenGL context in which the object was created
     * @param isMonochrome
     * @param positionInImage 
     * @param width
     * @param height
     */
    protected OpenGLTexture(
        int id, 
        OpenGLContext context, 
        boolean isMonochrome,
        PositionInImage positionInImage,
        int width, int height
    ) {
        this.pTextureId = id;
        this.pContext = context;
        this.pIsMonochrome = isMonochrome;
        this.pPos = positionInImage;
        this.pWidth = width; this.pHeight = height;
    }

    /**
     * release allocated ressources (i.e. OpenGL texture stored on GPU memory)
     * @return
     */
    public void delete() {
        if(pDeleted) return;
        GL30C.glDeleteTextures(pTextureId);
        GL30C.glFinish(); // force texture deletion!
        pDeleted = true;
    }

    /*
     * return the id of the stored texture
     */
    protected int getId() {
        return pTextureId;
    }

    /**
     * Downloads texture from the GPU and writes its data to the DataBuffer of a BufferedImage
     * @param target  The BufferedImage with a WritableRaster using a DataBufferUShort to which 
     *                the content of the texture will be written
     * Warns if target image is bigger than texture, throws an exception if texture would not fit
     */
    public void downloadTo(BufferedImage target) {
        String taskName = "downloading texture";
        pContext.pTimer.startTask(taskName, "sanity checks");
        // we will write overlap right/bottom before restoring original image content later
        int copiedWidth = (pWidth - pPos.overlapLeft);  
        int copiedHeight = (pHeight - pPos.overlapTop);
        if (pPos.originX + copiedWidth > target.getWidth() ||
            pPos.originY + copiedHeight > target.getHeight() || 
            pPos.originX - pPos.overlapLeft < 0 || 
            pPos.originY - pPos.overlapTop < 0) {
            throw new IllegalStateException("Trying to write outside target image.");
        }
        // other function in thread may have changed context (e.g. GUI)
        glfwMakeContextCurrent(pContext.getWindow());
        GL.createCapabilities();
        // set current texture according to id
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, pTextureId);

        // getting info about data channels
        WritableRaster raster = target.getRaster();
        int channels = raster.getNumDataElements();
        if(pIsMonochrome && channels != 1) {
            throw new IllegalStateException("Trying to download a monochrome texture to an image with " + channels + " channels");
        }
        if(!pIsMonochrome && channels != 3) {
            throw new IllegalStateException("Trying to download an RGB texture to an image with " + channels + " channels");
        }

        // getting info about transfer type
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
            "Trying to save unsigned shorts to a BufferedImage with" +
            "an underlying DataBuffer expecting " + transferTypeTranslated);
        }
        pContext.pTimer.endTask(taskName);
        pContext.pTimer.startTask(taskName, "allocating space and saving overlaps");
        // allocate buffer to receive data
        short[] buffer = new short[pWidth * pHeight * channels];
        // download original state of overlapping regions
        short[] topBar = (short[]) raster.getDataElements(
            pPos.originX - pPos.overlapLeft, // includes top left square
            pPos.originY - pPos.overlapTop,
            pWidth, // includes top right square
            pPos.overlapTop,
            null);
        short[] bottomBar = (short[]) raster.getDataElements(
            pPos.originX - pPos.overlapLeft, // includes bottom left square
            pPos.originY + pHeight - pPos.overlapTop - pPos.overlapBottom,
            pWidth, // includes bottom right square
            pPos.overlapBottom,
            null);
        short[] leftBar = (short[]) raster.getDataElements(
            pPos.originX - pPos.overlapLeft,
            pPos.originY, // excludes top left square
            pPos.overlapLeft,
            pHeight - pPos.overlapTop - pPos.overlapBottom, // excludes bottom left square
            null);
        short[] rightBar = (short[]) raster.getDataElements(
            pPos.originX + pWidth - pPos.overlapLeft - pPos.overlapRight,
            pPos.originY, // excludes top right square
            pPos.overlapRight,
            pHeight - pPos.overlapTop - pPos.overlapBottom, // excludes bottom right square
            null);
        pContext.pTimer.endTask(taskName);
        pContext.pTimer.startTask(taskName, "downloading to buffer");
        // download image to buffer
        GL30C.glGetTexImage(
          GL30C.GL_TEXTURE_2D,
          0,
          pIsMonochrome ? GL30C.GL_RED : GL30C.GL_RGB,
          GL30C.GL_UNSIGNED_SHORT,
          buffer
        );
        pContext.pTimer.endTask(taskName);
        pContext.pTimer.startTask(taskName, "updating image contents");
        // write back to image
        raster.setDataElements(
            pPos.originX - pPos.overlapLeft,
            pPos.originY - pPos.overlapTop,
            pWidth, pHeight, buffer);
        // restore original state of overlapping regions
        raster.setDataElements(
            pPos.originX - pPos.overlapLeft,
            pPos.originY - pPos.overlapTop,
            pWidth,
            pPos.overlapTop,
            topBar);
        raster.setDataElements(
            pPos.originX - pPos.overlapLeft,
            pPos.originY + pHeight - pPos.overlapTop - pPos.overlapBottom,
            pWidth,
            pPos.overlapBottom,
            bottomBar);
        raster.setDataElements(
            pPos.originX - pPos.overlapLeft,
            pPos.originY, // excludes top left square
            pPos.overlapLeft,
            pHeight - pPos.overlapTop - pPos.overlapBottom, // excludes bottom left square
            leftBar);
        raster.setDataElements(
            pPos.originX + pWidth - pPos.overlapLeft - pPos.overlapRight,
            pPos.originY, // excludes top right square
            pPos.overlapRight,
            pHeight - pPos.overlapTop - pPos.overlapBottom, // excludes bottom right square
            rightBar);
        pContext.pTimer.endTask(taskName);
    }

    /**
     * applies a fragment shader to each pixel of the texture
     * @param fragmentShaderSource the program code for the fragment shader - see FragmentShaderExamples for examples
     */
    public void applyShaderInPlace(String fragmentShaderSource) {
        applyShaderInPlace(fragmentShaderSource, false);
    }
    
    /**
     * applies a fragment shader to each pixel of the texture
     * @param fragmentShaderSource the program code for the fragment shader - see FragmentShaderExamples for examples
     * @param demosaic whether the shader turns a monochrome into an RGB texture
     */
    public void applyShaderInPlace(String fragmentShaderSource, boolean demosaic) {
        int targetTexture = applyShaderInternal(fragmentShaderSource, demosaic, null, null, null, null);
        if(demosaic) pIsMonochrome = false;
        // delete old texture
        GL30C.glDeleteTextures(pTextureId);
        GL30C.glFinish(); // force texture deletion!
        // remember id of new texture
        pTextureId = targetTexture;
    }

    public OpenGLTexture applyShader(String fragmentShaderSource) {
        return applyShader(fragmentShaderSource, false);
    }

    public OpenGLTexture applyShader(String fragmentShaderSource, boolean demosaic) {
        return applyShader(fragmentShaderSource, demosaic, null, null, null, null);
    }

    public OpenGLTexture applyShader(String fragmentShaderSource, boolean demosaic, TransformableOnGPU t2, String tex2name, TransformableOnGPU t3, String tex3name) {
        // more general type needed for interface to work
        OpenGLTexture tex2 = (OpenGLTexture) t2;
        OpenGLTexture tex3 = (OpenGLTexture) t3;
        int targetTexture = applyShaderInternal(fragmentShaderSource, demosaic, tex2, tex2name, tex3, tex3name);
        return new OpenGLTexture(
            targetTexture, 
            pContext, 
            pIsMonochrome && !demosaic, 
            pPos, 
            pWidth, 
            pHeight);
    }

    private int applyShaderInternal(String fragmentShaderSource, boolean demosaic, OpenGLTexture tex2, String tex2Name, OpenGLTexture tex3, String tex3Name) {
        if (!pIsMonochrome && demosaic) {
            throw new IllegalArgumentException("cannot demosaic an already RGB texture");
        }
        // other function in thread may have changed context (e.g. GUI)
        glfwMakeContextCurrent(pContext.getWindow());
        GL.createCapabilities();
        // ### create a framebuffer to hold target texture
        int FBO = GL30C.glGenFramebuffers();
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, FBO);

        // ### get dimensions of input texture
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, pTextureId);

        // ### create new texture of same size
        int targetTexture = GL30C.glGenTextures();
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, targetTexture);

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
            throw new IllegalStateException("Error before filling target texture with empty data");
        }

        // fill with empty data
        boolean becomesMonochrome = pIsMonochrome && !demosaic;
        GL30C.glTexImage2D(
        GL30C.GL_TEXTURE_2D,
            0,
            becomesMonochrome ? GL30C.GL_R32F : GL30C.GL_RGB32F, //16UI,
            pWidth,
            pHeight,
            0,
            becomesMonochrome ? GL30C.GL_RED : GL30C.GL_RGB,
            GL30C.GL_UNSIGNED_SHORT,
            0
        );
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
            throw new IllegalStateException("Error while filling target texture with empty data: " + translatedError);
        }

        // configure FBO to use texture as render target
        GL30C.glFramebufferTexture2D(
            GL30C.GL_FRAMEBUFFER,
            GL30C.GL_COLOR_ATTACHMENT0,
            GL30C.GL_TEXTURE_2D,
            targetTexture,
            0);
        GL30C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);

        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if(status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Error while preparing FrameBuffer");
        }
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, FBO);
        
        // set viewport to target size
        GL30C.glViewport(0, 0, pWidth, pHeight);
        // upload vertex data
        int VAO = uploadVertexData(VERTICES_RENDERING);
        // create shader program
        int shaderProgram = createShaderProgram(fragmentShaderSource);
        // activate program
        GL30C.glUseProgram(shaderProgram);

        // bind textures to be used by program
        GL30C.glActiveTexture(GL30C.GL_TEXTURE0);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, pTextureId);
        if(tex2 != null && tex2Name != null) {
            int loc = GL30C.glGetUniformLocation(shaderProgram, tex2Name);
            GL30C.glUniform1i(loc, 1);
            GL30C.glActiveTexture(GL30C.GL_TEXTURE0 + 1);
            GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, tex2.getId());
        }
        if(tex3 != null && tex3Name != null) {
            int loc = GL30C.glGetUniformLocation(shaderProgram, tex3Name);
            GL30C.glUniform1i(loc, 2);
            GL30C.glActiveTexture(GL30C.GL_TEXTURE0 + 2);
            GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, tex3.getId());
        }

        // set object
        GL30C.glBindVertexArray(VAO);

        // actually draw array:
        GL30C.glDrawArrays(GL30C.GL_TRIANGLES, 0, NUM_VERTICES);

        // be nice and clean up
        GL30C.glDeleteFramebuffers(FBO);
        GL30C.glDeleteVertexArrays(VAO);
        GL30C.glDeleteProgram(shaderProgram);
        // return new id
        return targetTexture;
    }
      
    /**
     * Private helper to create an OpenGL shader program. The vertex shader just hands forward the 
     * position and texture coordinates given by the vertex data
     * @param fragmentShaderSource Source code of the fragment shader
     * @return id of the created shader program
     */
    private int createShaderProgram(String fragmentShaderSource) {
        // ###### vertex shader creation
        // vertex shader as a string
        // does not change anything, just hands forward positions
        String vertexShaderSource = """
        #version 130
        out vec2 TexCoord;
  
        in vec3 aPos;
        in vec2 aTexCoord;
  
        void main()
        {
            gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);
            TexCoord = aTexCoord;
        }
        """;
  
        // create vertex shader object
        int vertexShader = GL30C.glCreateShader(GL30C.GL_VERTEX_SHADER);
  
        // upload shader source
        GL30C.glShaderSource(vertexShader, vertexShaderSource);
  
        // compile shader
        GL30C.glCompileShader(vertexShader);
  
        // check for errors
        int success = GL30C.glGetShaderi(
          vertexShader,
          GL30C.GL_COMPILE_STATUS
        );
        if(success <= 0) {
          throw new IllegalStateException(
            "### Error compiling vertex shader\n" +
            GL30C.glGetShaderInfoLog(vertexShader));
        }
  
        // ###### fragment shader creation
        // create fragment shader object
        int fragmentShader = createFragmentShader(fragmentShaderSource);
  
        // ##### shader Program
        int shaderProgram = GL30C.glCreateProgram();
        GL30C.glAttachShader(shaderProgram, vertexShader);
        GL30C.glAttachShader(shaderProgram, fragmentShader);
        GL30C.glLinkProgram(shaderProgram);
  
        // check success
        success = GL30C.glGetProgrami(shaderProgram, GL30C.GL_LINK_STATUS);
        if(success <= 0) {
          throw new IllegalStateException(
            "### Error linking shader program" +
            GL30C.glGetProgramInfoLog(shaderProgram)
          );
        }
        // clean up
        GL30C.glDeleteShader(vertexShader);
        GL30C.glDeleteShader(fragmentShader);
  
        return shaderProgram;
    }
  
    /**
     * Creates an OpenGL fragment shader from a string of source code
     * @param source source code of the fragment shader
     * @return id of the created fragment shader
     */
    private int createFragmentShader(String source) {
        // create the ID
        int id = GL30C.glCreateShader(GL30C.GL_FRAGMENT_SHADER);

        // load the source
        GL30C.glShaderSource(id, source);

        // compile and check
        GL30C.glCompileShader(id);
        int status = GL30C.glGetShaderi(id, GL30C.GL_COMPILE_STATUS);
        if (status != GL30C.GL_TRUE) {
            throw new RuntimeException(
            "#### Error compiling fragment shader \n" +
            GL30C.glGetShaderInfoLog(id)
            );
        }
        return id;
    }
      
    /**
     * Upload vertex data to the GPU (create geometry to be rendered)
     * @param vertices Vertex data - assuming five floats per vertex (x,y,z and u,t texture coordinates)
     * @return id of the created Vertex Array Object (VAO)
     */
    private int uploadVertexData(float[] vertices) {
    // allocate GPU memory buffer
        int VAO = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(VAO);
        GL30C.glBindBuffer(GL30C.GL_ARRAY_BUFFER, VAO);

        // upload vertex data read-only
        GL30C.glBufferData(
        GL30C.GL_ARRAY_BUFFER,
        vertices,
        GL30C.GL_STATIC_DRAW
        );
        final int BYTES_PER_FLOAT = 4;
        GL30C.glVertexAttribPointer(
        0, 3, GL30C.GL_FLOAT, false, 5 * BYTES_PER_FLOAT, 0 // 5 els, 4 byte per float
        );
        GL30C.glVertexAttribPointer(
        1, 2, GL30C.GL_FLOAT, false, 5 * BYTES_PER_FLOAT, 3 * BYTES_PER_FLOAT
        );
        GL30C.glEnableVertexAttribArray(0);
        GL30C.glEnableVertexAttribArray(1);
        return VAO;
    }
  
    /**
     * Rectangle used for displaying textures
     */
    private static float[] VERTICES_DISPLAYING = {
        // positions [-1,1] // texture coordinates [0,1]
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
        1.0f,  1.0f, 0.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 0.0f, 0.0f,
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
        1.0f,  1.0f, 0.0f, 1.0f, 0.0f
    };
    /**
     * Rectangle used for rendering texture to texture
     */
    private static float[] VERTICES_RENDERING = {
        // positions [-1,1] // texture coordinates [0,1]
        -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
        -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
        -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
        1.0f,  1.0f, 0.0f, 1.0f, 1.0f
    };
  
    // the number of vertices uploaded by the previous method
    private static int NUM_VERTICES = 6;  
      
    /**
     * Display a texture for visual debugging.
     * Since the texture is set to do no interpolation, you will most likely see a downsampled version 
     * that fits the screen.
     */
    public void display() {
        // other function in thread may have changed context (e.g. GUI)
        glfwMakeContextCurrent(pContext.getWindow());
        GL.createCapabilities();
        // get maximum window size
        GLFWVidMode vidMode = pContext.getVidMode();
        int width = pWidth;
        int height = pHeight;
        while(width > vidMode.width() || height > vidMode.height()) {
          // this will probably be okay since most sizes are divisible by two...
          width = width / 2;
          height = height / 2;
        }
        // set window size 
        long window = pContext.getWindow();
        glfwSetWindowSize(window, width, height);
        glfwSetWindowTitle(window, 
          "Texture at " + pPos.originX + "," + pPos.originY + " of size " + pWidth + "x" + pHeight);
        // and render area:
        GL30C.glViewport(0, 0, width, height);
  
        // upload vertex data
        int VAO = uploadVertexData(VERTICES_DISPLAYING);
  
        // create shader program
        int shaderProgram = createShaderProgram("""
          #version 130
          out vec4 FragColor;
    
          in vec2 TexCoord;
    
          uniform sampler2D ourTexture;
    
          void main()
          {
              FragColor = texture(ourTexture, TexCoord);
          }
          """);
  
        // activate program
        GL30C.glUseProgram(shaderProgram);
        // set object
        GL30C.glBindVertexArray(VAO);
        // set current texture according to id
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, pTextureId);
  
        // actually draw array:
        GL30C.glDrawArrays(GL30C.GL_TRIANGLES, 0, NUM_VERTICES);
        glfwShowWindow(window);
        glfwSwapBuffers(window);
        // clean up
        GL30C.glDeleteVertexArrays(VAO);
        GL30C.glDeleteProgram(shaderProgram);
        // give us time to inspect the result
        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
        glfwSetWindowShouldClose(window, false);
        glfwHideWindow(window);
    }
  
    
}
