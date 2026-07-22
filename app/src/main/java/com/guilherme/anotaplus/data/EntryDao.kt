package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: EntryType): Flow<List<Entry>>

    // Gasto + Recebimento misturados (extrato único do Financeiro, sem
    // separar por tipo — sinal +/- de cada linha já indica qual é qual).
    @Query(
        "SELECT * FROM entries WHERE type IN ('GASTO', 'RECEBIMENTO') AND timestamp BETWEEN :inicio AND :fim ORDER BY timestamp DESC"
    )
    fun getFinanceiroPorMes(inicio: Long, fim: Long): Flow<List<Entry>>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Seleção múltipla das Anotações: excluir/marcar tag em lote.
    @Query("DELETE FROM entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE entries SET categoria = :categoria WHERE id IN (:ids)")
    suspend fun atualizarCategoriaEmMassa(ids: List<Long>, categoria: String?)

    // Renomear categoria (CategoriasActivity): categoria é string livre em
    // Entry, não FK — sem isso, lançamentos antigos ficariam presos ao
    // nome antigo depois de renomear.
    @Query("UPDATE entries SET categoria = :novoNome WHERE categoria = :nomeAntigo")
    suspend fun renomearCategoriaEmTodosLancamentos(nomeAntigo: String, novoNome: String)

    @Query("SELECT * FROM entries WHERE remoteId IS NULL")
    suspend fun getPendentesDeSync(): List<Entry>

    @Query("SELECT EXISTS(SELECT 1 FROM entries WHERE remoteId = :remoteId)")
    suspend fun existsByRemoteId(remoteId: String): Boolean

    @Query("UPDATE entries SET remoteId = :remoteId WHERE id = :id")
    suspend fun marcarSincronizada(id: Long, remoteId: String)

    @Query(
        """
        SELECT categoria, SUM(valor) AS total FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY categoria
        ORDER BY total DESC
        """
    )
    fun getGastoPorCategoria(inicio: Long, fim: Long): Flow<List<CategoriaTotal>>

    // Versão "de uma vez só" (sem Flow), usada pelo widget de categorias
    // (CategoriasWidgetUpdater): ele não fica escutando o banco, só relê
    // sob demanda, mesmo padrão de getTotalGastoOnce.
    @Query(
        """
        SELECT categoria, SUM(valor) AS total FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY categoria
        ORDER BY total DESC
        """
    )
    suspend fun getGastoPorCategoriaOnce(inicio: Long, fim: Long): List<CategoriaTotal>

    // Total gasto numa categoria específica no mês — base do alerta de
    // orçamento (BudgetAlertNotifier), chamado logo depois de salvar um Gasto.
    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'GASTO' AND categoria = :categoria AND timestamp BETWEEN :inicio AND :fim"
    )
    suspend fun getTotalGastoPorCategoriaOnce(categoria: String, inicio: Long, fim: Long): Double

    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    fun getTotalGasto(inicio: Long, fim: Long): Flow<Double>

    // Versão "de uma vez só" (sem Flow), usada pelo widget da tela inicial:
    // ele não fica escutando o banco, só relê o total quando é atualizado.
    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    suspend fun getTotalGastoOnce(inicio: Long, fim: Long): Double

    @Query(
        "SELECT COUNT(*) FROM entries WHERE type = 'PENSAMENTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    fun getTotalIdeias(inicio: Long, fim: Long): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'RECEBIMENTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    fun getTotalRecebimento(inicio: Long, fim: Long): Flow<Double>

    // Total gasto por dia (fuso do aparelho, via modificador 'localtime' do
    // SQLite) — base do gráfico de saldo acumulado do Financeiro (junto
    // com getRecebimentoPorDia).
    @Query(
        """
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dia, SUM(valor) AS total
        FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY dia
        ORDER BY dia ASC
        """
    )
    fun getGastoPorDia(inicio: Long, fim: Long): Flow<List<GastoDiario>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dia, SUM(valor) AS total
        FROM entries
        WHERE type = 'RECEBIMENTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY dia
        ORDER BY dia ASC
        """
    )
    fun getRecebimentoPorDia(inicio: Long, fim: Long): Flow<List<GastoDiario>>

    @Query(
        "SELECT * FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL ORDER BY valor DESC LIMIT 1"
    )
    fun getMaiorGasto(inicio: Long, fim: Long): Flow<Entry?>

    @Query(
        "SELECT COALESCE(AVG(valor), 0) FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL"
    )
    fun getMediaGasto(inicio: Long, fim: Long): Flow<Double>

    // Categoria mais usada por FREQUÊNCIA (contagem de lançamentos) —
    // diferente de getGastoPorCategoria, que ordena por soma de valor.
    @Query(
        """
        SELECT categoria, COUNT(*) AS qtd FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim
        GROUP BY categoria
        ORDER BY qtd DESC
        LIMIT 1
        """
    )
    fun getCategoriaMaisUsada(inicio: Long, fim: Long): Flow<CategoriaContagem?>

    @Query(
        """
        SELECT strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS diaSemana, SUM(valor) AS total
        FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY diaSemana
        ORDER BY total DESC
        LIMIT 1
        """
    )
    fun getDiaSemanaMaisGasto(inicio: Long, fim: Long): Flow<DiaSemanaTotal?>
}
