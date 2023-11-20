from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
from openai import OpenAI
from pytube import YouTube
from newspaper import Article
import socket

def internet_connection():
    try:
        host = socket.gethostbyname("www.google.com")
        socket.create_connection((host, 80), timeout=5)
        return True
    except (socket.gaierror, socket.error):
        return False

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
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=['af', 'ak', 'sq', 'am', 'as', 'ay', 'az', 'bn', 'eu', 'be', 'bho', 'bs', 'bg', 'my', 'ca', 'ceb', 'co', 'hr', 'cs', 'da', 'dv', 'eo', 'et', 'ee', 'fil', 'fi', 'gl', 'lg', 'ka', 'el', 'gn', 'gu', 'ht', 'ha', 'haw', 'iw', 'hmn', 'hu', 'is', 'ig', 'id', 'ga', 'it', 'jv', 'kn', 'kk', 'km', 'rw', 'ko', 'kri', 'ku', 'ky', 'lo', 'la', 'lv', 'ln', 'lt', 'lb', 'mk', 'mg', 'ms', 'ml', 'mt', 'mi', 'mr', 'mn', 'ne', 'nso', 'no', 'ny', 'or', 'om', 'ps', 'fa', 'pl', 'pt', 'pa', 'qu', 'ro', 'ru', 'sm', 'sa', 'gd', 'sr', 'sn', 'sd', 'si', 'sk', 'sl', 'so', 'st', 'es', 'su', 'sw', 'sv', 'tg', 'ta', 'tt', 'ti', 'ts', 'tr', 'tk', 'uk', 'ur', 'ug', 'uz', 'vi', 'cy', 'fy', 'xh', 'yi', 'yo', 'zu', 'de', 'en'])
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
    client = OpenAI( api_key = key)
    if article == False:
        if language == "the same language as the ":
            language = language + "video"
        if length == 0:
            instructions = f"You will be provided with a transcript of a video, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points. The very short summary should be in {language}."
            max_tokens = 100
        elif length == 1:
            instructions = f"You will be provided with a transcript of a video, and your task is to generate a very short, concise summary with a maximum of 60 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
            max_tokens = 160
        else:
            instructions = f"You will be provided with a transcript of a video, and your task is to generate a short, concise summary with a maximum of 120 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
            max_tokens = 230
    else:
        if language == "the same language as the ":
            language = language + "article"
        if length == 0:
            instructions = f"You will be provided with an article, and your task is to generate a summary a very short, concise summary with a maximum of 20 word of the transcript in {language} using only 3 bullet points."
            max_tokens = 100
        elif length == 1:
            instructions = f"You will be provided with an article, and your task is to generate a very short, concise summary with a maximum of 60 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that as in the end."
            max_tokens = 160
        else:
            instructions = f"You will be provided with an article, and your task is to generate a short, concise summary with a maximum of 120 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that as in the end."
            max_tokens = 230

    try:
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": instructions},
                {"role": "user", "content": text}
            ],
            temperature=0.2,
            n=1,
            max_tokens=max_tokens,
            presence_penalty=0,
            frequency_penalty=0.1,
        )

        # Return the generated summary
        return response.choices[0].message.content.strip()

    except Exception as e:
        print(f"An error occurred with 'gpt-3.5-turbo': {str(e)}")

        try:
            # Attempt to generate a summary using "gpt-3.5-turbo-16k" if there was an error with the previous model
            response = client.chat.completions.create(
                model="gpt-3.5-turbo-16k",
                messages=[
                    {"role": "system", "content": instructions},
                    {"role": "user", "content": text}
                ],
                temperature=0.2,
                n=1,
                max_tokens=max_tokens,
                presence_penalty=0,
                frequency_penalty=0.1,
            )

            # Return the generated summary using "gpt-3.5-turbo-16k" with "16K" at the beginning
            generated_summary = response.choices[0].message.content.strip()
            return generated_summary

        except Exception as e:
            # If an error still occurs, handle it or return an error message
            print(f"An error occurred with 'gpt-3.5-turbo-16k': {str(e)}")
            raise e

def get_site_transcript(url: str) -> str:
    try:
        site = Article(url)
        site.download()
        site.parse()

        # Paywall detection
        pattern = r'"isAccessibleForFree"\s*:\s*"?false"?'
        match = re.search(pattern, site.html, re.IGNORECASE)
        if match:
            return "paywall detected"

        return site.text
    except Exception as e:
        return None

def summarize(url: str, key: str, length: int, language: str) -> str:
    """
    Summarize the provided YouTube video
    """

    try:
        # Content-Detection
        if url == "":
            raise Exception("no content")

        # Internet-Connection
        if not internet_connection():
            raise Exception("no internet")

        # Extract the video ID from the URL
        video_id = extract_youtube_video_id(url)

        # Error message: Invalid link
        if not video_id:
            transcript = get_site_transcript(url)
            if transcript is None or transcript == "":
                raise Exception("invalid link")
            elif transcript == "paywall detected":
                raise Exception("paywall detected")
            else:
                try:
                    summary = generate_summary(transcript, key, length, True, language)
                    return summary
                except Exception as e:
                    if "Incorrect API" in str(e):
                        raise Exception("incorrect api")
                    elif "You didn't provide an API key" in str(e):
                        raise Exception("no api")
                    else:
                        raise e

        # Fetch the video transcript
        transcript = get_video_transcript(video_id)

        # If no transcript is found, return an error message
        if not transcript:
            raise Exception("no transcript")

        try:
            summary = generate_summary(transcript, key, length, False, language)
        except Exception as e:
            if "Incorrect API" in str(e):
                raise Exception("incorrect api")
            elif "You didn't provide an API key" in str(e):
                raise Exception("no api")
            else:
                raise e

                # Return the summary
        return summary
    except Exception as e:
        raise e
