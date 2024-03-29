from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
from openai import OpenAI
from pytube import YouTube
from newspaper import Article
import socket
#import google.generativeai as genai
from groq import Groq
import random

def internet_connection():
    try:
        host = socket.gethostbyname("www.google.com")
        socket.create_connection((host, 80), timeout=5)
        return True
    except (socket.gaierror, socket.error):
        return False

def get_title(url):
    try:
        yt_video = YouTube(url)
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
        # Create YouTube video object
        yt_video = YouTube(url)

        # Get the author of the video
        video_author = yt_video.author
        if video_author == "unknown":
            try:
                site = Article(url)
                site.download()
                site.parse()
                authors = site.authors
                for element in authors:
                    if element and len(element.split()) > 0:
                        # If the element is not empty and contains at least one word
                        # Show the element and end the loop
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
                    # If the element is not empty and contains at least one word
                    # Show the element and end the loop
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
        # Get the transcript languages
        transcript_list = YouTubeTranscriptApi.list_transcripts(video_id)
        language = [transcript.language_code for transcript in transcript_list]
        # Get the transcript
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=[language[0]])
    except TranscriptsDisabled:
        # The video doesn't have a transcript
        return None
    text = " ".join([line["text"] for line in transcript])
    return text


def generate_summary(text: str, key: str, length: int, article: bool, language: str, title: str) -> str:
    """
    Generate a summary of the provided text
    """
    model = "OpenAI" # "OpenAI" or "Google"

    if model == "OpenAI":
        # Initialize the OpenAI API client
        client = OpenAI(api_key=key)
        MODEL = "gpt-3.5-turbo"

        # Prompts
        promptVideo0 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points. The very short summary should be in {language}." # Done
        promptVideo1 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 60 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done
        promptVideo3 = f"You will be provided with a transcript of the video with{title}, and your task is to generate a summary of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done
        promptArticle0 = f"You will be provided with the article{title}, and your task is to generate a very short, concise summary with a maximum of 20 word of the text in {language} using only 3 bullet points." #Done
        promptArticle1 = f"You will be provided with the article{title}, and your task is to generate a very short, concise summary with a maximum of 60 words of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done
        promptArticle3 = f"You will be provided with the article{title}, and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done

    elif model == "Gemini":
        # Initialize Gemini
        genai.configure(api_key=key)
        model = genai.GenerativeModel(model_name='gemini-pro-vision')

        # Prompts
        promptVideo0 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points. The very short summary should be in {language}."
        promptVideo1 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 60 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptVideo3 = f"You will be provided with a transcript of the video with{title}, and your task is to generate a summary of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptArticle0 = f"You will be provided with the article{title}, and your task is to generate a very short, concise summary with a maximum of 20 word of the transcript in {language} using only 3 bullet points."
        promptArticle1 = f"You will be provided with the article{title}, and your task is to generate a very short, concise summary with a maximum of 60 words of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptArticle3 = f"You will be provided with the article{title}, and your task is to generate a summary of the transcript in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."

    elif model == "Groq":
        # Initialize Groq
        client = Groq(api_key=key)
        MODEL = "mixtral-8x7b-32768"

        # Prompts
        promptVideo0 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points introduced by dashes. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Please answer in {language}!!!!!!!!!!" #Done
        promptVideo1 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise and compact summary with a maximum of 40 words of the transcript. If it includes a conclusion or key takeaway, make sure to include that in the end. Your answer should be in {language}!!!!!!!!!!!!1" #Done - just working with "1"
        promptVideo3 = f"You will be provided with a transcript of the video with{title}, and your task is to generate a compact summary of the transcript in 130 words. If it includes a conclusion or key takeaway, make sure to include that in the end. Don't use the prefix 'summary' or 'conclusion'. Your answer should be in {language}!!!!!!!!!!!!1" #Done
        promptArticle0 = f"You will be provided with the article{title}, and your task is to summarize it in 3 very short and concise bullet points. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Summarize it in {language}!" #Done in German
        promptArticle1 = f"You will be provided with the article{title}, and your task is to generate a very short, concise and compact summary with a maximum of 50 words of the text. If it includes a conclusion or key takeaway, make sure to include that in the end. Summarize it in {language}!" #Done in German
        promptArticle3 = f"You will be provided with the article{title}, and your task is to generate a compact summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done in German

    if title is not None:
        title = " with the title " + title
    else:
        title = ""


    if article == False:
        if language == "the same language as the ":
            language = language + "transcript"
        if length == 0:
            instructions = promptVideo0
            max_tokens = 110
        elif length == 1:
            instructions = promptVideo1
            max_tokens = 170
        else:
            instructions = promptVideo3
            max_tokens = 350
    else:
        if language == "the same language as the ":
            language = language + "article"
        if length == 0:
            instructions = promptArticle0
            max_tokens = 110
        elif length == 1:
            instructions = promptArticle1
            max_tokens = 180
        else:
            instructions = promptArticle3
            max_tokens = 350

    try:
        if model == "OpenAI" or model == "Groq":
            seed = random.randint(0, 1000000)

            response = client.chat.completions.create(
                model=MODEL,
                messages=[
                    {"role": "system", "content": instructions},
                    {"role": "user", "content": text}
                ],
                seed=seed,
                temperature=0.2,
                n=1,
                max_tokens=max_tokens,
                presence_penalty=0,
                frequency_penalty=0.1,
            )

            # Return the generated summary
            return response.choices[0].message.content.strip()

        elif model == "Gemini":
            prompt = instructions + text
            response = model.generate_content(prompt)
            return response

    except Exception as e:
        """
        print("Text is too long.")
        """
        print(f"An error occurred with '{MODEL}': {str(e)}")



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

        title = get_title(url)

        # Error message: Invalid link
        if not video_id:
            transcript = get_site_transcript(url)
            if transcript is None or transcript == "":
                raise Exception("invalid link")
            elif transcript == "paywall detected":
                raise Exception("paywall detected")
            else:
                try:
                    summary = generate_summary(transcript, key, length, True, language, title)
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
            summary = generate_summary(transcript, key, length, False, language, title)
        except Exception as e:
            if "Incorrect API" in str(e):
                raise Exception("incorrect api")
            elif "You didn't provide an API key" in str(e):
                raise Exception("no api")
            elif "Please reduce the length of the messages" in str(e):
                raise Exception("too long")
            else:
                raise e

                # Return the summary
        return summary
    except Exception as e:
        raise e
