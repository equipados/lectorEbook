package com.ebookreader.core.tts.controller

import com.ebookreader.core.data.preferences.TtsEngineType
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.engine.CloudTtsEngine
import com.ebookreader.core.tts.engine.LocalTtsEngine
import com.ebookreader.core.tts.engine.TtsEngine
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.model.TtsVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsControllerImpl @Inject constructor(
    private val localEngine: LocalTtsEngine,
    private val cloudEngine: CloudTtsEngine,
    private val userPreferences: UserPreferences
) : TtsController {

    private val _state = MutableStateFlow(TtsState())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _currentSegment = MutableStateFlow<TextSegment?>(null)
    override val currentSegment: StateFlow<TextSegment?> = _currentSegment.asStateFlow()

    private var segments: List<TextSegment> = emptyList()

    // Longitud de texto por chapterIndex (para detectar "capítulos" que son
    // portada / índice / copyright) y primer capítulo narrativo real.
    private var chapterLengths: Map<Int, Int> = emptyMap()
    private var firstContentChapter: Int = 0

    // Scope para relanzar `speak` desde el callback onDone (no se puede
    // llamar a una función suspend directamente desde un callback).
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Migración: si quedó guardada una voz en inglés (default antiguo),
        // reemplazarla por la voz española por defecto.
        scope.launch {
            val prefs = userPreferences.ttsPrefs.first()
            if (prefs.cloudVoiceName.startsWith("en-", ignoreCase = true)) {
                userPreferences.updateTtsPrefs(prefs.copy(cloudVoiceName = "es-ES-Standard-A"))
            }
        }

        // Observa las preferencias de TTS para cambiar el motor activo
        // cuando el usuario lo modifica en Ajustes.
        userPreferences.ttsPrefs
            .onEach { prefs ->
                val newType = when (prefs.preferredEngine) {
                    TtsEngineType.LOCAL -> EngineType.LOCAL
                    TtsEngineType.CLOUD -> EngineType.CLOUD
                }
                if (_state.value.engineType != newType) {
                    // Al cambiar de motor, para lo que esté reproduciendo
                    // en el motor anterior y actualiza el estado.
                    runCatching { activeEngine.stop() }
                    _state.update { it.copy(engineType = newType, isPlaying = false) }
                }
                // Al cambiar la voz en Ajustes, refresca el engine activo
                // para que la próxima frase sintetizada use la nueva voz.
                if (newType == EngineType.CLOUD && prefs.cloudVoiceName.isNotBlank()) {
                    val lang = prefs.cloudVoiceName.split("-").take(2).joinToString("-")
                    cloudEngine.setVoice(
                        TtsVoice(
                            id = prefs.cloudVoiceName,
                            name = prefs.cloudVoiceName,
                            language = lang,
                            engineType = EngineType.CLOUD
                        )
                    )
                }
            }
            .launchIn(scope)
    }

    private val activeEngine: TtsEngine
        get() = when (_state.value.engineType) {
            EngineType.LOCAL -> localEngine
            EngineType.CLOUD -> cloudEngine
        }

    override suspend fun loadText(chapters: List<Pair<String, String>>) {
        val built = mutableListOf<TextSegment>()
        val lengths = mutableMapOf<Int, Int>()
        chapters.forEachIndexed { chapterIndex, (_, content) ->
            val trimmed = content.trim()
            lengths[chapterIndex] = trimmed.length
            // Construimos segments para TODOS los capítulos (incluso cortos)
            // para preservar los índices coherentes con el reader visual.
            if (trimmed.isBlank()) return@forEachIndexed
            val sentences = splitIntoSentences(trimmed)
            var offset = 0
            for (sentence in sentences) {
                val start = trimmed.indexOf(sentence, offset).takeIf { it >= 0 } ?: offset
                val end = start + sentence.length
                built.add(TextSegment(sentence, start, end, chapterIndex))
                offset = end
            }
        }
        segments = built
        chapterLengths = lengths
        firstContentChapter = chapters.indexOfFirst { (_, content) ->
            val trimmed = content.trim()
            trimmed.length >= NARRATIVE_CHAPTER_MIN_CHARS && !isLikelyFrontmatter(trimmed)
        }.takeIf { it >= 0 } ?: 0

        val first = segments.firstOrNull()
        _state.update {
            it.copy(
                currentSegmentIndex = 0,
                currentChapterIndex = first?.chapterIndex ?: 0
            )
        }
        _currentSegment.value = first
    }

    override suspend fun play() {
        if (!activeEngine.isInitialized()) {
            activeEngine.initialize()
        }

        // Si el capítulo actual es "frontmatter" (portada, índice, copyright,
        // créditos...) o tiene muy poco texto, saltar automáticamente al
        // primer capítulo narrativo para no leer contenido basura en voz alta.
        val currentChapter = _state.value.currentChapterIndex
        val shouldSkipCurrent = currentChapter < firstContentChapter
        if (shouldSkipCurrent && firstContentChapter != currentChapter) {
            val targetSegmentIndex =
                segments.indexOfFirst { it.chapterIndex == firstContentChapter }
            if (targetSegmentIndex >= 0) {
                val segment = segments[targetSegmentIndex]
                _state.update {
                    it.copy(
                        currentSegmentIndex = targetSegmentIndex,
                        currentChapterIndex = segment.chapterIndex
                    )
                }
                _currentSegment.value = segment
            }
        }

        _state.update { it.copy(isPlaying = true) }
        speakCurrentSegment()
    }

    override suspend fun pause() {
        activeEngine.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    override suspend fun stop() {
        activeEngine.stop()
        _state.update { it.copy(isPlaying = false, currentSegmentIndex = 0, currentChapterIndex = 0) }
        _currentSegment.value = segments.firstOrNull()
    }

    override suspend fun nextSentence() {
        val next = _state.value.currentSegmentIndex + 1
        if (next < segments.size) {
            val segment = segments[next]
            _state.update {
                it.copy(
                    currentSegmentIndex = next,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override suspend fun previousSentence() {
        val prev = (_state.value.currentSegmentIndex - 1).coerceAtLeast(0)
        val segment = segments[prev]
        _state.update {
            it.copy(
                currentSegmentIndex = prev,
                currentChapterIndex = segment.chapterIndex
            )
        }
        _currentSegment.value = segment
        if (_state.value.isPlaying) {
            speakCurrentSegment()
        }
    }

    override suspend fun nextChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val nextSegmentIndex = segments.indexOfFirst { it.chapterIndex > currentChapter }
        if (nextSegmentIndex >= 0) {
            val segment = segments[nextSegmentIndex]
            _state.update {
                it.copy(
                    currentSegmentIndex = nextSegmentIndex,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override suspend fun previousChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val targetChapter = (currentChapter - 1).coerceAtLeast(0)
        jumpToChapter(targetChapter)
    }

    override suspend fun jumpToChapter(index: Int) {
        val segmentIndex = segments.indexOfFirst { it.chapterIndex == index }
        if (segmentIndex >= 0) {
            val segment = segments[segmentIndex]
            _state.update {
                it.copy(
                    currentSegmentIndex = segmentIndex,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        activeEngine.setSpeed(speed)
    }

    override fun setVoice(voice: TtsVoice) {
        _state.update { it.copy(activeVoice = voice) }
        activeEngine.setVoice(voice)
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        return activeEngine.getAvailableVoices()
    }

    override fun shutdown() {
        localEngine.shutdown()
        cloudEngine.shutdown()
    }

    private suspend fun speakCurrentSegment() {
        val index = _state.value.currentSegmentIndex
        val segment = segments.getOrNull(index) ?: return
        _currentSegment.value = segment

        activeEngine.speak(segment.text) {
            // onDone callback: avanza al siguiente segmento y lo reproduce.
            val currentIndex = _state.value.currentSegmentIndex
            val nextIndex = currentIndex + 1
            if (_state.value.isPlaying && nextIndex < segments.size) {
                val nextSegment = segments[nextIndex]
                _state.value = _state.value.copy(
                    currentSegmentIndex = nextIndex,
                    currentChapterIndex = nextSegment.chapterIndex
                )
                _currentSegment.value = nextSegment
                // Relanza speak con el siguiente segmento (no se puede llamar
                // directamente a una función suspend desde aquí).
                scope.launch { speakCurrentSegment() }
            } else if (nextIndex >= segments.size) {
                _state.value = _state.value.copy(isPlaying = false)
            }
        }
    }

    /**
     * Detecta si un texto es probablemente "frontmatter" del libro
     * (copyright, créditos, dedicatoria, índice) buscando palabras clave
     * típicas en los primeros 1500 caracteres. Se considera frontmatter
     * si aparecen al menos 2 coincidencias.
     */
    private fun isLikelyFrontmatter(content: String): Boolean {
        val sample = content.lowercase().take(1500)
        val hits = FRONTMATTER_KEYWORDS.count { sample.contains(it) }
        return hits >= 2
    }

    companion object {
        private const val NARRATIVE_CHAPTER_MIN_CHARS = 1500

        private val FRONTMATTER_KEYWORDS = listOf(
            "copyright",
            "©",
            "isbn",
            "todos los derechos",
            "derechos reservados",
            "depósito legal",
            "deposito legal",
            "prohibida la reproducción",
            "prohibida la reproduccion",
            "queda prohibida",
            "all rights reserved",
            "first published",
            "printed in",
            "título original",
            "titulo original",
            "traducción de",
            "traduccion de",
            "maquetación",
            "maquetacion",
            "edición",
            "créditos",
            "creditos",
            "editorial",
            "dedicatoria",
            "agradecimientos",
            "tabla de contenido",
            "tabla de contenidos",
            "índice",
            "indice",
            "table of contents"
        )
    }

    private fun splitIntoSentences(text: String): List<String> {
        val pattern = Regex("[^.!?]+[.!?]+\\s*")
        val matches = pattern.findAll(text).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
        if (matches.isEmpty() && text.isNotBlank()) {
            return listOf(text.trim())
        }
        return matches
    }
}
