package com.example.ClipAI.service.chat;

import com.example.ClipAI.model.ClipAIRest;
import org.springframework.stereotype.Service;

@Service
public class ShortsGeminiServiceImpl extends AbstractGeminiService implements ShortsGeminiService {

    @Override
    public String getImagesPrompt(ClipAIRest clipAIRest) {
        String script = clipAIRest.getScript();
        return "%s Generate 13 detailed cinematic image descriptions for a YouTube Shorts video about %s. Each image should match specific moments in the script and create maximum visual impact.->REQUIREMENTS: - Create photorealistic, high-resolution image prompts that include camera angles and style specifications - Match vertical video format (9:16 aspect ratio) - Focus on dramatic, attention-grabbing visuals - Maintain chronological order matching script flow - Include detailed environment and atmosphere descriptions - Generate most detailed image     FORMAT SPECIFICATIONS: Return a JSON array with the following structure:{  result: [     {      key_from_script: Detailed image prompt including camera angle and style    }  ]}        KEY FORMATTING RULES:  - Keys must be exact script phrases (2-4 words) - Words separated by underscores - Keys must match script sequence - No generic or unmatched keys - Keys should be select such that there is 10-14 words in between two successive keys      PROMPT STRUCTURE FOR EACH IMAGE: - Start with camera angle/perspective - Describe main subject and action - Include lighting and atmosphere - Add environmental details - Specify style and artistic elements - do not include double quotes in prompt if needed use single quotes Example Format: {  result: [    {       temple_in_the_mist: High-angle aerial shot of ancient Greek temple ruins nestled among towering cliffs at dawn, with mist swirling around the structure, creating a mystical, dreamlike atmosphere. Soft, ethereal light filters through the mist, casting gentle rays over the scene. The temple's weathered marble columns and intricate carvings stand out against the dramatic cliff faces, while wisps of mist drift upward. Photorealistic detail with a cinematic, fantasy style, capturing the ancient beauty and solitude of the location     }   ] }        PROMPT MUST INCLUDE: - Camera angle (High Angle, underwater, close-up, Dutch angle, Eye Level,Low Angle,Worms-Eye View,Rack Focus) - Use camera angles as per its meaning and script line on which image is generated - Lighting description (golden hour, storm lighting, underwater caustics) - Atmospheric elements (mist, particles, reflections) - Time of day and weather conditions - Foreground and background elements - Scale references for size perspective - Artistic style (photorealistic, cinematic, dramatic) !!! Important: Keys must be direct quotes from script, maintaining exact chronological order and proper underscore formatting".formatted(script, clipAIRest.getTopic());
    }

    @Override
    public String getPrompt(ClipAIRest clipAIRest) {
        return """
                Generate a 80-seconds YouTube Shorts script about %s. The script should: FORMAT REQUIREMENTS: - Maximum 400 words - Write in plain text without quotation marks - No speaker/narrator labels - No timestamps or technical directions - One continuous paragraph - Avoid special characters that could break JSON - do not entre new line at end of script- Clearly define the topic, ensuring its attention-grabbing and intriguing. - Request a dramatic, suspenseful, and hyperbolic tone for the narrative. - Ask for a shocking or striking opening statement to immediately hook the audience. - Highlight specific details about the subject, such as size, abilities, and unique traits. - Include speculative or mysterious claims to provoke curiosity and engagement. - Emphasize the potential threat or urgency related to the subject. - use example of real incidence related to topic for proff and relevance- Conclude with a strong call to action, encouraging the audience to interact (e.g., comment, share, or like). - Ensure short, impactful sentences are used to maintain a fast-paced, engaging style.      HOOK STRUCTURE: - Headline-worthy Opener->Starts with a striking, fear-inducing statement (A Kodiak Kraken has just been spotted).    Structure-> 1)Introduction: Briefly establishes the subject with a shocking premise (spotting a terrifying creature). 1)Body: Expands on details, describing the creature’s unique traits, actions, and potential threats in a sensational manner. 3)Engagement Hook: Ends with a prompt for interaction, motivating readers to engage with the content.            Emphasis on Numbers and Scale->Highlights measurements like (60 feet in size) and (30 feet long tentacles) to emphasize the creatures enormity and power.       Informal and Conversational Tone->Phrases like (If you want to see) and (then just press on) add an approachable, conversational vibe that appeals to a casual audience. - End with an conclusion of whole script or call to action       WRITING STYLE: - Use vivid, sensory language - Keep sentences short and punchy - Include rhetorical questions - Use power words: mysterious, shocking, incredible, hidden, secret - Create tension through pacing - Target 13-25 second reading time        EXAMPLE OUTPUT:     A Kodiak Kraken has just been spotted in the ocean just off the coast of America, and it is absolutely terrifying. An enormous kraken-like creature has been reported completely strangling creatures as big as blue whales and great white sharks to death in a matter of seconds. What makes the Kodiak Kraken so different from a normal kraken is how much more deadly it is. A normal kraken is known for destroying ships and planes near the Bermuda Triangle but usually stays clear of other animals in the ocean. However, the Kodiak Kraken has no mercy. Anything that gets in its way, it sees as a threat, and its survival instincts tell it to kill the threat as fast as possible.This creature has been recorded at around 60 feet in size, with its tentacles recorded at lengths of 30 feet long. This means that this creature can grasp things such as other creatures, boats, and planes from extreme distances, and once its tentacles have a grip, it is almost impossible to escape. This creature is the main suspect for the disappearance of the USS Cyclops, and if it is not put to an end soon, these ocean predators such as sharks and killer whales may go extinct, as they stand no chance against the Kodiak Kraken. If you want to see this creature strangling a great white shark, which is very graphic, but if you want to see it, then just press on share, on more, and then comment, Kodiak Kraken.
                """
                .formatted(clipAIRest.getTopic());
    }
}
