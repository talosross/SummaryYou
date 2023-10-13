from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
import openai
from pytube import YouTube
from newspaper import Article

def get_title(url):
    try:
        # YouTube-Video-Objekt erstellen
        yt_video = YouTube(url)

        # Titel des Videos abrufen
        video_title = yt_video.title

        return video_title
    except Exception as e:
        try:
            site = Article(url)
            site.download()
            site.parse()
            return site.title
        except Exception as e:
            return None


def get_author(url):
    try:
        # YouTube-Video-Objekt erstellen
        yt_video = YouTube(url)

        # Autor des Videos abrufen
        video_author = yt_video.author
        if video_author == "unknown":
            try:
                site = Article(url)
                site.download()
                site.parse()
                authors = site.authors
                for element in authors:
                    if element and len(element.split()) > 0:
                        # Wenn das Element nicht leer ist und mindestens ein Wort enthält
                        # Zeige das Element und beende die Schleife
                        return element
                return None
            except Exception as e:
                return None
        else:
            return video_author
    except VideoUnavailable:
        try:
            site = Article(url)
            site.download()
            site.parse()
            authors = site.authors
            for element in authors:
                if element and len(element.split()) > 0:
                    # Wenn das Element nicht leer ist und mindestens ein Wort enthält
                    # Zeige das Element und beende die Schleife
                    return element
            return None
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
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=['de', 'de-DE', 'en'])
    except TranscriptsDisabled:
        # The video doesn't have a transcript
        return None

    text = " ".join([line["text"] for line in transcript])
    return text or "Fehler"

def generate_summary(text: str, key: str, length: int, article: bool, language: str) -> str:
    """
    Generate a summary of the provided text using OpenAI API
    """
    # Initialize the OpenAI API client
    openai.api_key = key
    if article == False:
        if length == 0:
            instructions = "Summarize the transcript of this YouTube video in three short bullet points. If the video has a conclusion include it. In" + language
        elif length == 1:
            instructions = "Provide a concise summary of the transcript from a YouTube video. If the video includes a conclusion or key takeaway, make sure to include that as well. In" + language
        else:
            instructions = "Summarize the transcript of the video mentioned above. Begin with an overview. Next, list the top five key highlights. Finally, briefly encapsulate the main message of the video. Maintaining objectivity, and employing appropriate writing style. In" + language
    else:
        if length == 0:
            instructions = "Summarize the article in three concise, very short bullet points. If the article includes a conclusion, provide that as well. In" + language
        elif length == 1:
            instructions = "Provide a concise summary of this article. If it includes a conclusion or key takeaway, make sure to include that as well. In" + language
        else:
            instructions = "Summarize the article mentioned above. Begin with an overview. Next, list the top five key highlights. Finally, briefly encapsulate the main message of the video. Maintaining objectivity, and employing appropriate writing style." + language

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
            return f"Ein Fehler ist aufgetreten, und keine Zusammenfassung konnte generiert werden.{str(e)}"

def get_site_transcript(url: str) -> str:
    try:
        site = Article(url)
        site.download()
        site.parse()
        return site.text
    except Exception as e:
        return None

def summarize(url: str, key: str, length: int, language: str) -> str:
    """
    Summarize the provided YouTube video
    """
    # Extract the video ID from the URL
    video_id = extract_youtube_video_id(url)

    #Error message: Invalid link
    if not video_id:
        transcript = get_site_transcript(url)
        if transcript == None or transcript == "":
            return "invalid link"
        else:
            summary = generate_summary(transcript, key, length, True, language)
            return summary

    # Fetch the video transcript
    transcript = get_video_transcript(video_id)

    # If no transcript is found, return an error message
    if not transcript:
        return f"Keine Untertitel für diese Video gefunden: {url}"

    # Generate the summary
    summary = generate_summary(transcript, key, length, False, language)

    # Return the summary
    return summary