package com.watchjobs

import com.watchjobs.util.splitJobDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobDescriptionTest {
    @Test
    fun splitsKnownHeadingsWithoutLosingText() {
        val text = "Halden Labs builds simulation software. What you'll do Own the component library. Review pull requests. " +
            "Requirements: Strong TypeScript. Based in Copenhagen. Benefits Gym."
        val sections = splitJobDescription(text)
        assertEquals(listOf("Sobre a vaga", "O que você vai fazer", "Requisitos", "Benefícios"), sections.map { it.title })
        assertEquals(listOf("Own the component library.", "Review pull requests."), sections[1].items)
        assertTrue(sections[1].asList)
        assertEquals(listOf("Strong TypeScript.", "Based in Copenhagen."), sections[2].items)
        assertEquals(listOf("Gym."), sections[3].items)
    }

    @Test
    fun lowercaseWordsAreNotHeadings() {
        val sections = splitJobDescription("You will meet the requirements of clients.")
        assertEquals(1, sections.size)
        assertEquals("Sobre a vaga", sections.single().title)
    }

    @Test
    fun emptyDescriptionHasNoSections() {
        assertTrue(splitJobDescription(null).isEmpty())
        assertTrue(splitJobDescription("  ").isEmpty())
    }
}
