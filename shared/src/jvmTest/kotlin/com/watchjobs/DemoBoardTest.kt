package com.watchjobs

import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.remote.FakeBackendTransport
import com.watchjobs.model.BoardColumn
import com.watchjobs.model.BoardGrouping
import com.watchjobs.model.MoveRequest
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.SimpleBoardColumn
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

// Kanban no modo demonstração: as mesmas rotas e regras de movimentação do backend.
class DemoBoardTest {
    @Test
    fun demoBoardGroupsAndMovesLikeTheBackend() = runBlocking {
        val container = AppContainer(demoMode = true)
        container.authRepository.signin(SigninRequest(FakeBackendTransport.DEMO_EMAIL, FakeBackendTransport.DEMO_PASSWORD))
        val boards = container.boardRepository

        val board = boards.board()
        assertEquals(6, BoardColumn.entries.sumOf { board.cards(it).size })
        assertEquals(listOf(2, 1, 2, 1), SimpleBoardColumn.entries.map { BoardGrouping.entries(board, it).size })
        fun idOf(company: String) = BoardColumn.entries.flatMap { board.cards(it) }.first { it.organizationName == company }.id

        // Candidatado -> Em processo seletivo entra em "Em análise" (etapa Triagem).
        val halden = idOf("Halden Labs")
        boards.move(halden, MoveRequest(BoardColumn.IN_REVIEW.name))
        val afterReview = boards.board()
        assertEquals(halden, afterReview.cards(BoardColumn.IN_REVIEW).single().id)
        assertEquals("Triagem", afterReview.cards(BoardColumn.IN_REVIEW).single().stageName)

        // Salvas -> Candidatado cria a candidatura.
        val meridian = idOf("Meridian Freight")
        boards.move(meridian, MoveRequest(BoardColumn.APPLIED.name))
        assertNotNull(boards.board().cards(BoardColumn.APPLIED).single { it.id == meridian }.applicationId)

        // Encerradas exige o desfecho.
        val error = assertFailsWith<ApiException> { boards.move(halden, MoveRequest(BoardColumn.CLOSED.name)) }
        assertEquals(400, error.status)
        boards.move(halden, MoveRequest(BoardColumn.CLOSED.name, outcome = SavedJobStatus.WITHDRAWN))
        val closed = boards.board().cards(BoardColumn.CLOSED)
        assertEquals(SavedJobStatus.WITHDRAWN, closed.single { it.id == halden }.status)

        // Nada some nem duplica depois das movimentações.
        val final = boards.board()
        val ids = BoardColumn.entries.flatMap { final.cards(it) }.map { it.id }
        assertEquals(6, ids.toSet().size)
        assertEquals(6, ids.size)
        assert(boards.timeline(halden).isNotEmpty())
    }
}
