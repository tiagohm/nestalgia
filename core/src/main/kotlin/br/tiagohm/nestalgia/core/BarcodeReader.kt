package br.tiagohm.nestalgia.core

fun interface BarcodeReader {

    fun inputBarcode(barcode: Long, digitCount: Int)
}
