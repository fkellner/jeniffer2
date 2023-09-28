package de.unituebingen.opengl;

public class FragmentShaderExamples {
    // shader source for the identity transformation on RGB textures
    public static String ID_RGB = """
        #version 130
        out vec3 color;
    
        in vec2 TexCoord;
    
        uniform sampler2D ourTexture;
    
        void main()
        {
            color = texture(ourTexture, TexCoord).rgb;
        }
        """;
    
    // shader source for the identity transformation on monochrome textures
    public static String ID_MONO = """
        #version 130
        out float color;
    
        in vec2 TexCoord;
    
        uniform sampler2D ourTexture;
    
        void main()
        {
            color = texture(ourTexture, TexCoord).r;
        }
        """;
    
    // shader source for inverting an rgb texture
    public static String INVERT_RGB = """
        #version 130
        out vec3 color;
    
        in vec2 TexCoord;
    
        uniform sampler2D ourTexture;
    
        void main()
        {
            color = vec3(1.0f,1.0f,1.0f) - texture(ourTexture, TexCoord).rgb;
        }
        """;
        
    // shader source for scaling a texture to half its size, demonstrating the edge handling strategy used (currently, repeating the edge pixels)
    public static String CLAMP_DEMO = """
        #version 130
        out vec3 color;
    
        in vec2 TexCoord;
    
        uniform sampler2D ourTexture;
    
        void main()
        {
            color = texture(ourTexture, vec2(TexCoord.x * 2.0f - 0.5f, TexCoord.y * 2.0f - 0.5f)).rgb;
        }
        """;
}
