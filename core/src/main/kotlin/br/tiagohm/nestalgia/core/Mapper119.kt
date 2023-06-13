package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_119

class Mapper119(console: Console) : MMC3ChrRam(console, 0x40, 0x7F, 8)
