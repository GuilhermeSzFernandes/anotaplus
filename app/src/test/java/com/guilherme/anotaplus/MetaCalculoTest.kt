package com.guilherme.anotaplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MetaCalculoTest {

    private fun epoch(ano: Int, mes: Int, dia: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(ano, mes - 1, dia, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `mesesAte calcula meses inteiros entre duas datas`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        assertEquals(6, MetaCalculo.mesesAte(prazo, agora))
    }

    @Test
    fun `mesesAte nunca retorna menos que 1`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2026, 7, 25)
        assertEquals(1, MetaCalculo.mesesAte(prazo, agora))
    }

    @Test
    fun `valorMensalNecessario divide o valor restante pelos meses`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 0.0, prazo, agora)
        assertEquals(166.67, resultado!!, 0.01)
    }

    @Test
    fun `valorMensalNecessario desconta o valor ja guardado`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 400.0, prazo, agora)
        assertEquals(100.0, resultado!!, 0.01)
    }

    @Test
    fun `valorMensalNecessario retorna null sem prazo`() {
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 0.0, null, epoch(2026, 7, 23))
        assertNull(resultado)
    }

    @Test
    fun `prazoEmMeses soma meses a partir de agora`() {
        val agora = epoch(2026, 7, 23)
        val resultado = MetaCalculo.prazoEmMeses(agora, 6)
        assertEquals(epoch(2027, 1, 23), resultado)
    }
}
