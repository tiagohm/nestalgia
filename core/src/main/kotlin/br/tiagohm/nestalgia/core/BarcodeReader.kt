package br.tiagohm.nestalgia.core

interface BarcodeReader {

    fun inputBarcode(barcode: Long, digitCount: Int)
}
