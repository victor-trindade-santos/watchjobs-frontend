package com.watchjobs.model

// Duas formas de ver o mesmo quadro: o backend continua com as sete colunas reais.
enum class BoardView(val label: String) {
    SIMPLE("Simplificada"),
    DETAILED("Detalhada")
}

// Colunas da visualização simplificada, cada uma agrupando colunas reais na ordem de exibição.
enum class SimpleBoardColumn(val label: String, val columns: List<BoardColumn>) {
    SAVED("Salvas", listOf(BoardColumn.SAVED)),
    APPLIED("Candidatado", listOf(BoardColumn.APPLIED)),
    IN_PROCESS("Em processo seletivo", listOf(BoardColumn.IN_REVIEW, BoardColumn.INTERVIEW, BoardColumn.TECH_TEST, BoardColumn.OFFER)),
    CLOSED("Encerradas", listOf(BoardColumn.CLOSED));

    // Só "Em processo seletivo" mistura etapas; nela a ordem vem da etapa, não do arrasto.
    val allowsReorder: Boolean
        get() = columns.size == 1

    companion object {
        fun of(column: BoardColumn): SimpleBoardColumn = entries.first { column in it.columns }
    }
}

// Card com a coluna real onde ele está, para a etiqueta e para decidir o destino ao mover.
data class BoardEntry(val card: BoardCard, val column: BoardColumn)

object BoardGrouping {
    // Mantém a ordem do backend dentro de cada etapa e as etapas na ordem da coluna simplificada.
    fun entries(board: Board, column: SimpleBoardColumn): List<BoardEntry> =
        column.columns.flatMap { real -> board.cards(real).map { BoardEntry(it, real) } }

    fun entries(board: Board, column: BoardColumn): List<BoardEntry> = board.cards(column).map { BoardEntry(it, column) }

    // Coluna real de destino ao soltar numa coluna simplificada; null quando nada deve mudar.
    fun moveTarget(current: BoardColumn, target: SimpleBoardColumn): BoardColumn? = when (target) {
        SimpleBoardColumn.IN_PROCESS -> if (current in target.columns) null else BoardColumn.IN_REVIEW
        else -> target.columns.single()
    }
}
