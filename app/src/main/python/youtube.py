from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
import openai
from pytube import YouTube

def get_youtube_video_title(youtube_url):
    try:
        # YouTube-Video-Objekt erstellen
        yt_video = YouTube(youtube_url)

        # Titel des Videos abrufen
        video_title = yt_video.title

        return video_title
    except Exception as e:
        return None

def get_youtube_video_author(youtube_url):
    try:
        # YouTube-Video-Objekt erstellen
        yt_video = YouTube(youtube_url)

        # Autor des Videos abrufen
        video_author = yt_video.author

        return video_author
    except Exception as e:
        return None

def extract_youtube_video_id(url: str) -> str:
    """
    Extract the video ID from the URL
    https://www.youtube.com/watch?v=XXX -> XXX
    https://youtu.be/XXX -> XXX
    """
    found = re.search(r"(?:youtu\.be\/|watch\?v=)([\w-]+)", url)
    if found:
        return found.group(1)
    return None

def get_video_transcript(video_id: str) -> str:
    """
    Fetch the transcript of the provided YouTube video
    """
    try:
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=['de', 'en'])
    except TranscriptsDisabled:
        # The video doesn't have a transcript
        return None

    text = " ".join([line["text"] for line in transcript])
    return text or "Fehler"

def generate_summary(text: str, key: str) -> str:
    """
    Generate a summary of the provided text using OpenAI API
    """
    # Initialize the OpenAI API client
    openai.api_key = key

    # Use GPT to generate a summary
    instructions = "Fasse das oben genannte Transkript eines Videos zusammen. Gib dafür zuerst eine Zusammenfassung. Liste anschließend die fünf wichtigsten Höhepunkte auf. Zum Schluss fasst du die Kernaussage des Videos kurz zusammen. Schreibe in Deutsch, bleibe außerdem objektiv und benutze einen angemessenen Schreibstil."

    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": instructions},
                {"role": "user", "content": text}
            ],
            temperature=0.2,
            n=1,
            max_tokens=1000,
            presence_penalty=0,
            frequency_penalty=0.1,
        )

        # Return the generated summary
        return response.choices[0].message.content.strip()

    except Exception as e:
        print(f"An error occurred with 'gpt-3.5-turbo': {str(e)}")

        try:
            # Attempt to generate a summary using "gpt-3.5-turbo-16k" if there was an error with the previous model
            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo-16k",
                messages=[
                    {"role": "system", "content": instructions},
                    {"role": "user", "content": text}
                ],
                temperature=0.2,
                n=1,
                max_tokens=1000,
                presence_penalty=0,
                frequency_penalty=0.1,
            )

            # Return the generated summary using "gpt-3.5-turbo-16k" with "16K" at the beginning
            generated_summary = response.choices[0].message.content.strip()
            return f"16K: {generated_summary}"

        except Exception as e:
            # If an error still occurs, handle it or return an error message
            print(f"An error occurred with 'gpt-3.5-turbo-16k': {str(e)}")
            return "Ein Fehler ist aufgetreten, und keine Zusammenfassung konnte generiert werden."


def summarize_youtube_video(video_url: str, key: str) -> str:
    """
    Summarize the provided YouTube video
    """
    # Extract the video ID from the URL
    video_id = extract_youtube_video_id(video_url)

    #Fehlerausgabe ungültiger Link
    if not video_id:
        return f"Ungültiger Link"

    # Fetch the video transcript
    transcript = get_video_transcript(video_id)

    # If no transcript is found, return an error message
    if not transcript:
        return f"Keine Untertitel für diese Video gefunden: {video_url}"

    # Generate the summary
    summary = generate_summary(transcript, key)

    # Return the summary
    return summary