import asyncio
import argparse
from edge_tts import Communicate

async def quick_fast_tts(text: str):
    voice = "en-US-ChristopherNeural"
    speed = "+20%"  # 75% faster than normal

    communicate = Communicate(text, voice, rate=speed)
    await communicate.save("C:\\Users\\pansu\\OneDrive\\Desktop\\study\\youtube\\ClipAi\\src\\main\\resources\\static\\narration.mp3")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Text-to-Speech with Fast Speed")
    parser.add_argument("text", help="Text to be converted to speech")

    args = parser.parse_args()

    # Run the TTS function with the provided text argument
    asyncio.run(quick_fast_tts(args.text))
