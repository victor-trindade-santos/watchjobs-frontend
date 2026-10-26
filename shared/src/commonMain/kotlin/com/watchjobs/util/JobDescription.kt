package com.watchjobs.util

data class DescriptionSection(val title: String, val items: List<String>, val asList: Boolean)

const val ABOUT_ROLE = "Sobre a vaga"
private const val ABOUT_COMPANY = "Sobre a empresa"

// Títulos comuns nas descrições (PT e EN), agrupados no título que a tela mostra.
private val HEADINGS: List<Pair<String, List<String>>> = listOf(
    ABOUT_ROLE to listOf(
        "Sobre a vaga", "Descrição da vaga", "About the role", "About the job", "The opportunity", "Job description"
    ),
    "O que você vai fazer" to listOf(
        "O que você vai fazer", "O que você fará", "Suas responsabilidades", "Responsabilidades", "Principais atividades",
        "Atividades", "Atribuições", "Key responsibilities", "Responsibilities", "What you'll do", "What you will do",
        "Your responsibilities", "In this role you will"
    ),
    "Requisitos" to listOf(
        "Requisitos obrigatórios", "Requisitos e qualificações", "Requisitos", "Qualificações", "O que buscamos",
        "O que esperamos de você", "Minimum qualifications", "Basic qualifications", "Requirements", "Qualifications",
        "What we're looking for", "What you'll bring", "What you bring", "Who you are", "Must have", "Must-haves"
    ),
    "Diferenciais" to listOf(
        "Será um diferencial", "Diferenciais", "Desejáveis", "Desejável", "Preferred qualifications", "Nice to have",
        "Nice-to-have", "Bonus points"
    ),
    "Benefícios" to listOf("O que oferecemos", "Benefícios", "Perks and benefits", "What we offer", "Benefits", "Perks"),
    ABOUT_COMPANY to listOf("Sobre a empresa", "Sobre nós", "Quem somos", "About the company", "About us", "Who we are")
)

private val headingTitles: Map<String, String> =
    HEADINGS.flatMap { (title, phrases) -> phrases.map { it.lowercase() to title } }.toMap()

private val headingRegex: Regex = run {
    val alternation = headingTitles.keys.sortedByDescending { it.length }.joinToString("|") { it.replace("'", "['’]") }
    Regex("(^|\\s)($alternation)(\\s*:)?(?=\\s|$)", RegexOption.IGNORE_CASE)
}

private val sentenceEnd = Regex("([.;!?])\\s+")

// O backend guarda a descrição como texto corrido (sem HTML). Aqui ela é quebrada nas seções que
// aparecem no texto ("Requisitos", "What you'll do"...). Nenhum trecho é descartado.
fun splitJobDescription(text: String?): List<DescriptionSection> {
    val clean = text?.trim().orEmpty()
    if (clean.isEmpty()) return emptyList()
    val chunks = mutableListOf<Pair<String, String>>()
    var title = ABOUT_ROLE
    var cursor = 0
    headingRegex.findAll(clean)
        .filter { it.groupValues[2].first().isUpperCase() }
        .forEach { match ->
            val start = match.range.first + match.groupValues[1].length
            chunks += title to clean.substring(cursor, start)
            title = headingTitles[match.groupValues[2].lowercase().replace('’', '\'')] ?: ABOUT_ROLE
            cursor = match.range.last + 1
        }
    chunks += title to clean.substring(cursor)

    val merged = linkedMapOf<String, MutableList<String>>()
    chunks.forEach { (sectionTitle, body) ->
        val items = toItems(body)
        if (items.isNotEmpty()) merged.getOrPut(sectionTitle) { mutableListOf() } += items
    }
    return merged.map { (sectionTitle, items) ->
        DescriptionSection(sectionTitle, items, asList = sectionTitle != ABOUT_ROLE && sectionTitle != ABOUT_COMPANY && items.size > 1)
    }
}

private fun toItems(body: String): List<String> {
    val text = body.trim().trimStart(':', '-', '•').trim()
    if (text.isEmpty()) return emptyList()
    val parts = if (text.contains('•')) text.split('•') else text.replace(sentenceEnd, "$1\u0000").split('\u0000')
    return parts.map { it.trim().trimStart('-', '*').trim() }.filter { it.isNotEmpty() }
}
