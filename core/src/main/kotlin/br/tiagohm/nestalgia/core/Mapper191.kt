package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_191

class Mapper191(console: Console) : MMC3ChrRam(console, 0x80, 0xFF, 2)
