package com.guilherme.anotaplus

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Dialogos "sem chrome genérico do Android": nenhum usa setTitle/
// setPositiveButton/setNegativeButton do builder (isso é sempre o
// TextButton/título padrão do Material3, o motivo do "modal quadrado, fora
// do design system" que a gente queria matar) — a própria view em
// dialog_*.xml já traz título e botões com nossos estilos, e só trocamos
// o fundo da janela pra combinar com o resto da tela que abriu o dialog.
//
// Duas variantes: `criarDialogoFlat` pras telas redesenhadas ("fintech
// moderno") e `criarDialogoPaper` só pro que ainda vive na Captura Rápida
// (paper/ink, reaproveita @drawable/bg_card_free — a mesma "carta" usada
// em todo o resto daquele fluxo).
fun Context.criarDialogoFlat(view: View, cancelavel: Boolean = true): AlertDialog =
    MaterialAlertDialogBuilder(this)
        .setView(view)
        .setCancelable(cancelavel)
        .create()
        .apply { window?.setBackgroundDrawableResource(R.drawable.bg_dialog_flat) }

fun Context.criarDialogoPaper(view: View, cancelavel: Boolean = true): AlertDialog =
    MaterialAlertDialogBuilder(this)
        .setView(view)
        .setCancelable(cancelavel)
        .create()
        .apply { window?.setBackgroundDrawableResource(R.drawable.bg_card_free) }
