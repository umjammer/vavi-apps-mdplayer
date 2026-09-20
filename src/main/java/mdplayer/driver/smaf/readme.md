# mdplayer.driver.smaf

smaf (.mmf) driver

There are two of them, and `SmafPlugin` builds `SmafDriver2`.

| driver        | sound source                                     | needs                         |
|---------------|--------------------------------------------------|-------------------------------|
| `SmafDriver2` | the yamaha MA-7 in pure java                     | `libM7_EmuSmw7.so`            |
| `SmafDriver`  | mmftoolc.exe on an emulated PC (jdosbox)         | mmftool and its yamaha dlls   |

## SmafDriver2, the MA-7 in pure java

vavi-sound reads the song into a midi sequence (`vavi.sound.midi.smaf.SmafMidiFileReader`) and the
driver plays it, against the samples it renders, on `vavi.sound.midi.smaf.SmafMa7Synthesizer` of
vavi-apps-mfiplayer - the MA-7 emulator of yamaha's `libM7_EmuSmw7.so` reading a sequence's
exclusives as smaf ones. It is opened without a line of its own (`openStream`), so the song is
mixed, recorded and paused like any other rendered format.

That is one sound source for every generation the format has: an MA-1, MA-2, MA-3, MA-5 or Uta
song is played by the MA-7, which is what a later phone did with it too. The file's own generation
is kept in the metadata as the "System" (`SMAF MA-3`); what `Tag.Chip` names - the fmdsp header,
the window title - is the MA-7, because that is what makes the sound.

The rom is read out of the installed library, which is nobody's to ship:
`-Dvavi.sound.ma7.path=<libM7_EmuSmw7.so, or the apk it is in>`, default `tmp/libM7_EmuSmw7.so`.
Without it the song cannot start and says so.

The stream waves of a song ("Mwa\*", "Awa\*") are the one thing the MA-7 has nothing of yet. They
are played by the adpcm engines of vavi-sound and mixed into what this renders
(`AudioEngineMixer`), so they sound in the song and not beside it, the way the mfi driver does it.

There is nothing to emulate at chip speed here and no emulated PC to keep fed, so a song starts at
once and renders many times faster than real time.

## SmafDriver, the emulated player

There is no MA-2/MA-3/MA-5 emulator to write chip registers to, so this plays the real thing:
[mmftool](https://github.com/murachue/mmftool) driving Yamaha's `M5_Emu*.dll` on an emulated PC
(jdosbox, embedded through `jdos.api.JDosBox`). Point `-Dmdplayer.smaf.mmftool=<dir>` at the
directory holding `mmftoolc.exe`, `M5_EmuHw.dll`, `M5_EmuSmw5.dll` and `DefMA3_16.vm3`.

mmftool needs two patches this project relies on. It reads `MMFTOOL_SAMPLE_RATE` (and
`MMFTOOL_MASTER_VOLUME`) from its environment, in `EmuSetSampleRate` before `EmuInitialize`.
That matters because synthesis costs in proportion to the rate: the heaviest MA-5 song is
synthesized at 0.95 of real time at 48000 and 1.43 at 32000, which is worth more than every
optimization made to the emulator put together. Only 22050, 32000, 44100 and 48000 work — the
dll answers anything else with silence rather than an error, so mmftool refuses those.

And with `MMFTOOL_TELEMETRY=1` - which this always sets - it writes out what it is playing (its
`telemetry.c`), which is where the fmdsp visualizer gets its notes from; see below.

| property                 | default                  | what it is                                                       |
|--------------------------|--------------------------|------------------------------------------------------------------|
| `mdplayer.smaf.mmftool`  | `/usr/local/src/mmftool` | where mmftoolc.exe and its dlls are                              |
| `mdplayer.smaf.volume`   | `100`                    | the player's own master volume, 0-127; 127 clips on loud songs   |
| `mdplayer.smaf.rate.ma5` | `32000`                  | what MA-5 and MA-7 are synthesized at; costs only above 16kHz    |
| `mdplayer.smaf.prime`    | `3`                      | seconds of audio in hand before the song starts                  |

MA-1/2/3 and Uta are synthesized at 48000 — there is nothing to gain by lowering them. Raising
`rate.ma5` back to 48000 makes that song *behind* for its whole length, so it then has to start
on a ten second cushion instead of three; that follows the rate by itself and is not a setting.

The player must run with `-XX:+UseParallelGC` (the `run` profile does): the emulated PC is one
guest cpu on one host thread, pinned at 100% of a core for the whole song, and g1's write
barriers cost that thread about 5% for concurrency it never needs. No other core can help — x86
emulation of one guest cpu is serial, which is why a 24 core host shows 12% and still struggles.

### going back to it

Three lines, all of them marked:

* `SmafPlugin` — the type parameter, the `driverVirtual = ...` line in `prepare` (the old one is
  commented out under it), the `stopPlayer()` line in `stop`, and the `DRIVER` constant the tests
  of the driver that is not in use skip themselves against.

Nothing else moves. Both fmdsp readers stay registered and each one only ever answers for its own
driver, and `SmafDriverTest` / `SmafFmDspProbe` come back out of skip by themselves.

## The visualizer

`SmafDriver2` shows the notes the way the mfi driver does: nothing goes past on its way out of the
MA-7 either, but every message of the song goes past the driver on its way *in*, so `MidiChannels`
keeps the sixteen channels as the song leaves them and `Smaf2Reader` puts them on the fm rows -
see `mdplayer.fmdsp.MidiChannelsReader`, which is shared with the mfi driver's `MldReader`.

`SmafDriver` cannot do that - the dll is handed the file and sequences it itself, answering nothing
but a position - so its display is driven by the score, which mmftool has already parsed for its
own piano roll and writes out before it starts playing: notes with the length they are held for,
and the controls that decide how loud they are and where. `SmafTelemetry` reads that, `SmafScore`
says what is sounding at a given moment, and `SmafReader` puts it on the fmdsp rows.

What makes that line up with what is heard is that every line the guest prints is stamped, on its
way out of the emulator, with how much audio it had produced when it wrote it (jdosbox's
`StdioSink`). A position and a stamp together say "song time P is output frame F", which is the
one thing that cannot be worked out from this end - the silence a player writes while it loads and
the silence a song opens on are the same silence, and both jdosbox and `MmfToolPlayer` drop what
they take for the first kind. After that the audio keeps the time by itself.

A file may also name a start point (`MspI`), and the dll plays from it and counts its position
from it, so the score is moved back onto that clock as it arrives - see `SmafScore#startMillis`.
Without that the display runs ahead of the music by however far in the file the song starts, which
for the sample song is 2.2 seconds.

The analyzer bars are not either reader's business: SMAF audio goes through mdplayer's own mixer,
so they are measured off the sound itself the way every rendered format's are.
