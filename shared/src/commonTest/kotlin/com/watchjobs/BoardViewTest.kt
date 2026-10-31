package com.watchjobs

import com.watchjobs.data.local.BoardViewPreference
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.model.Board
import com.watchjobs.model.BoardCard
import com.watchjobs.model.BoardColumn
import com.watchjobs.model.BoardColumnData
import com.watchjobs.model.BoardGrouping
import com.watchjobs.model.BoardView
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.SimpleBoardColumn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoardViewTest {
    private var nextId = 1L

    private fun cards(vararg status: SavedJobStatus) = status.map { BoardCard(nextId++, "Cargo", "Empresa", status = it) }

    private val board = Board(
        listOf(
            BoardColumnData("SAVED", cards(SavedJobStatus.SAVED, SavedJobStatus.PLANNING_TO_APPLY)),
            BoardColumnData("APPLIED", cards(SavedJobStatus.APPLIED)),
            BoardColumnData("IN_REVIEW", cards(SavedJobStatus.IN_PROCESS, SavedJobStatus.IN_PROCESS)),
            BoardColumnData("INTERVIEW", cards(SavedJobStatus.IN_PROCESS)),
            BoardColumnData("TECH_TEST", cards(SavedJobStatus.IN_PROCESS)),
            BoardColumnData("OFFER", cards(SavedJobStatus.OFFER_RECEIVED)),
            BoardColumnData("CLOSED", cards(SavedJobStatus.REJECTED, SavedJobStatus.ACCEPTED))
        )
    )

    @Test
    fun simpleViewKeepsEveryCardExactlyOnce() {
        val detailedIds = BoardColumn.entries.flatMap { board.cards(it) }.map { it.id }
        val simpleIds = SimpleBoardColumn.entries.flatMap { BoardGrouping.entries(board, it) }.map { it.card.id }
        assertEquals(detailedIds.sorted(), simpleIds.sorted())
        assertEquals(simpleIds.size, simpleIds.toSet().size)
        assertEquals(listOf(2, 1, 5, 2), SimpleBoardColumn.entries.map { BoardGrouping.entries(board, it).size })
    }

    @Test
    fun inProcessFollowsStageOrderAndKeepsOrderInsideEachStage() {
        val entries = BoardGrouping.entries(board, SimpleBoardColumn.IN_PROCESS)
        assertEquals(
            listOf(BoardColumn.IN_REVIEW, BoardColumn.IN_REVIEW, BoardColumn.INTERVIEW, BoardColumn.TECH_TEST, BoardColumn.OFFER),
            entries.map { it.column }
        )
        assertEquals(board.cards(BoardColumn.IN_REVIEW).map { it.id }, entries.take(2).map { it.card.id })
    }

    @Test
    fun movingInSimpleViewMapsToRealColumns() {
        assertEquals(BoardColumn.SAVED, BoardGrouping.moveTarget(BoardColumn.APPLIED, SimpleBoardColumn.SAVED))
        assertEquals(BoardColumn.APPLIED, BoardGrouping.moveTarget(BoardColumn.SAVED, SimpleBoardColumn.APPLIED))
        assertEquals(BoardColumn.IN_REVIEW, BoardGrouping.moveTarget(BoardColumn.APPLIED, SimpleBoardColumn.IN_PROCESS))
        assertEquals(BoardColumn.IN_REVIEW, BoardGrouping.moveTarget(BoardColumn.CLOSED, SimpleBoardColumn.IN_PROCESS))
        assertEquals(BoardColumn.CLOSED, BoardGrouping.moveTarget(BoardColumn.OFFER, SimpleBoardColumn.CLOSED))
        listOf(BoardColumn.IN_REVIEW, BoardColumn.INTERVIEW, BoardColumn.TECH_TEST, BoardColumn.OFFER).forEach {
            assertNull(BoardGrouping.moveTarget(it, SimpleBoardColumn.IN_PROCESS), "Sem alteração ao soltar $it em Em processo seletivo")
        }
    }

    @Test
    fun onlyInProcessForbidsManualReorder() {
        assertEquals(listOf(true, true, false, true), SimpleBoardColumn.entries.map { it.allowsReorder })
        BoardColumn.entries.forEach { assertEquals(true, it in SimpleBoardColumn.of(it).columns) }
    }

    @Test
    fun preferenceDefaultsToSimpleAndPersists() {
        val store = InMemoryKeyValueStore()
        val preference = BoardViewPreference(store)
        assertEquals(BoardView.SIMPLE, preference.get())
        preference.set(BoardView.DETAILED)
        assertEquals("DETAILED", store.get("boardView"))
        assertEquals(BoardView.DETAILED, BoardViewPreference(store).get())
        store.put("boardView", "QUALQUER")
        assertEquals(BoardView.SIMPLE, preference.get())
    }
}
