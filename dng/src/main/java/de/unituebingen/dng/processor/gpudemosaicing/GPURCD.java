package de.unituebingen.dng.processor.gpudemosaicing;

import java.util.Objects;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.opengl.TransformableOnGPU;

public class GPURCD implements Processor<TransformableOnGPU> {
    // Ratio Corrected Demosaicing, adapted from:
    // https://github.com/LuisSR/RCD-Demosaicing/blob/master/rcd_demosaicing.c

    //  assume square CFA pattern of side length 2. Positions are indexed as follows:
    //  +---+---+
    //  | 0 | 1 |
    //  +---+---+
    //  | 2 | 3 |
    //  +---+---+
    private final int green1Idx;
    private final int green2Idx;
    private final int redIdx;
    private final int blueIdx;
    private RCDStep stopAt = RCDStep.DONE;

    public GPURCD(CFAPattern cfaPattern) {
        Objects.requireNonNull(cfaPattern);
        // precompute Indices for Shaders
        switch(cfaPattern) {
            case GBRG:
                green1Idx = 0; blueIdx = 1; redIdx = 2; green2Idx = 3;
                break;
            case BGGR:
                blueIdx = 0; green1Idx = 1; green2Idx = 2; redIdx = 3;
                break;
            case RGGB:
                redIdx = 0; green1Idx = 1; green2Idx = 2; blueIdx = 3;
                break;
            case GRBG:
            default:
                green1Idx = 0; redIdx = 1; blueIdx = 2; green2Idx = 3;
                break;
        }
    }

    public static enum RCDStep {
        RAW_DATA("0. Raw data (do nothing)"),
        XY_GRADIENT("1. XY-Gradient"),
        LOW_PASS("2. Low Pass Filter at Red and Blue (Green=bilinear interp.)"),
        GREENS("3. Finished Interpolation of Green Pixels"),
        PQ_GRADIENT("4. PQ-Gradient at Red and Blue (Green=bilinear interp.)"),
        RB_AT_BR("5. All done except RB at G"),
        DONE("6. Finished (but no post-processing)");


        private String label;

        RCDStep(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public RCDStep getStopAt() {
        return stopAt;
    }

    public void setStopAt(RCDStep step) {
        stopAt = step;
    }

    public void setStopAt(String s) {
        RCDStep[] options = RCDStep.values();
        for(int i = 0; i < options.length; i++) {
            if (options[i].getLabel() == s) {
                stopAt = options[i];
                break;
            }
        }
    }

    public TransformableOnGPU process(TransformableOnGPU img) {
        /* Dataflow and space in multiples of N = undemosaiced pixels

        1   img
            |
        2   O-----> xyGradient
            |       |
        3   O------------------> lowPassAtRB
            |       |            |
        4   O-------O------------O-----------> greens
            |       |                          |
        4   O-----------------------------------------> pqGradient
            |       |                          |        |
        6   O----------------------------------O--------O----------> greenEverywhereRBinPlace
                    |                                                |
        7           O------------------------------------------------O------------------------> result
        
        */

        // calculate greens
        TransformableOnGPU xyGradient = img.applyShader(xyGradientShader());
        if(stopAt == RCDStep.XY_GRADIENT) 
            return abort(xyGradient, debugShaderMono(), new TransformableOnGPU[]{img});
        TransformableOnGPU lowPassAtRB = img.applyShader(lowPassAtRBShader());
        if(stopAt == RCDStep.LOW_PASS) 
            return abort(lowPassAtRB, debugShaderRB(), new TransformableOnGPU[]{img, xyGradient});
        TransformableOnGPU greens = img.applyShader(
            greenShader(),
            false,
            xyGradient,
            "xyGradient",
            lowPassAtRB,
            "lowPass"
        );
        lowPassAtRB.delete();
        if(stopAt == RCDStep.GREENS) 
            return abort(greens, debugShaderMono(), new TransformableOnGPU[]{img, xyGradient});
        // calculate red and blue at blue and red
        TransformableOnGPU pqAtRB = img.applyShader(pqAtRBShader());
        if(stopAt == RCDStep.PQ_GRADIENT) 
            return abort(pqAtRB, debugShaderRB(), new TransformableOnGPU[]{img, xyGradient, greens});
        TransformableOnGPU greenEverywhereRBInPlace = img.applyShader(
            rbAtBrShader(), 
            true, 
            pqAtRB,
            "pqGrad",
            greens,
            "greens"
        );
        greens.delete();
        img.delete();
        pqAtRB.delete();
        if(stopAt == RCDStep.RB_AT_BR) {
            xyGradient.delete();
            return greenEverywhereRBInPlace;
        }
        // calculate red and blue at green
        TransformableOnGPU result = greenEverywhereRBInPlace.applyShader(
            rbAtGShader(), 
            false, 
            xyGradient, 
            "xyGradient",
            null,
            null
        );
        xyGradient.delete();
        greenEverywhereRBInPlace.delete();
        return result;
    }

    public TransformableOnGPU abort(TransformableOnGPU intermediateResult, String shader, TransformableOnGPU[] garbage) {
        for(int i = 0; i < garbage.length; i++) {
            garbage[i].delete();
        }
        intermediateResult.applyShaderInPlace(shader, true);
        return intermediateResult;
    }

    private String debugShaderMono() {
        
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;

void main()
{
    float monoVal = texture(tex, TexCoord).r;
    color = vec3(monoVal, monoVal, monoVal);
}
        """;
    }

//     private String debugShaderMultiMono() {
        
//         return """
// #version 130
// out vec3 color;

// in vec2 TexCoord;

// uniform sampler2D tex1;
// uniform sampler2D tex2;
// uniform sampler2D tex3;

// void main()
// {
//     float monoVal = 
//         TexCoord.x < 0.3f 
//         ? texture(tex1, TexCoord).r 
//         : (TexCoord.x < 0.6f 
//            ? texture(tex2, TexCoord).r 
//            : texture(tex3, TexCoord).r );
//     color = vec3(monoVal, monoVal, monoVal);
// }
//         """;
//     }

//     // show only pixels at green locations, red and green take pixel that is above/below
//     private String debugShaderMonoGreens() {
//         return """
// #version 130
// out vec3 color;

// in vec2 TexCoord;

// uniform sampler2D tex;

// void main()
// {
//     int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
//     ivec2 textureSize2d = textureSize(tex,0);
//     float texelSizeX = 1.0 / float(textureSize2d.x);
//     float texelSizeY = 1.0 / float(textureSize2d.y);
//     if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
//         float green = texture(tex, TexCoord).r;
//         color = vec3(green, green, green);    
//     } else {
//         float green = patternIdx > 1 
//                     ? texture(tex, vec2(TexCoord.x, TexCoord.y - texelSizeY)).r
//                     : texture(tex, vec2(TexCoord.x, TexCoord.y + texelSizeY)).r;
//         color = vec3(green, green, green); 
//     }
// }
                
//         """;
//     }

    // interpolate red and blue via nearest neighbour (effectively downsampling it)
    // add green as average between them to use full brightness spectrum
    private String debugShaderRB() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(tex,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        float val = (
            texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y)).r +
            texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y)).r +
            texture(tex, vec2(TexCoord.x, TexCoord.y + texelSizeY)).r +
            texture(tex, vec2(TexCoord.x, TexCoord.y - texelSizeY)).r
        ) / 4.0f;
        color = vec3(val, val, val);
    } else {
        float val = texture(tex, TexCoord).r;
        color = vec3(val, val, val);
    }
}
                
        """;
    }

    private String xyGradientShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;
uniform float epssq = 0.1f / (255 * 255); // 1e-10;

void main()
{
    ivec2 textureSize2d = textureSize(tex,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);

    float vM4 = texture(tex, vec2(TexCoord.x, TexCoord.y - 4.0f * texelSizeY)).r;
    float vM3 = texture(tex, vec2(TexCoord.x, TexCoord.y - 3.0f * texelSizeY)).r;
    float vM2 = texture(tex, vec2(TexCoord.x, TexCoord.y - 2.0f * texelSizeY)).r;
    float vM1 = texture(tex, vec2(TexCoord.x, TexCoord.y - 1.0f * texelSizeY)).r;
    float cur = texture(tex, TexCoord).r;
    float vP1 = texture(tex, vec2(TexCoord.x, TexCoord.y + 1.0f * texelSizeY)).r;
    float vP2 = texture(tex, vec2(TexCoord.x, TexCoord.y + 2.0f * texelSizeY)).r;
    float vP3 = texture(tex, vec2(TexCoord.x, TexCoord.y + 3.0f * texelSizeY)).r;
    float vP4 = texture(tex, vec2(TexCoord.x, TexCoord.y + 4.0f * texelSizeY)).r;

    float deltaY = max(
         1.f * vM4 * vM4 +
        -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
        -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
        12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
        -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur + 
        -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
         2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 +  11.f * vP2 * vP2 +
                             2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                      10.f * vP3 * vP3 +
                                                 2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 +  -2.f * vP2 * vP4 +  -6.f * vP3 * vP4 +  1.f * vP4 * vP4,
        epssq);

    float hM4 = texture(tex, vec2(TexCoord.x - 4.0f * texelSizeX, TexCoord.y)).r;
    float hM3 = texture(tex, vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y)).r;
    float hM2 = texture(tex, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y)).r;
    float hM1 = texture(tex, vec2(TexCoord.x - 1.0f * texelSizeX, TexCoord.y)).r;

    float hP1 = texture(tex, vec2(TexCoord.x + 1.0f * texelSizeX, TexCoord.y)).r;
    float hP2 = texture(tex, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y)).r;
    float hP3 = texture(tex, vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y)).r;
    float hP4 = texture(tex, vec2(TexCoord.x + 4.0f * texelSizeX, TexCoord.y)).r;

    float deltaX = max(
         1.f * hM4 * hM4 +
        -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
        -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
        12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
        -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur + 
        -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
         2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 +  11.f * hP2 * hP2 +
                             2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                      10.f * hP3 * hP3 +
                                                 2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 +  -2.f * hP2 * hP4 +  -6.f * hP3 * hP4 +  1.f * hP4 * hP4,
         epssq);

    color.r = deltaY / (deltaY + deltaX);
}
        """;
    }

    private String lowPassAtRBShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(tex,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        color.r = 0.0f;
    } else {
        // 3x3 low-pass filter

        float middle = texture(tex, TexCoord).r;

        float top =    texture(tex, vec2(TexCoord.x             , TexCoord.y - texelSizeY)).r;
        float bottom = texture(tex, vec2(TexCoord.x             , TexCoord.y + texelSizeY)).r;
        float left =   texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y             )).r;
        float right =  texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y             )).r;

        float topLeft =     texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r;
        float topRight =    texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r;
        float bottomLeft =  texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r;
        float bottomRight = texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r;

        color.r = 
            0.25f * middle +
            0.125f * (top + bottom + left + right) +
            0.0625f * (topLeft + topRight + bottomLeft + bottomRight);
    }
}
                
        """;
    }

    private String pqAtRBShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;
uniform float epssq = 0.1f / (255 * 255); //1e-10;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(tex,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        color.r = 0.0f;
    } else {
        // pq-gradient

        float vM4 = texture(tex, vec2(TexCoord.x - 4.0f * texelSizeX, TexCoord.y - 4.0f * texelSizeY)).r;
        float vM3 = texture(tex, vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y - 3.0f * texelSizeY)).r;
        float vM2 = texture(tex, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y - 2.0f * texelSizeY)).r;
        float vM1 = texture(tex, vec2(TexCoord.x - 1.0f * texelSizeX, TexCoord.y - 1.0f * texelSizeY)).r;
        float cur = texture(tex, TexCoord).r;
        float vP1 = texture(tex, vec2(TexCoord.x + 1.0f * texelSizeX, TexCoord.y + 1.0f * texelSizeY)).r;
        float vP2 = texture(tex, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y + 2.0f * texelSizeY)).r;
        float vP3 = texture(tex, vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y + 3.0f * texelSizeY)).r;
        float vP4 = texture(tex, vec2(TexCoord.x + 4.0f * texelSizeX, TexCoord.y + 4.0f * texelSizeY)).r;

        float pStat = max(
             1.f * vM4 * vM4 +
            -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
            -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
            12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
            -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur + 
            -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
             2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 +  11.f * vP2 * vP2 +
                                 2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                      10.f * vP3 * vP3 +
                                                     2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 +  -2.f * vP2 * vP4 +  -6.f * vP3 * vP4 +  1.f * vP4 * vP4,
            epssq);

        float hM4 = texture(tex, vec2(TexCoord.x - 4.0f * texelSizeX, TexCoord.y + 4.0f * texelSizeY)).r;
        float hM3 = texture(tex, vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y + 3.0f * texelSizeY)).r;
        float hM2 = texture(tex, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y + 2.0f * texelSizeY)).r;
        float hM1 = texture(tex, vec2(TexCoord.x - 1.0f * texelSizeX, TexCoord.y + 1.0f * texelSizeY)).r;

        float hP1 = texture(tex, vec2(TexCoord.x + 1.0f * texelSizeX, TexCoord.y - 1.0f * texelSizeY)).r;
        float hP2 = texture(tex, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y - 2.0f * texelSizeY)).r;
        float hP3 = texture(tex, vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y - 3.0f * texelSizeY)).r;
        float hP4 = texture(tex, vec2(TexCoord.x + 4.0f * texelSizeX, TexCoord.y - 4.0f * texelSizeY)).r;

        float qStat = max(
             1.f * hM4 * hM4 +
            -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
            -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
            12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
            -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur + 
            -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
             2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 +  11.f * hP2 * hP2 +
                                 2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                      10.f * hP3 * hP3 +
                                                     2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 +  -2.f * hP2 * hP4 +  -6.f * hP3 * hP4 +  1.f * hP4 * hP4,
             epssq);

        color.r = pStat / (pStat + qStat);
    }
}
                
        """;
    }

    private String greenShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D orig;
uniform sampler2D xyGradient;
uniform sampler2D lowPass;

uniform float eps = 0.1f / (255 * 255); //1e-5;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(orig,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        // we already have green values
        color.r = texture(orig, TexCoord).r;
    } else {
        // ## greens at red and blue pixels

        float centerGradient = texture(xyGradient, TexCoord).r;
        // X-shaped to get gradient for greens in neighbourhood
        float neighbourhoodGradient = 0.25f * (
            texture(xyGradient, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r
        );
        // take the bigger one
        float vhDisc = 
            abs(centerGradient - 0.5f) < abs(neighbourhoodGradient - 0.5f)
            ? neighbourhoodGradient
            : centerGradient;

        // cardinal gradients
        float vM4 = texture(orig, vec2(TexCoord.x, TexCoord.y - 4.0f * texelSizeY)).r;
        float vM3 = texture(orig, vec2(TexCoord.x, TexCoord.y - 3.0f * texelSizeY)).r;
        float vM2 = texture(orig, vec2(TexCoord.x, TexCoord.y - 2.0f * texelSizeY)).r;
        float vM1 = texture(orig, vec2(TexCoord.x, TexCoord.y - 1.0f * texelSizeY)).r;
        float cur = texture(orig, TexCoord).r;
        float vP1 = texture(orig, vec2(TexCoord.x, TexCoord.y + 1.0f * texelSizeY)).r;
        float vP2 = texture(orig, vec2(TexCoord.x, TexCoord.y + 2.0f * texelSizeY)).r;
        float vP3 = texture(orig, vec2(TexCoord.x, TexCoord.y + 3.0f * texelSizeY)).r;
        float vP4 = texture(orig, vec2(TexCoord.x, TexCoord.y + 4.0f * texelSizeY)).r;

        float hM4 = texture(orig, vec2(TexCoord.x - 4.0f * texelSizeX, TexCoord.y)).r;
        float hM3 = texture(orig, vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y)).r;
        float hM2 = texture(orig, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y)).r;
        float hM1 = texture(orig, vec2(TexCoord.x - 1.0f * texelSizeX, TexCoord.y)).r;
    
        float hP1 = texture(orig, vec2(TexCoord.x + 1.0f * texelSizeX, TexCoord.y)).r;
        float hP2 = texture(orig, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y)).r;
        float hP3 = texture(orig, vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y)).r;
        float hP4 = texture(orig, vec2(TexCoord.x + 4.0f * texelSizeX, TexCoord.y)).r;

        float nGrad = eps + 
            abs(vM1 - vP1) + 
            abs(cur - vM2) + 
            abs(vM1 - vM3) + 
            abs(vM2 - vM4);
        float sGrad = eps + 
            abs(vP1 - vM1) + 
            abs(cur - vP2) + 
            abs(vP1 - vP3) + 
            abs(vP2 - vP4);
        float wGrad = eps + 
            abs(hM1 - hP1) + 
            abs(cur - hM2) + 
            abs(hM1 - hM3) + 
            abs(hM2 - hM4);
        float eGrad = eps + 
            abs(hP1 - hM1) + 
            abs(cur - hP2) + 
            abs(hP1 - hP3) + 
            abs(hP2 - hP4);

        float lpfCur = texture(lowPass, TexCoord).r;
        float lpfN = texture(lowPass, vec2(TexCoord.x, TexCoord.y - 2.0f * texelSizeY)).r;
        float lpfS = texture(lowPass, vec2(TexCoord.x, TexCoord.y + 2.0f * texelSizeY)).r;
        float lpfW = texture(lowPass, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y)).r;
        float lpfE = texture(lowPass, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y)).r;

        // cardinal pixel estimations
        float nEst = 
            vM1 * (1.0f + (lpfCur - lpfN) / (eps + lpfCur + lpfN));
        float sEst = 
            vP1 * (1.0f + (lpfCur - lpfS) / (eps + lpfCur + lpfS));
        float wEst = 
            hM1 * (1.0f + (lpfCur - lpfW) / (eps + lpfCur + lpfW));
        float eEst = 
            hP1 * (1.0f + (lpfCur - lpfE) / (eps + lpfCur + lpfE));

        // vertical and horizontal estimations
        float vEst = 
            (sGrad * nEst + nGrad * sEst) / (nGrad + sGrad);
        float hEst = 
            (wGrad * eEst + eGrad * wEst) / (eGrad + wGrad);

        // interpolation
        color.r = clamp(
            vhDisc * hEst + (1.0f - vhDisc) * vEst,
            0.0f, 1.0f
        );
    }
}
                
        """;
    }

    private String rbAtBrShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D orig;
uniform sampler2D pqGrad;
uniform sampler2D greens;

uniform float epssq = 0.1f / (255 * 255); //1e-10;
uniform float eps = 0.1f / (255 * 255); //1e-5;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(orig,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        color.g = texture(greens, TexCoord).r;
        color.r = 0.0f;
        color.b = 0.0f;
    } else {
        // ## red and blue at blue and red pixels

        float centerGradient = texture(pqGrad, TexCoord).r;
        // X-shaped to get gradient for reds/blues in neighbourhood
        float neighbourhoodGradient = 0.25f * (
            texture(pqGrad, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(pqGrad, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r +
            texture(pqGrad, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(pqGrad, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r
        );
        // take the bigger one
        float pqDisc = 
            abs(centerGradient - 0.5f) < abs(neighbourhoodGradient - 0.5f)
            ? neighbourhoodGradient
            : centerGradient;

        // diagonal gradients
        float center = texture(greens, TexCoord).r;
        float northWest1 = texture(orig,   vec2(TexCoord.x -        texelSizeX, TexCoord.y - texelSizeY       )).r;
        float northWest2 = texture(greens, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y - 2.0f * texelSizeY)).r;
        float northWest3 = texture(orig,   vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y - 3.0f * texelSizeY)).r;

        float southEast1 = texture(orig,   vec2(TexCoord.x +        texelSizeX, TexCoord.y + texelSizeY       )).r;
        float southEast2 = texture(greens, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y + 2.0f * texelSizeY)).r;
        float southEast3 = texture(orig,   vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y + 3.0f * texelSizeY)).r;

        float southWest1 = texture(orig,   vec2(TexCoord.x -        texelSizeX, TexCoord.y + texelSizeY       )).r;
        float southWest2 = texture(greens, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y + 2.0f * texelSizeY)).r;
        float southWest3 = texture(orig,   vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y + 3.0f * texelSizeY)).r;

        float northEast1 = texture(orig,   vec2(TexCoord.x +        texelSizeX, TexCoord.y - texelSizeY       )).r;
        float northEast2 = texture(greens, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y - 2.0f * texelSizeY)).r;
        float northEast3 = texture(orig,   vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y - 3.0f * texelSizeY)).r;
        
        float nwGrad = eps + abs(northWest1 - southEast1) + abs(northWest1 - northWest3) + abs(center - northWest2);
        float neGrad = eps + abs(northEast1 - southWest1) + abs(northEast1 - northEast3) + abs(center - northEast2);
        float swGrad = eps + abs(southWest1 - northEast1) + abs(southWest1 - southWest3) + abs(center - southWest2);
        float seGrad = eps + abs(southEast1 - northWest1) + abs(southEast1 - southEast3) + abs(center - southEast2);        

        // diagonal color differences
        float nwEst = 
            northWest1 -
            texture(greens, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r;
        float neEst = 
            northEast1 -
            texture(greens, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r;
        float swEst = 
            southWest1 -
            texture(greens, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r;
        float seEst = 
            southEast1 -
            texture(greens, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r;

        // p and q estimations
        float pEst = 
            (nwGrad * seEst + seGrad * nwEst) / (nwGrad + seGrad);
        float qEst = 
            (neGrad * swEst + swGrad * neEst) / (neGrad + swGrad);

        // interpolation
        color.g = center;
        float interp = clamp(
            center + (1.0f - pqDisc) * pEst + pqDisc * qEst,
            0.0f, 1.0f
        );
        float cur = texture(orig, TexCoord).r;
        color.r = patternIdx == """ + redIdx + " ? cur : interp;" + """
    	color.b = patternIdx == """ + blueIdx + " ? cur : interp;" + """
    }
}
                
        """;
    }

    private String rbAtGShader() {
        return """
#version 130
out vec3 color;

in vec2 TexCoord;

uniform sampler2D tex;
uniform sampler2D xyGradient;

uniform float eps = 0.1f / (255 * 255); //1e-5;

void main()
{
    int patternIdx = (int(floor(gl_FragCoord.x)) % 2) + 2 * (int(floor(gl_FragCoord.y)) % 2);
    ivec2 textureSize2d = textureSize(tex,0);
    float texelSizeX = 1.0 / float(textureSize2d.x);
    float texelSizeY = 1.0 / float(textureSize2d.y);
    if(patternIdx == """ + green1Idx + " || patternIdx ==" + green2Idx + ") {" + """
        // we already have green values
        color.g = texture(tex, TexCoord).g;
        // Refined vertical and horizontal local discrimination
        float centerGradient = texture(xyGradient, TexCoord).r;
        // X-shaped to get gradient for greens in neighbourhood
        float neighbourhoodGradient = 0.25f * (
            texture(xyGradient, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r +
            texture(xyGradient, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r
        );
        // take the bigger one
        float vhDisc = 
            abs(centerGradient - 0.5f) < abs(neighbourhoodGradient - 0.5f)
            ? neighbourhoodGradient
            : centerGradient;

        // Cardinal gradients
        vec3 center = texture(tex, TexCoord).rgb;
        vec3 north1 = texture(tex, vec2(TexCoord.x, TexCoord.y - texelSizeY)).rgb;
        vec3 north2 = texture(tex, vec2(TexCoord.x, TexCoord.y - 2.0f * texelSizeY)).rgb;
        vec3 north3 = texture(tex, vec2(TexCoord.x, TexCoord.y - 3.0f * texelSizeY)).rgb;
        vec3 south1 = texture(tex, vec2(TexCoord.x, TexCoord.y + texelSizeY)).rgb;
        vec3 south2 = texture(tex, vec2(TexCoord.x, TexCoord.y + 2.0f * texelSizeY)).rgb;
        vec3 south3 = texture(tex, vec2(TexCoord.x, TexCoord.y + 3.0f * texelSizeY)).rgb;
        vec3 west1 = texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y)).rgb;
        vec3 west2 = texture(tex, vec2(TexCoord.x - 2.0f * texelSizeX, TexCoord.y)).rgb;
        vec3 west3 = texture(tex, vec2(TexCoord.x - 3.0f * texelSizeX, TexCoord.y)).rgb;
        vec3 east1 = texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y)).rgb;
        vec3 east2 = texture(tex, vec2(TexCoord.x + 2.0f * texelSizeX, TexCoord.y)).rgb;
        vec3 east3 = texture(tex, vec2(TexCoord.x + 3.0f * texelSizeX, TexCoord.y)).rgb;

        float nGradR = eps + abs(center.g - north2.g) + abs(north1.r - south1.r) + abs(north1.r - north3.r);
        float sGradR = eps + abs(center.g - south2.g) + abs(south1.r - north1.r) + abs(south1.r - south3.r);
        float wGradR = eps + abs(center.g - west2.g) + abs(west1.r - east1.r) + abs(west1.r - west3.r);
        float eGradR = eps + abs(center.g - east2.g) + abs(east1.r - west1.r) + abs(east1.r - east3.r);

        float nGradB = eps + abs(center.g - north2.g) + abs(north1.b - south1.b) + abs(north1.b - north3.b);
        float sGradB = eps + abs(center.g - south2.g) + abs(south1.b - north1.b) + abs(south1.b - south3.b);
        float wGradB = eps + abs(center.g - west2.g) + abs(west1.b - east1.b) + abs(west1.b - west3.b);
        float eGradB = eps + abs(center.g - east2.g) + abs(east1.b - west1.b) + abs(east1.b - east3.b);

        // cardinal color differences
        float nEstR = north1.r - north1.g;
        float sEstR = south1.r - south1.g;
        float wEstR = west1.r - west1.g;
        float eEstR = east1.r - east1.g;

        float nEstB = north1.b - north1.g;
        float sEstB = south1.b - south1.g;
        float wEstB = west1.b - west1.g;
        float eEstB = east1.b - east1.g;
        
        // Vertical and horizontal estimations
        float vEstR = (nGradR * sEstR + sGradR * nEstR) / (nGradR + sGradR);
        float hEstR = (eGradR * wEstR + wGradR * eEstR) / (eGradR + wGradR);
        float vEstB = (nGradB * sEstB + sGradB * nEstB) / (nGradB + sGradB);
        float hEstB = (eGradB * wEstB + wGradB * eEstB) / (eGradB + wGradB);

        // interpolation
        color.r = clamp(
            center.g + (1.0f - vhDisc) * vEstR + vhDisc * hEstR,
            0.0f,
            1.0f
        );
        color.b = clamp(
            center.g + (1.0f - vhDisc) * vEstB + vhDisc * hEstB,
            0.0f,
            1.0f
        );
    } else {
        // we are already done
        color = texture(tex, TexCoord).rgb;
    }
}
                
        """;
    }
}
