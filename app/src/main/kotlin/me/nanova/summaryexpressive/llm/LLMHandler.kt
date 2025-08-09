package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import android.content.Context
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.llm.tools.Article
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.File
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscript
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.model.ExtractedContent
import me.nanova.summaryexpressive.model.SummaryData

@Serializable
data class SummaryOutput(
    override val title: String,
    override val author: String,
    override val summary: String,
    val isYoutubeLink: Boolean,
    val length: SummaryLength
) : SummaryData

class LLMHandler(private val context: Context, httpClient: HttpClient) {
    private val fileExtractorTool: FileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)

    fun createSummarizationAgent(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String? = null,
        modelName: String? = null,
        summaryLength: SummaryLength = SummaryLength.MEDIUM,
        language: String,
    ): AIAgent<String, SummaryOutput> {
        val executor = createExecutor(provider, apiKey, baseUrl)

        val articleToolRegistry = ToolRegistry { tool(articleExtractorTool) }
        val videoToolRegistry = ToolRegistry { tool(youTubeTranscriptTool) }
        val fileToolRegistry = ToolRegistry { tool(fileExtractorTool) }
        val tools = articleToolRegistry + videoToolRegistry + fileToolRegistry

        val agentConfig = createAgentConfig(provider, modelName, summaryLength, language)

        return AIAgent(
            promptExecutor = executor,
            strategy = createSummarizationStrategy(
                articleExtractorTool,
                youTubeTranscriptTool,
                fileExtractorTool,
                summaryLength
            ),
            agentConfig = agentConfig,
            toolRegistry = tools
        )
    }

    private fun createExecutor(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
    ): PromptExecutor {
        return when (provider) {
            AIProvider.OPENAI -> createOpenAIExecutor(apiKey, baseUrl)
            AIProvider.GEMINI -> simpleGoogleAIExecutor(apiKey)
            AIProvider.GROQ -> createGroqExecutor(apiKey)
        }
    }

    private fun createAgentConfig(
        provider: AIProvider,
        modelName: String?,
        summaryLength: SummaryLength,
        language: String,
    ): AIAgentConfig {
        val llmModel = when (provider) {
            // FIXME
            AIProvider.OPENAI -> (modelName?.let { OpenAIModels.Chat.GPT4_1 }
                ?: OpenAIModels.Chat.GPT4o)

            AIProvider.GEMINI -> (modelName?.let { GoogleModels.Gemini2_5Pro }
                ?: GoogleModels.Gemini2_5Flash)

            AIProvider.GROQ -> OpenAIModels.Chat.GPT4o
//            (
//                modelName ?: "llama3-8b-8192"
//            )
        }

        return AIAgentConfig(
            prompt = createSummarizationPrompt(summaryLength, language),
            model = llmModel,
            maxAgentIterations = 10,
        )
    }

    private fun createOpenAIExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = if (baseUrl.isNullOrBlank()) {
            OpenAILLMClient(apiKey)
        } else {
            OpenAILLMClient(
                apiKey,
                settings = OpenAIClientSettings(baseUrl = baseUrl)
            )
        }
        return SingleLLMPromptExecutor(client)
    }

    private fun createGroqExecutor(apiKey: String): PromptExecutor {
        val client = OpenAILLMClient(
            apiKey,
            settings = OpenAIClientSettings(baseUrl = "https://api.groq.com/openai/v1")
        )
        return SingleLLMPromptExecutor(client)
    }

    private val isYouTubeUrl = { input: String -> YouTubeTranscriptTool.isYouTubeLink(input) }
    private val isWebUrl = { input: String -> input.startsWith("http") && !isYouTubeUrl(input) }
    private val isFileUri =
        { input: String -> input.startsWith("content://") || input.startsWith("file://") }
    private val isPlainText =
        { input: String -> !isWebUrl(input) && !isYouTubeUrl(input) && !isFileUri(input) && input.isNotBlank() }

    private fun createSummarizationStrategy(
        articleExtractorTool: ArticleExtractorTool,
        youTubeTranscriptTool: YouTubeTranscriptTool,
        fileExtractorTool: FileExtractorTool,
        summaryLength: SummaryLength,
    ): AIAgentStrategy<String, SummaryOutput> =
        strategy("summarization_router_strategy")
        {
            var preparedContentHolder: ExtractedContent? = null
            var isYoutube = false

            val nodeAnalyzeInput by node<String, String>("analyze_input") { input ->
                isYoutube = isYouTubeUrl(input)
                input
            }

            // FIXME: the declaration of tool node should run execute manually
            val nodeExtractArticle by node<String, ExtractedContent>("extract_article") { url ->
                articleExtractorTool.execute(Article(url))
            }

            val nodeExtractVideo by node<String, ExtractedContent>("extract_video") { url ->
                youTubeTranscriptTool.execute(YouTubeTranscript(url))
            }

            val nodeExtractFile by node<String, ExtractedContent>("extract_file") { uriString ->
                fileExtractorTool.execute(File(uriString))
            }

            val nodePreparePlainText by node<String, ExtractedContent>("prepare_plain_text") { text ->
                ExtractedContent("Text Input", "Unknown", text)
            }

            val nodePrepareForLLM by node<ExtractedContent, String>("prepare_for_llm") { ec ->
                if (ec.title == "Error") {
                    ec.content // This is the error message
                } else {
                    preparedContentHolder = ec
                    ec.content
                }
            }

            val nodeSummarizeText by nodeLLMRequest(
                "summarize_extracted_text",
                allowToolCalls = false
            )

            val nodeCombineResult by node<Message.Response, SummaryOutput>("combine_result") { response ->
                val pc = preparedContentHolder!!
                SummaryOutput(
                    title = pc.title,
                    author = pc.author,
                    summary = response.content,
                    isYoutubeLink = isYoutube,
                    length = summaryLength
                )
            }

            // FIXME: the error should not show as result
            val nodeHandleError by node<String, SummaryOutput>("handle_error") { errorContent ->
                SummaryOutput(
                    title = "Error",
                    author = "System",
                    summary = errorContent,
                    isYoutubeLink = isYoutube,
                    length = summaryLength
                )
            }

            edge(nodeStart forwardTo nodeAnalyzeInput)

            edge(
                nodeAnalyzeInput forwardTo nodeExtractArticle
                        onCondition { isWebUrl(it) })
            edge(
                nodeAnalyzeInput forwardTo nodeExtractVideo
                        onCondition { isYouTubeUrl(it) })
            edge(
                nodeAnalyzeInput forwardTo nodeExtractFile
                        onCondition { isFileUri(it) })
            edge(
                nodeAnalyzeInput forwardTo nodePreparePlainText
                        onCondition { isPlainText(it) })

            edge(nodeExtractArticle forwardTo nodePrepareForLLM)
            edge(nodeExtractVideo forwardTo nodePrepareForLLM)
            edge(nodeExtractFile forwardTo nodePrepareForLLM)
            edge(nodePreparePlainText forwardTo nodePrepareForLLM)

            edge(nodePrepareForLLM forwardTo nodeSummarizeText onCondition { preparedContentHolder != null })
            edge(nodePrepareForLLM forwardTo nodeHandleError onCondition { preparedContentHolder == null })

            edge(nodeSummarizeText forwardTo nodeCombineResult)

            edge(nodeCombineResult forwardTo nodeFinish)
            edge(nodeHandleError forwardTo nodeFinish)
        }
}