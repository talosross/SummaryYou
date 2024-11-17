from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
from openai import OpenAI
from newspaper import Article
import socket
#import google.generativeai as genai
from groq import Groq
import random
import requests
from trafilatura import fetch_url, extract, extract_metadata

cache = {}

def internet_connection():
    try:
        requests.get('https://www.wikipedia.org', timeout=5)
        return True
    except requests.ConnectionError:
        return False

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

def get_video_title(url):
    try:
        # Get the webpage content
        response = requests.get(url)
        if response.status_code != 200:
            return "Error: Could not fetch video page"

        # Find title using regex
        title_match = re.search(r'<title>(.+?)</title>', response.text)
        if title_match:
            # Clean up the title (remove " - YouTube" suffix)
            title = title_match.group(1)
            title = title.replace(' - YouTube', '')
            return title
        return "Error: Title not found"

    except Exception as e:
        return f"An error occurred: {e}"

def get_channel_name(url):
    try:
        response = requests.get(url)
        if response.status_code != 200:
            return "Error: Could not fetch video page"

        # Look for channel name in meta tags
        channel_match = re.search(r'<link itemprop="name" content="(.+?)">', response.text)
        if channel_match:
            return channel_match.group(1)
        return "Error: Channel name not found"

    except Exception as e:
        return f"An error occurred: {e}"

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
        MODEL = genai.GenerativeModel('gemini-1.5-flash')

        # Prompts
        promptVideo0 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points introduced by dashes. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Please answer in {language}!!!!!!!!!!" #Done
        promptVideo1 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise and compact summary with a maximum of 40 words of the transcript. If it includes a conclusion or key takeaway, make sure to include that in the end. Your answer should be in {language}!!!!!!!!!!!!1" #Done - just working with "1"
        promptVideo3 = f"You will be provided with a transcript of the video with{title}, and your task is to generate a summary of the transcript in 130 words. If it includes a conclusion or key takeaway, make sure to include that in the end. Don't use the prefix 'summary' or 'conclusion'. Your answer should be in {language}!!!!!!!!!!!!1" #Done
        promptArticle0 = f"You will be provided with the article{title}, and your task is to summarize it in 3 very short and concise bullet points. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Summarize it in {language}!" #Done in German
        promptArticle1 = f"You will be provided with the article{title}, and your task is to generate a very short, concise and compact summary with a maximum of 50 words of the text. If it includes a conclusion or key takeaway, make sure to include that in the end. Summarize it in {language}!" #Done in German
        promptArticle3 = f"You will be provided with the article{title}, and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done in German
        promptText0 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 20 word of the text in {language} using only 3 bullet points."
        promptText1 = f"You will be provided with a text and your task is to generate a very short, concise summary with a maximum of 60 words of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptText3 = f"You will be provided with a text and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument0 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 20 word of the document in {language} using only 3 bullet points."
        promptDocument1 = f"You will be provided with a document, and your task is to generate a very short, concise summary with a maximum of 60 words of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."
        promptDocument3 = f"You will be provided with a document, and your task is to generate a summary of the document in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end."

    elif model == "Groq":
        # Initialize Groq
        client = Groq(api_key=key)
        MODEL = "llama-3.1-8b-instant"

        # Prompts
        promptVideo0 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise summary with a maximum of 20 words of the transcript using only 3 bullet points introduced by dashes. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Please answer in {language}!!!!!!!!!!" #Done
        promptVideo1 = f"You will be provided with a transcript of the video{title}, and your task is to generate a very short, concise and compact summary with a maximum of 40 words of the transcript. If it includes a conclusion or key takeaway, make sure to include that in the end. Your answer should be in {language}!!!!!!!!!!!!1" #Done - just working with "1"
        promptVideo3 = f"You will be provided with a transcript of the video with{title}, and your task is to generate a summary of the transcript in 130 words. If it includes a conclusion or key takeaway, make sure to include that in the end. Don't use the prefix 'summary' or 'conclusion'. Your answer should be in {language}!!!!!!!!!!!!1" #Done
        promptArticle0 = f"You will be provided with the article{title}, and your task is to summarize it in 3 very short and concise bullet points. Every bullet point should be a maximum of 5 words, start with a hyphen and not be a full sentences. Summarize it in {language}!" #Done in German
        promptArticle1 = f"You will be provided with the article{title}, and your task is to generate a very short, concise and compact summary with a maximum of 50 words of the text. If it includes a conclusion or key takeaway, make sure to include that in the end. Summarize it in {language}!" #Done in German
        promptArticle3 = f"You will be provided with the article{title}, and your task is to generate a summary of the text in {language}. If it includes a conclusion or key takeaway, make sure to include that in the end." #Done in German
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
            max_tokens = 400
        elif length == 1:
            instructions = promptVideo1
            max_tokens = 600
        else:
            instructions = promptVideo3
            max_tokens = 800
    elif type == "article":
        if language == "the same language as the ":
            language = language + "article"
        if length == 0:
            instructions = promptArticle0
            max_tokens = 400
        elif length == 1:
            instructions = promptArticle1
            max_tokens = 600
        else:
            instructions = promptArticle3
            max_tokens = 800
    elif type == "text":
        if language == "the same language as the ":
            language = language + "text"
        if length == 0:
            instructions = promptText0
            max_tokens = 400
        elif length == 1:
            instructions = promptText1
            max_tokens = 600
        else:
            instructions = promptText3
            max_tokens = 800
    elif type == "document":
        if language == "the same language as the ":
            language = language + "document"
        if length == 0:
            instructions = promptDocument0
            max_tokens = 400
        elif length == 1:
            instructions = promptDocument1
            max_tokens = 600
        else:
            instructions = promptDocument3
            max_tokens = 800

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
            response = MODEL.generate_content(prompt)
            return response

    except Exception as e:
        """
        print("Text is too long.")
        """
        print(f"An error occurred with '{MODEL}': {str(e)}")
        raise e

def handle_exception(e: Exception) -> None:
    if "Incorrect API" in str(e) or "Invalid API Key" in str(e):
        raise Exception("incorrect key")
    elif "Rate limit reached" in str(e):
        raise Exception("rate limit")
    elif "You didn't provide an API key" in str(e):
        raise Exception("no key")
    elif "Please reduce the length of the messages" in str(e):
        raise Exception("too long")
    else:
        raise e

def summarize(url: str, length: int, language: str, key: str, model: str) -> dict:
    result = {}
    result['title'] = None
    result['author'] = None
    result['summary'] = None
    title = None
    transcript = None
    try:
        # Content-Detection
        if url == "":
            raise Exception("no content")

        # Internet-Connection
        if not internet_connection():
            raise Exception("no internet")

        if url in cache:
            print("Cache hit")
            result['title'] = cache[url]['title']
            result['author'] = cache[url]['author']
            transcript = cache[url]['transcript']
        else:
            # Text
            if not url.startswith("http"):
                if len(url) > 100:
                    if url.startswith("Document:"):
                        try:
                            summary = generate_summary(url, length, "document", language, None, key, model)
                            result['summary'] = summary
                            return result
                        except Exception as e:
                            handle_exception(e)
                    else:
                        try:
                            summary = generate_summary(url, length, "text", language, None, key, model)
                            result['summary'] = summary
                            return result
                        except Exception as e:
                            handle_exception(e)
                else:
                    raise Exception("too short")

            # Extract the video ID from the URL
            video_id = extract_youtube_video_id(url)

            # Video
            if video_id:
                try:
                    # Get the transcript languages
                    transcript_list = YouTubeTranscriptApi.list_transcripts(video_id)
                    language = [transcript.language_code for transcript in transcript_list]

                    # Get the transcript
                    text = YouTubeTranscriptApi.get_transcript(video_id, languages=[language[0]])
                    transcript = " ".join([line["text"] for line in text])

                    # Get the title of the video
                    title = get_video_title(url)
                    result['title'] = title

                    # Get the author of the video
                    author = get_channel_name(url)
                    if author == "unknown":
                        author = None
                    result['author'] = author

                except TranscriptsDisabled:
                    raise Exception("no transcript")

                except Exception as e:
                    handle_exception(e)

            # Article
            else:
                try:
                    site = fetch_url(url)

                    # Paywall detection
                    pattern = r'"isAccessibleForFree"\s*:\s*"?false"?'

                    if site == None:
                        site = Article(url)
                        site.download()
                        site.parse()
                        match = re.search(pattern, site.html, re.IGNORECASE)
                        if match:
                            raise Exception("paywall detected")

                        transcript = site.text

                        if transcript is None or transcript == "":
                            raise Exception("invalid link")

                        # Get title
                        title = site.title
                        result['title'] = title

                        # Get author
                        authors = site.authors
                        author = None
                        if author:
                            for element in authors:
                                if element and len(element.split()) > 0:
                                    author = element
                                    break

                        result['author'] = author

                    else:
                        match = re.search(pattern, site, re.IGNORECASE)
                        if match:
                            raise Exception("paywall detected")

                        transcript = extract(site)

                        if transcript is None or transcript == "":
                            raise Exception("invalid link")

                        # Get title
                        metadata = extract_metadata(site)
                        title = metadata.title
                        result['title'] = title

                        # Get author
                        author = metadata.author
                        result['author'] = author

                except Exception as e:
                    handle_exception(e)

            cache[url] = {'title': result['title'], 'author': result['author'], 'transcript': transcript}

        # Generate the summary
        video_id = extract_youtube_video_id(url)

        # Video
        if video_id:
            summary = generate_summary(transcript, length, "video", language, title, key, model)
        else:
            summary = generate_summary(transcript, length, "article", language, title, key, model)
        result['summary'] = summary
        return result

    except Exception as e:
        raise e