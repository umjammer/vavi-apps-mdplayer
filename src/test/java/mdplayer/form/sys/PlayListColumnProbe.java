package mdplayer.form.sys;

import java.lang.reflect.Method;

import mdplayer.PlayList;
import org.junit.jupiter.api.Test;

class PlayListColumnProbe {
    @Test
    void dump() throws Exception {
        PlayList pl = new PlayList();
        pl.addFile("/Users/nsano/src/java/simplevgm/tmp/Adlib_Music_Synthesizer_Card_Demo_Songs_(IBM_PC_XT_AT)/007 When the Saints Go Marching In.vgz");
        PlayList.Music m = pl.getMusics().getFirst();
        Method value = FormPlayList.class.getDeclaredMethod("value", PlayList.Music.class, FormPlayList.cols.class);
        Method header = FormPlayList.class.getDeclaredMethod("header", String.class);
        value.setAccessible(true); header.setAccessible(true);
        for (FormPlayList.cols c : FormPlayList.cols.values())
            System.err.println("PROBE " + c.ordinal() + " " + c + " [" + header.invoke(null, c.name()) + "] = " + value.invoke(null, m, c));
    }
}
