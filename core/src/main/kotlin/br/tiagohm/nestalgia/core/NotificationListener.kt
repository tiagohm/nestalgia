package br.tiagohm.nestalgia.core

fun interface NotificationListener {

    fun processNotification(type: NotificationType, vararg data: Any?)
}
