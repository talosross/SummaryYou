package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
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
import me.nanova.summaryexpressive.model.SummaryException

@Serializable
data class SummaryOutput(
    override val title: String,
    override val author: String,
    override val summary: String,
    val sourceLink: String? = null,
    // TODO: just use SummarySource
    val isYoutubeLink: Boolean,
    val length: SummaryLength,
) : SummaryData

class LLMHandler(context: Context, httpClient: HttpClient) {
    private val fileExtractorTool: FileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)

    fun getSummarizationAgent(
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
            AIProvider.GEMINI -> createGeminiExecutor(apiKey, baseUrl)
            AIProvider.CLAUDE -> createClaudExecutor(apiKey, baseUrl)
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
                ?: OpenAIModels.CostOptimized.GPT4oMini)

            AIProvider.GEMINI -> (modelName?.let { GoogleModels.Gemini2_5Pro }
                ?: GoogleModels.Gemini2_5Flash)

            AIProvider.CLAUDE -> (modelName?.let { AnthropicModels.Sonnet_4 }
                ?: AnthropicModels.Sonnet_3_5)

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
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OpenAILLMClient(apiKey, settings = OpenAIClientSettings(baseUrl = it)) }
            ?: OpenAILLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createGeminiExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { GoogleLLMClient(apiKey, settings = GoogleClientSettings(baseUrl = it)) }
            ?: GoogleLLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createClaudExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { AnthropicLLMClient(apiKey, settings = AnthropicClientSettings(baseUrl = it)) }
            ?: AnthropicLLMClient(apiKey)

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
            var extractedContent: ExtractedContent? = null
            var isYoutube = false
            var sourceLink = ""

            val nodeExtractArticle by nodeExecuteSingleTool(tool = articleExtractorTool)
            val nodeExtractVideo by nodeExecuteSingleTool(tool = youTubeTranscriptTool)
            val nodeExtractFile by nodeExecuteSingleTool(tool = fileExtractorTool)

            val nodePreparePlainText by node<String, ExtractedContent>("prepare_plain_text") { text ->
                ExtractedContent("Text Input", "Unknown", text)
            }

            // TODO: this is not necessary if the compress history is working
            // see: https://github.com/JetBrains/koog/issues/706
            val nodeTriggerSummaryFromTool by node<ExtractedContent, String>("trigger_summary_from_tool") {
                extractedContent = it
                "Content has been extracted. Please summarize it based on the instructions."
            }

            val nodeSummarizeText by nodeLLMRequest(
                "summarize_extracted_text",
                allowToolCalls = false,
            )

            val processToolResult = { it: SafeTool.Result<out ExtractedContent> ->
                when (it) {
                    is SafeTool.Result.Success -> it.result
                    is SafeTool.Result.Failure -> {
                        val errorMessage = it.message
                        throw when {
                            errorMessage.contains("Paywall detected") -> SummaryException.PaywallException
                            errorMessage.contains("Could not extract video ID") -> SummaryException.InvalidLinkException
                            errorMessage.contains("Could not get transcript") -> SummaryException.NoTranscriptException
                            errorMessage.contains("Could not extract text from URL") -> SummaryException.NoContentException
                            errorMessage.contains("Unsupported file type") -> SummaryException.InvalidLinkException // Or a more specific FileTypeException
                            errorMessage.contains("Extracted text from file is empty") -> SummaryException.NoContentException
                            else -> SummaryException.UnknownException(errorMessage)
                        }
                    }
                }
            }

            val nodeCombineResult by node<Message.Response, SummaryOutput>("combine_result") { response ->
                val content = response.content
                if (content.isBlank() || content.startsWith("Error:", ignoreCase = true)) {
                    if (content.contains("API key", ignoreCase = true))
                        throw SummaryException.IncorrectKeyException
                    if (content.contains("rate limit", ignoreCase = true))
                        throw SummaryException.RateLimitException
                    throw SummaryException.UnknownException(content)
                }
                val ec = extractedContent!!
                SummaryOutput(
                    title = ec.title,
                    author = ec.author,
                    summary = content,
                    sourceLink = sourceLink,
                    isYoutubeLink = isYoutube,
                    length = summaryLength
                )
            }

            edge(
                nodeStart forwardTo nodeExtractArticle
                        onCondition { isWebUrl(it) }
                        transformed {
                    sourceLink = it
                    Article(it)
                }
            )
            edge(
                nodeStart forwardTo nodeExtractVideo
                        onCondition { isYouTubeUrl(it) }
                        transformed {
                    isYoutube = true
                    sourceLink = it
                    YouTubeTranscript(it)
                }
            )
            edge(
                nodeStart forwardTo nodeExtractFile
                        onCondition { isFileUri(it) }
                        transformed { File(it) }
            )
            edge(
                nodeStart forwardTo nodePreparePlainText
                        onCondition { isPlainText(it) })

            // Tool-based paths
            edge(nodeExtractArticle forwardTo nodeTriggerSummaryFromTool transformed { processToolResult(it) })
            edge(nodeExtractVideo forwardTo nodeTriggerSummaryFromTool transformed { processToolResult(it) })
            edge(nodeExtractFile forwardTo nodeTriggerSummaryFromTool transformed { processToolResult(it) })
            edge(nodeTriggerSummaryFromTool forwardTo nodeSummarizeText)

            // Plain text path
            edge(nodePreparePlainText forwardTo nodeSummarizeText transformed {
                extractedContent = it
                it.content
            })

            edge(nodeSummarizeText forwardTo nodeCombineResult)

            edge(nodeCombineResult forwardTo nodeFinish)
        }
}