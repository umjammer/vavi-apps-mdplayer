# mdplayer.driver.mfi

mfi (`.mld`, i-mode ringtone) driver

## Usage

The song is read by vavi-sound (`MfiMidiFileReader`) and played on a synthesizer chosen for the
sound chip of the phone the file was made for. The synthesizer renders when the player asks it to,
so an mfi is mixed, recorded and paused like any other song. None of it goes through `MidiPlugin`.

| chip    | phones (docomo)                                                         |
|---------|-------------------------------------------------------------------------|
| Yamaha  | N, SO504i ~ SO213i, F503i (MA-2 ~ MA-7)                                 |
| FueTrek | SH252i ~, P505i/506i, D505i/506i/900i, 903i ~ (48A, 64B, PCM128)        |
| Rohm    | F504i ~ F901iS, D901i, P900i ~ P901iS, SH251i/505i (BU8788KN, BU8709KN) |

## The synthesizers

A synthesizer is a `MldSynth` service provider (`META-INF/services/mdplayer.driver.mfi.MldSynth`).
Of the ones that sound as a chip, the available one of the highest priority plays it.

| name               | chips            | priority | synthesizer                                                                 | needs                                                    |
|--------------------|------------------|---------:|-----------------------------------------------------------------------------|----------------------------------------------------------|
| `ma7`              | Yamaha           |      100 | `vavi.sound.midi.ma7.Ma7Synthesizer` (mfiplayer)                            | `libM7_EmuSmw7.so` (`-Dvavi.sound.mfi.ma7.path`)          |
| `nuked`            | Yamaha           |        0 | Nuked OPL3, with the FM voices the file sends (mfiplayer)                   |                                                          |
| `opendoja.ma3`     | Yamaha           |      -10 | `vavi.sound.midi.openDoja.Ma3Synthesizer` (vavi-sound-sandbox)              | vavi-sound-sandbox on the class path                     |
| `ucs`              | FueTrek          |      100 | `vavi.sound.midi.ucs.UcsSynthesizer` (mfiplayer)                            | `rt_synth_4.dll` (`-Dvavi.sound.mfi.faith.path`)          |
| `opendoja.fuetrek` | FueTrek          |       50 | `vavi.sound.midi.openDoja.FuetrekSynthesizer` (vavi-sound-sandbox)          | vavi-sound-sandbox on the class path                     |
| `rohm`             | Rohm             |      100 | the rohm sound source (mfiplayer)                                           | `rt_synth_2.dll` (`-Dvavi.sound.mfi.faith.path`)          |
| `gervill`          | all              |     -100 | Gervill, the jdk's general midi pcm                                         |                                                          |

`opendoja.ma3` is below `nuked` because it doesn't sound the NEC machine dependent song
`02 TRANSPARENT.mld` that nuked does (peak 511 against 12263); `ma7` has the same gap, but it only
plays when its library is configured.

A synthesizer of the midi api is played without a line: `UcsSynthesizer`, `Ma7Synthesizer` and
openDoja's give an `openStream()`, Gervill is an `AudioSynthesizer` (`StreamMldSynth`).

| property                              | default  | what it is                                                                           |
|---------------------------------------|----------|--------------------------------------------------------------------------------------|
| `mdplayer.mfi.chip.default`           | `YAMAHA` | the chip of a file that doesn't say (`YAMAHA`, `FUETREK`, `ROHM`)                    |
| `mdplayer.mfi.synth.<chip>`           |          | the synthesizer for the files of a chip, `mdplayer.mfi.synth.yamaha=ma7`            |
| `mdplayer.mfi.synth`                  |          | the synthesizer for every file, `.<chip>` wins over it                               |
| `vavi.sound.mfi.faith.path`           | wine's   | the faith authoring tool's `Tools` directory, where `rt_synth_4.dll` and `rt_synth_2.dll` are |
| `vavi.sound.mfi.ma7.path`             | `tmp/libM7_EmuSmw7.so` | the MA-7 library                                                       |

## Which chip

A file never names its chip, so `MldChip` works it out. It tries these in order:

1. the `ainf` audio formats: `0x80` rohm, `0x81` fuetrek, `0x82` yamaha. This is certain, but
   only an mfi 4 or later with adpcm has one
2. a phone model in `supt` (`MFICONV_N505I`), looked up in `models.csv`
3. a Yamaha part in `supt` (`MA3Plugin_N`, `SCP-MA5-N-Plugin`)
4. the maker, together with the mfi version (`vers`). The maker comes from the vendor letter
   in `supt` (`SH_PlugIn`, `MFi4PlugIn_F`, `AT531_N`) or from the vendor nibble of the machine
   dependent messages: `0x10` N, `0x20` F, `0x30` SO, `0x40` P, `0x60` D, `0x70` SH

`models.csv` and the version rules come from the "MFi" sheet of the phone database.
When a maker changed chips within one mfi generation, the later phones win.

What a corpus says about it:

| corpus                          | files | told by                                                      |
|---------------------------------|------:|--------------------------------------------------------------|
| `~/Public/np2/mfi` (commercial) |  4433 | half have a `supt`, most of the rest have a vendor nibble    |
| `UnGoodMLD` (hobbyists, MLDC)   | 15462 | nothing: no `supt`, no `ainf`, no machine dependent messages |

The folders named after phones come out as their chips: `F901iC` all rohm, `P902i` 21 of 22
fuetrek, `N506iS` 91 of 92 yamaha (`MldDriverTest`). A file records the phone it was made for,
not the one it was found on: the `F-09A` folder is mostly `SH` data.

## The visualizer

None of the synthesizers says what it is sounding. But every message of the song goes past
`MldDriver` on its way to one, so `MldChannels` keeps the sixteen channels as the song leaves
them, and `mdplayer.fmdsp.MldReader` puts them on the fmdsp rows. A Yamaha song goes on the FM
rows and a FueTrek or Rohm one on the PCM rows, taken in the order the channels first sound.
The meters follow velocity × volume × expression. The analyzer bars are measured off the mixer,
because the song is rendered there. The chip is named in upper case (`YAMAHA MA-3`, `FUETREK`).

A song whose tune lives only in NEC machine dependent messages (`02 TRANSPARENT.mld`) sends no
midi note, so it lights no row. The bars still move with its sound.

## TODO

* ~~rohm synthesizer~~ ... the rohm sound source of
  [vavi-apps-mfiplayer](https://github.com/umjammer/vavi-apps-mfiplayer) (`vavi.sound.mfi.rohm`), a port of
  `rt_synth_2.dll`, bit exact to it
* ~~the adpcm of vavi-sound's `AudioEngine`s plays into a line of its own, not through the mixer~~
  ... `AudioEngineMixer` (vavi-sound 1.1.2): the driver mixes it in, starting on the frame of its message
* level: UCS and Gervill reach full scale, and nothing is calibrated yet
  (`DefaultVolumeBalance_MLD.xml` is SMAF's copy)
* loop points (`0xdd`): a song plays once
* ~~make synthesizer changeable `openDoja`, `SiON` etc.~~ ... `MldSynth` service providers, `SiON` is still to come
