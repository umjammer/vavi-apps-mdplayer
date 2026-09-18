/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import mdplayer.Common.EnmArcType;
import mdplayer.PlayList;
import mdplayer.PlayList.Music;
import mdplayer.driver.BaseFileFormat;
import mdplayer.driver.Plugin;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.sampled.md.MdEncoding;
import vavi.sound.sampled.md.MdFileFormatType;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;


/**
 * MFi (".mld", i-mode ringtone) FileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class MldFileFormat extends BaseFileFormat {

    @Override
    public String[] getExtensions() {
        return new String[] {".mld"};
    }

    @Override
    public MetaData getMetaData() {
        return new MldDriver().retrieveMetaData(this.srcBuf);
    }

    @Override
    public List<PlayList.Music> getMusic(String file, byte[] buf, String zipFile /* = null */, Archive archive, Entry entry /* = null */) {
        MetaData md = getMetaData();

        PlayList.Music music = new PlayList.Music();
        music.format = this;
        music.fileName = file;
        music.arcFileName = zipFile;
        music.arcType = EnmArcType.unknown;
        if (zipFile != null && !zipFile.isEmpty())
            music.arcType = zipFile.toLowerCase().lastIndexOf(".zip") != -1 ? EnmArcType.ZIP : EnmArcType.LZH;

        if (md != null) {
            music.title = md.getFirst(Tag.Title);
            music.titleJ = md.getFirst(Tag.TitleJ);
            music.composer = md.getFirst(Tag.Composer);
            music.composerJ = md.getFirst(Tag.ComposerJ);
            music.vgmby = md.getFirst(Tag.Maker);
            music.notes = md.getFirst(Tag.Note);
        }
        music.songNo = 0;

        return Collections.singletonList(music);
    }

    @Override
    public List<Music> getMusic(Music ms, byte[] buf, String zipFile) {
        return getMusicCommon(ms, buf, zipFile);
    }

    @Override
    public String[] getPresetMixerBalance() {
        return new String[] {"DriverBalance_MLD.mbc", "/resources/DefaultVolumeBalance_MLD.xml"};
    }

    @Override
    public Plugin getPlugin() {
        return Plugin.getPlugin(MldPlugin.class);
    }

    @Override
    public Encoding getEncoding() {
        return new MdEncoding("MFi", "mld");
    }

    @Override
    public Type getType() {
        return new MdFileFormatType("MFi", "mld");
    }

    @Override
    public int getMarkSize() {
        return 8;
    }

    @Override
    public boolean isSupported(InputStream is) throws IOException {
        if (isCompressedStream(is)) return false;
        byte[] header = new byte[4];
        int r = is.read(header);
        if (r < 4) return false;
        return header[0] == 'm' && header[1] == 'e' && header[2] == 'l' && header[3] == 'o';
    }
}
