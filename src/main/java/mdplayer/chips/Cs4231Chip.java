/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdsound.Instrument;
import mdsound.instrument.Cs4231Inst;


/**
 * Cs4231Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-12-29 nsano initial version <br>
 */
public class Cs4231Chip extends BaseChip {

    @Override
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Cs4231Inst.class};
    }

    /**
     * What the codec is putting out, which is all there is to show of it.
     * <p>
     * It is a codec, not a synthesizer: a driver hands it a stream and it plays it, so there is no
     * channel, no note and no level register anywhere - only the sound. The emulator keeps a
     * sample of its own output, and that is reported here under the name the sampled readers ask
     * for it by.
     *
     * @see mdplayer.fmdsp.Cs4231Reader
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(Cs4231Inst.class);
        if (inst == null) return Collections.emptyMap();
        // the instrument keys its one number by its own name; the display wants it by what it is
        Object output = inst.getView(chipId, "volume").values().stream().findFirst().orElse(null);
        return output instanceof Integer sample ? Map.of("output", sample) : Collections.emptyMap();
    }

    public void setFifoBuf(int chipId, byte[] buf) {
        context.mds.inst(Cs4231Inst.class).setFifoBuf(chipId, buf);
    }

    public void setFifoBuf(int chipIndex, int chipId, byte[] buf) {
        context.mds.inst(Cs4231Inst.class, chipIndex).setFifoBuf(chipId, buf);
    }

    public byte[] EMS_GetCurrentMapBuf(int chipId) {
        return context.mds.inst(Cs4231Inst.class).EMS_GetCurrentMapBuf(chipId);
    }

    public byte[] EMS_GetCurrentMapBuf(int chipIndex, int chipId) {
        return context.mds.inst(Cs4231Inst.class, chipIndex).EMS_GetCurrentMapBuf(chipId);
    }

    public void EMS_Map(int chipId, byte al, byte[] ah, int bx, int dx) {
        context.mds.inst(Cs4231Inst.class).EMS_Map(chipId, al, ah, bx, dx);
    }

    public void EMS_Map(int chipIndex, int chipId, int al, byte[] ah, int bx, int dx) {
        context.mds.inst(Cs4231Inst.class, chipIndex).EMS_Map(chipId, al, ah, bx, dx);
    }

    public int EMS_GetPageMap(int chipId) {
        return context.mds.inst(Cs4231Inst.class).EMS_GetPageMap(chipId);
    }

    public int EMS_GetPageMap(int chipIndex, int chipId) {
        return context.mds.inst(Cs4231Inst.class, chipIndex).EMS_GetPageMap(chipId);
    }

    public void EMS_GetHandleName(int chipId, byte[] ah, int dx, String[] buf) {
        context.mds.inst(Cs4231Inst.class).EMS_GetHandleName(chipId, ah, dx, buf);
    }

    public void EMS_GetHandleName(int chipIndex, int chipId, byte[] ah, int dx, String[] buf) {
        context.mds.inst(Cs4231Inst.class, chipIndex).EMS_GetHandleName(chipId, ah, dx, buf);
    }

    public void EMS_SetHandleName(int chipId, byte[] ah, int dx, String buf) {
        context.mds.inst(Cs4231Inst.class).EMS_SetHandleName(chipId, ah, dx, buf);
    }

    public void EMS_SetHandleName(int chipIndex, int chipId, byte[] ah, int dx, String buf) {
        context.mds.inst(Cs4231Inst.class, chipIndex).EMS_SetHandleName(chipId, ah, dx, buf);
    }

    public void EMS_AllocMemory(int chipId, byte[] ah, int[] dx, int bx) {
        context.mds.inst(Cs4231Inst.class).EMS_AllocMemory(chipId, ah, dx, bx);
    }

    public void EMS_AllocMemory(int chipIndex, int chipId, byte[] ah, int[] dx, int bx) {
        context.mds.inst(Cs4231Inst.class, chipIndex).EMS_AllocMemory(chipId, ah, dx, bx);
    }

//    public void Int0bEnt(int chipId, Runnable act) {
//        context.mds.inst(Cs4231Inst.class).setInt0bEnt(chipId, act);
//    }
//
//    public void Int0bEnt(int chipIndex, int chipId, Runnable act) {
//        context.mds.inst(Cs4231Inst.class, chipIndex).setInt0bEnt(chipId, act);
//    }

    public void write(int chipId, int port, int Adr, int Data) {
        context.mds.inst(Cs4231Inst.class).write(chipId, port, Adr, Data);
    }

    public void write(int chipIndex, int chipId, int port, int Adr, int Data) {
        context.mds.inst(Cs4231Inst.class).write(chipId, port, Adr, Data);
    }

    public int read(int chipId, int adr) {
        return context.mds.inst(Cs4231Inst.class).read(chipId, adr);
    }

    public int read(int chipIndex, int chipId, int adr) {
        return context.mds.inst(Cs4231Inst.class, chipIndex).read(chipId, adr);
    }
}
