from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled
import re
#from openai import OpenAI
from newspaper import Article
import socket
from groq import Groq
import random
import requests
from trafilatura import fetch_url, extract, extract_metadata
from java import jclass

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
        MODEL = "gemini-2.0-flash"
        # Prompts
        promptVideo0 = f"Act as an expert content summarizer. Extract exactly 3 key points from the video transcript '{title}'. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptVideo1 = f"Act as a professional summarizer. Condense the video transcript '{title}' into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptVideo3 = f"Act as a content analysis specialist. Create a detailed summary of the video transcript '{title}' using exactly 130 words. Cover the main topic, key points, and any conclusions in a cohesive narrative. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the 130-word summary in {language}."

        promptArticle0 = f"Act as an expert content summarizer. Extract exactly 3 key points from the article '{title}'. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptArticle1 = f"Act as a professional summarizer. Condense the article '{title}' into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptArticle3 = f"Act as a content analysis specialist. Create a comprehensive summary of the article '{title}' that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

        promptText0 = f"Act as an expert content summarizer. Extract exactly 3 key points from this text. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptText1 = f"Act as a professional summarizer. Condense this text into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptText3 = f"Act as a content analysis specialist. Create a comprehensive summary of this text that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

        promptDocument0 = f"Act as an expert content summarizer. Extract exactly 3 key points from this document. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptDocument1 = f"Act as a professional summarizer. Condense this document into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptDocument3 = f"Act as a content analysis specialist. Create a comprehensive summary of this document that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

    elif model == "Groq":
        # Initialize Groq
        client = Groq(api_key=key)
        MODEL = "llama-3.1-8b-instant"

        # Prompts
        promptVideo0 = f"Act as an expert content summarizer. Extract exactly 3 key points from the video transcript '{title}'. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptVideo1 = f"Act as a professional summarizer. Condense the video transcript '{title}' into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptVideo3 = f"Act as a content analysis specialist. Create a detailed summary of the video transcript '{title}' using exactly 130 words. Cover the main topic, key points, and any conclusions in a cohesive narrative. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the 130-word summary in {language}."

        promptArticle0 = f"Act as an expert content summarizer. Extract exactly 3 key points from the article '{title}'. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptArticle1 = f"Act as a professional summarizer. Condense the article '{title}' into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptArticle3 = f"Act as a content analysis specialist. Create a comprehensive summary of the article '{title}' that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

        promptText0 = f"Act as an expert content summarizer. Extract exactly 3 key points from this text. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptText1 = f"Act as a professional summarizer. Condense this text into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptText3 = f"Act as a content analysis specialist. Create a comprehensive summary of this text that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

        promptDocument0 = f"Act as an expert content summarizer. Extract exactly 3 key points from this document. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in {language}."

        promptDocument1 = f"Act as a professional summarizer. Condense this document into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in {language}."

        promptDocument3 = f"Act as a content analysis specialist. Create a comprehensive summary of this document that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in {language}."

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
            GeminiHandler = jclass("com.talosross.summaryyou.GeminiHandler")
            return GeminiHandler.generateContentSync(
                        key,
                        instructions,
                        text
                    )

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