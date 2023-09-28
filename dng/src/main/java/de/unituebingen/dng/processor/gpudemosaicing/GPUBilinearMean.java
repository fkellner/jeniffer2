package de.unituebingen.dng.processor.gpudemosaicing;

import java.util.Objects;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.opengl.TransformableOnGPU;

public class GPUBilinearMean implements Processor<TransformableOnGPU> {

    private final CFAPattern pCfaPattern;

    public GPUBilinearMean(CFAPattern cfaPattern) {
        Objects.requireNonNull(cfaPattern);
        this.pCfaPattern = cfaPattern;
    }

    public TransformableOnGPU process(TransformableOnGPU img) {
        img.applyShaderInPlace(fragmentShader(), true);
        return img;
    }

    private String fragmentShader() {
        //  assume square CFA pattern of side length 2. Positions are indexed as follows:
        //  +---+---+
        //  | 0 | 1 |
        //  +---+---+
        //  | 2 | 3 |
        //  +---+---+
        int green1Idx; int green2Idx; int redIdx; int blueIdx;
        switch(pCfaPattern) {
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
        // assume texture tiles align with pattern
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
    	color.g = texture(tex, TexCoord).r;
    	float vertical = 
    	    (texture(tex, vec2(TexCoord.x, TexCoord.y + texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x, TexCoord.y - texelSizeY)).r)
    	     / 2.0;
    	float horizontal = 
    	    (texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y)).r +
    	     texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y)).r)
    	     / 2.0;
    	color.r = (patternIdx + 2) % 4 == """ + redIdx + " ? vertical : horizontal;" + """
    	color.b = (patternIdx + 2) % 4 == """ + blueIdx + " ? vertical : horizontal;" + """    	     
    } else {
        // plus
    	color.g = 
    	    (texture(tex, vec2(TexCoord.x, TexCoord.y + texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x, TexCoord.y - texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y)).r +
    	     texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y)).r)
    	     / 4.0;
    	float cross = 
    	    (texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y + texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x + texelSizeX, TexCoord.y - texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y + texelSizeY)).r +
    	     texture(tex, vec2(TexCoord.x - texelSizeX, TexCoord.y - texelSizeY)).r)
    	     / 4.0;
    	color.r = patternIdx == """ + redIdx + " ? texture(tex, TexCoord).r : cross;" + """
    	color.b = patternIdx == """ + blueIdx + " ? texture(tex, TexCoord).r : cross;" + """
    }
}
                
        """;
    }
}
