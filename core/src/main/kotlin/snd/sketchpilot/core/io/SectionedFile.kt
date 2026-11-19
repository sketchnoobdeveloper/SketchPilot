package snd.sketchpilot.core.io

/** Lossless parser for Sketchware Pro's '@section' + JSON-line payloads. */
class SectionedFile {
    data class Section(val name: String, val lines: MutableList<String> = mutableListOf())

    private val sections = mutableListOf<Section>()

    fun all(): MutableList<Section> = sections

    fun first(name: String): Section? = sections.firstOrNull { it.name == name }

    fun put(name: String, lines: List<String>) {
        val target = first(name) ?: Section(name).also { sections.add(it) }
        target.lines.clear()
        target.lines.addAll(lines)
    }

    fun serialize(): String = buildString {
        sections.forEach { section ->
            if (section.name.isNotEmpty()) append('@').append(section.name).append('\n')
            section.lines.forEach { append(it).append('\n') }
        }
    }

    companion object {
        fun parse(text: String): SectionedFile {
            val result = SectionedFile()
            var current: Section? = null
            text.split('\n').forEachIndexed { index, line ->
                if (line.isEmpty() && index == text.split('\n').size - 1) return@forEachIndexed
                if (line.startsWith("@")) {
                    current = Section(line.substring(1)).also { result.sections.add(it) }
                } else {
                    val target = current ?: Section("").also { result.sections.add(it); current = it }
                    target.lines.add(line)
                }
            }
            return result
        }
    }
}
