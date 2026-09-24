# mdplayer.driver.zms

zmusic

## Usage

### system property

#### Mixer Balance

`mdplayer.balance.<element>` ... overrides one chip's mixer volume over whatever balance the song loaded. `<element>` is
the element name of the balance files (`DefaultVolumeBalance_*.xml`, `*.mbc`), the value is in the same 2&times;dB unit
(-192..20). The mixer passes a chip at most at its own full scale, which the default leaves 6dB below: +12 is the
ceiling, higher values change nothing.

`mdplayer.zms.mercury.mpcmVolume`, `mdplayer.zms.mercury.opmVolume` ... a Mercury-UNIT ZMS song (one that sets an MPCM
rate plain MPCM.X lacks, like 16bit 44.1kHz `@f13`) gets MPCM and OPM moved by these from the balance, for that song
only (default 12 and -12). Other songs and drivers keep their balance.
