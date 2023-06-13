package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_192

class Mapper192(console: Console) : MMC3ChrRam(console, 0x08, 0x0B, 4)
