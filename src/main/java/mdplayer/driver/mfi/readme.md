# mdplayer.driver.mfi

mfi (`.mld`, i-mode ringtone) driver

## Usage

The song is read by vavi-sound (`MfiMidiFileReader`) and played on a synthesizer of
[vavi-apps-mfiplayer](https://github.com/umjammer/vavi-apps-mfiplayer) chosen for the sound chip
of the phone the file was made for. The synthesizer renders when the player asks it to, so an mfi
is mixed, recorded and paused like any other song. None of it goes through `MidiPlugin`.

| chip    | phones (docomo)                         | synthesizer                                  |
|---------|-----------------------------------------|----------------------------------------------|
| Yamaha  | N, SO504i ~ SO213i, F503i (MA-2 ~ MA-7) | Nuked OPL3, with the FM voices the file sends |
| FueTrek | SH252i ~, P505i/506i, D505i/506i/900i, 903i ~ (48A, 64B, PCM128) | the fuetrek sound source (UCS), needs `rt_synth_4.dll` |
| Rohm    | F504i ~ F901iS, D901i, P900i ~ P901iS, SH251i/505i (BU8788KN, BU8709KN) | no emulator, Gervill stands in |

| property                    | default  | what it is                                                          |
|-----------------------------|----------|---------------------------------------------------------------------|
| `mdplayer.mfi.chip.default` | `YAMAHA` | the chip of a file that doesn't say (`YAMAHA`, `FUETREK`, `ROHM`)   |
| `mdplayer.mfi.synth`        |          | one synthesizer for every file: `nuked`, `ucs` or `gervill`         |
| `vavi.sound.mfi.faith.path` | wine's   | the faith authoring tool's `Tools` directory, where `rt_synth_4.dll` is |

Without `rt_synth_4.dll` a FueTrek song is played on the OPL3.

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

| corpus                         | files | told by                                                        |
|--------------------------------|------:|----------------------------------------------------------------|
| `~/Public/np2/mfi` (commercial) |  4433 | half have a `supt`, most of the rest have a vendor nibble      |
| `UnGoodMLD` (hobbyists, MLDC)  | 15462 | nothing: no `supt`, no `ainf`, no machine dependent messages   |

The folders named after phones come out as their chips: `F901iC` all rohm, `P902i` 21 of 22
fuetrek, `N506iS` 91 of 92 yamaha (`MldDriverTest`). A file records the phone it was made for,
not the one it was found on: the `F-09A` folder is mostly `SH` data.

## TODO

* rohm synthesizer. faith's `rt_synth_2.dll` is the "Ring Tone LSI Simulator Type 2", which is
  the rohm one: its ADPCM and "UCS Editor R" arrived together in 1.3.0. Its tables are not laid
  out like `rt_synth_4.dll`'s (`FuetrekRom` finds no groups in it), and it does not play when
  dropped in for Type 4 under rts4c. It needs reverse engineering of its own
* the adpcm of vavi-sound's `AudioEngine`s plays into a line of its own, not through the mixer
* level: UCS and Gervill reach full scale, and nothing is calibrated yet
  (`DefaultVolumeBalance_MLD.xml` is SMAF's copy)
* loop points (`0xdd`): a song plays once
