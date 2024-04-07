from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
from openai import OpenAI
from pytube import YouTube
from newspaper import Article
import socket
#import google.generativeai as genai
from groq import Groq
import random
import requests

def internet_connection():
    try:
        requests.get('https://www.wikipedia.org', timeout=5)
        return True
    except requests.ConnectionError:
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


def generate_summary(text: str, length: int, type: str, language: str, title: str, key: str, model: str) -> str:
    """
    Generate a summary of the provided text
    """

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
        promptText0 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 20 word of the text in {language} using only 3 bullet points."
        promptText1 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 60 words of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptText3 = f"You will be provided with a text and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument0 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 20 word of the document in {language} using only 3 bullet points."
        promptDocument1 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 60 words of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument3 = f"You will be provided with a document, and your task is to generate a summary of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."

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
        promptText0 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 20 word of the text in {language} using only 3 bullet points."
        promptText1 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 60 words of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptText3 = f"You will be provided with a text and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument0 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 20 word of the document in {language} using only 3 bullet points."
        promptDocument1 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 60 words of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument3 = f"You will be provided with a document, and your task is to generate a summary of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."

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
        promptText0 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 20 word of the text in {language} using only 3 bullet points."
        promptText1 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 60 words of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptText3 = f"You will be provided with a text and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument0 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 20 word of the document in {language} using only 3 bullet points."
        promptDocument1 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 60 words of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument3 = f"You will be provided with a document, and your task is to generate a summary of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."

    if title is not None:
        title = " with the title " + title
    else:
        title = ""


    if type == "video":
        if language == "the same language as the ":
            language = language + "transcript"
        if length == 0:
            instructions = promptVideo0
            max_tokens = 150
        elif length == 1:
            instructions = promptVideo1
            max_tokens = 200
        else:
            instructions = promptVideo3
            max_tokens = 400
    elif type == "article":
        if language == "the same language as the ":
            language = language + "article"
        if length == 0:
            instructions = promptArticle0
            max_tokens = 150
        elif length == 1:
            instructions = promptArticle1
            max_tokens = 200
        else:
            instructions = promptArticle3
            max_tokens = 400
    elif type == "text":
        if language == "the same language as the ":
            language = language + "text"
        if length == 0:
            instructions = promptText0
            max_tokens = 150
        elif length == 1:
            instructions = promptText1
            max_tokens = 200
        else:
            instructions = promptText3
            max_tokens = 400
    elif type == "document":
        if language == "the same language as the ":
            language = language + "document"
        if length == 0:
            instructions = promptDocument0
            max_tokens = 150
        elif length == 1:
            instructions = promptDocument1
            max_tokens = 200
        else:
            instructions = promptDocument3
            max_tokens = 400

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

def handle_exception(e: Exception) -> None:
    if "Incorrect API" in str(e) or "Invalid API Key" in str(e):
        raise Exception("incorrect key")
    elif "Rate limit reached" in str(e):
        raise Exception("rate limit")
    elif "You didn't provide an API key" or "APIConnectionError" in str(e):
        raise Exception("no key")
    elif "Please reduce the length of the messages" in str(e):
        raise Exception("too long")
    else:
        raise e

def summarize(url: str, length: int, language: str, key: str, model: str) -> str:
    try:
        # Content-Detection
        if url == "":
            raise Exception("no content")

        # Internet-Connection
        if not internet_connection():
            raise Exception("no internet")

        # Text ?
        if not url.startswith("http"):
            if len(url) > 100:
                if url.startswith("Document:"):
                    try:
                        summary = generate_summary(url, length, "document", language, None, key, model)
                        return summary
                    except Exception as e:
                        handle_exception(e)
                else:
                    try:
                        summary = generate_summary(url, length, "text", language, None, key, model)
                        return summary
                    except Exception as e:
                        handle_exception(e)
            else:
                raise Exception("invalid input")

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
                    summary = generate_summary(transcript, length, "article", language, title, key, model)
                    return summary
                except Exception as e:
                    handle_exception(e)

        # Fetch the video transcript
        transcript = get_video_transcript(video_id)

        # If no transcript is found, return an error message
        if not transcript:
            raise Exception("no transcript")

        try:
            summary = generate_summary(transcript, length, "video", language, title, key, model)
        except Exception as e:
            handle_exception(e)

        # Return the summary
        return summary
    except Exception as e:
        raise e
