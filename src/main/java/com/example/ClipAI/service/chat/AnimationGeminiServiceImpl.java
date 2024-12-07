package com.example.ClipAI.service.chat;

import com.example.ClipAI.model.ClipAIRest;
import org.springframework.stereotype.Service;

@Service
public class AnimationGeminiServiceImpl extends AbstractGeminiService implements AnimationGeminiService{

    @Override
    public String getImagesPrompt(ClipAIRest clipAIRest) {
        StringBuilder imagePromptTemplate = new StringBuilder()
                .append(String.format("%s \b \b Generate 13 detailed cinematic image descriptions for a video about %s. Each image should match specific moments in the script and create compelling visual impact in modern 3D animation style.\\n\\n", clipAIRest.getScript(), clipAIRest.getTopic()))
                .append("REQUIREMENTS:\\n")
                .append("3D ANIMATION STYLE SPECIFICATIONS:\\n")
                .append("- Implement modern 3D animation aesthetics similar to Pixar/Disney/Sony Animation\\n")
                .append("- Characters must have: \\n")
                .append("  * Large, expressive eyes with detailed iris/pupil definition\\n")
                .append("  * Subtle subsurface scattering on skin for realistic translucency\\n")
                .append("  * Dynamic hair simulation with natural movement\\n")
                .append("  * Micro-detail in facial features and expressions\\n")
                .append("  * Physically accurate cloth simulation for clothing\\n")
//                .append("RENDERING SPECIFICATIONS:\\n")
//                .append("- Use physically based rendering (PBR) materials\\n")
//                .append("- Implement global illumination and ambient occlusion\\n")
//                .append("- Add volumetric lighting for atmosphere\\n")
//                .append("- Include motion blur for dynamic scenes\\n")
//                .append("- Apply depth of field effects for cinematic focus\\n")
                .append("- Match standard landscape format (16:9 aspect ratio)\\n\\n")
                .append("FORMAT SPECIFICATIONS:\\n")
                .append("Return a JSON array with the following structure:\\n")
                .append("{\\n")
                .append("  result: [\\n")
                .append("    {\\n")
                .append("      key_from_script: Detailed image prompt including technical specifications\\n")
                .append("    }\\n")
                .append("  ]\\n")
                .append("}\\n\\n")
                .append("KEY FORMATTING RULES:\\n")
                .append("- Keys must be exact script phrases (2-4 words)\\n")
                .append("- Words separated by underscores\\n")
                .append("- Keys must match script sequence\\n")
                .append("- Keys should be select such that there is 10-14 words in between two successive keys\\n\\n")
                .append("PROMPT STRUCTURE FOR EACH IMAGE:\\n")
                .append("- image type (disney animation, Pixar animation, Sony animation)\\n")
                .append("- Character details (expressions, poses, interactions)\\n")
                .append("- Lighting setup (key light, fill light, rim light, global illumination)\\n")
                .append("- Environmental details (atmosphere, particles, depth)\\n")
                .append("Example Format:\\n")
                .append("{\\n")
                .append("  result: [\\n")
                .append("    {\\n")
                .append("      student_at_desk: Create a modern 3D animated scene of a student at desk. Camera: 35mm focal length, eye-level angle, subtle dolly movement. Character: large expressive eyes with catch lights, detailed subsurface scattering on skin (0.5 radius), natural hair simulation with 10k strands. Materials: PBR-based wooden desk (medium roughness), fabric clothing simulation with micro-fiber detail. Lighting: key light from windows (5600K), bounce fill from mint green walls, rim light for separation. Environment: classroom setting with volumetric light rays, floating dust particles, 24mm depth of field on character. Post-processing: slight warm color grade (highlights: #FFE4B5), subtle chromatic aberration (0.5px), cinematic motion blur (180-degree shutter)\\n")
                .append("    }\\n")
                .append("  ]\\n")
                .append("}\\n\\n")
                .append("PROMPT MUST INCLUDE:\\n")
                .append("- add Disney style 3D animated image in all prompt\\n")
                .append("- image animation type. keep same for all image prompts\\n")
                .append("- Precise lighting setup with color temperatures\\n")
                .append("!!! Important: Keys must be direct quotes from script, maintaining exact chronological order and proper underscore formatting");
        return imagePromptTemplate.toString();
    }

    @Override
    protected String getPrompt(ClipAIRest clipAIRest) {
        return "";
    }
}
