/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.emu.psx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static java.lang.System.getLogger;


/**
 * The little of a PlayStation and of a PS2 IOP a sound program touches.
 * <p>
 * ported from aosdk eng_psf/psx_hw.c. What is emulated:
 * <ul>
 *  <li>main ram, 2 MB mirrored the way the hardware mirrors it</li>
 *  <li>DMA channel 4 (and 7 on the PS2), both directions, with its completion irq</li>
 *  <li>the vblank irq and root counters 0 to 2, with their events</li>
 *  <li>the PS1 kernel calls a sound program uses, in software instead of from a BIOS rom</li>
 *  <li>the PS2 IOP kernel, including its threads, semaphores, event flags and timers</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-09 nsano initial version <br>
 */
public class PsxHw implements PsxBus {

    private static final Logger logger = getLogger(PsxHw.class.getName());

    /** the 33 MHz IOP clock is divided by this so the interpreter can keep up */
    private static final int CLOCK_DIV = 8;

    private static final int MAX_FILE_SLOTS = 32;
    private static final int SEMA_MAX = 64;

    // root counter modes
    private static final int RC_EN = 0x0001;
    private static final int RC_RESET = 0x0008;
    private static final int RC_DIV8 = 0x0200;

    // Sony event states
    private static final int EvStWAIT = 0x1000;
    private static final int EvStACTIVE = 0x2000;
    private static final int EvStALREADY = 0x4000;

    // Sony event modes
    private static final int EvMdINTR = 0x1000;

    // heap block struct offsets
    private static final int BLK_STAT = 0;
    private static final int BLK_SIZE = 4;
    private static final int BLK_FD = 8;
    private static final int BLK_BK = 12;

    /** the event control block table the kernel keeps, as a byte address in ram */
    private static final int EVENT_BASE = 0x1000;
    /** one EvtCtrlBlk is 32 entries of 16 bytes, and the counter events sit 64 blocks along */
    private static final int EVENT_BLOCK_SIZE = 32 * 16;
    private static final int COUNTER_EVENT_BASE = EVENT_BASE + 32 * 2 * EVENT_BLOCK_SIZE;

    // thread states
    private static final int TS_RUNNING = 0;
    private static final int TS_READY = 1;
    private static final int TS_WAITEVFLAG = 2;
    private static final int TS_WAITSEMA = 3;
    private static final int TS_WAITDELAY = 4;
    private static final int TS_SLEEPING = 5;
    private static final int TS_CREATED = 6;

    /** IOP ram, 2 MB of little endian words */
    public final int[] ram = new int[(2 * 1024 * 1024) / 4];
    public final int[] scratch = new int[0x400];

    public final R3000 cpu = new R3000(this);

    private Spu spu;
    private Spu2 spu2;
    private Psf2Filesystem filesystem;

    /** 50 for PAL, 60 for NTSC, -1 until the file says */
    public int psfRefresh = -1;

    /** the IOP dropped into its null state, which is how a PSF2 says it is finished */
    public boolean songDone;

    public void setSpu(Spu spu) {
        this.spu = spu;
    }

    public void setSpu2(Spu2 spu2) {
        this.spu2 = spu2;
    }

    public void setFilesystem(Psf2Filesystem filesystem) {
        this.filesystem = filesystem;
    }

    // ---- machine state ----

    private static class Thread {
        int iState;
        int flags;
        int routine;
        int stackloc;
        int stacksize;
        int refCon;
        int waitparm;
        final int[] saveRegs = new int[37];
    }

    private static class Semaphore {
        int attr;
        int option;
        int init;
        int current;
        int max;
        int threadsWaiting;
        boolean inuse;
    }

    private static class EventFlag {
        int type;
        int value;
        int param;
        boolean inUse;
    }

    private static class IOPTimer {
        int iActive;
        int count;
        int target;
        int source;
        int prescale;
        int handler;
        int hparam;
        int mode;
    }

    private static class Counter {
        int count;
        int mode;
        int target;
        int sysclock;
    }

    private static class ExternLib {
        String name;
        int dispatch;
    }

    private final Thread[] threads = new Thread[32];
    private int iNumThreads;
    private int iCurThread;

    private final Semaphore[] semaphores = new Semaphore[SEMA_MAX];
    private int iNumSema;

    private final EventFlag[] evflags = new EventFlag[32];
    private int iNumFlags;

    private final IOPTimer[] iopTimers = new IOPTimer[8];
    private int iNumTimers;

    private final Counter[] rootCnts = new Counter[3];

    private final ExternLib[] regLibs = new ExternLib[32];
    private int iNumLibs;

    private final int[] fileStat = new int[MAX_FILE_SLOTS];
    private final byte[][] fileData = new byte[MAX_FILE_SLOTS][];
    private final int[] fileSize = new int[MAX_FILE_SLOTS];
    private final int[] filePos = new int[MAX_FILE_SLOTS];

    private int spuDelay, dmaIcr, irqData, irqMask, dmaTimer, wai;
    private int dma4Madr, dma4Bcr, dma4Chcr, dma4Delay;
    private int dma7Madr, dma7Bcr, dma7Chcr, dma7Delay;
    private int dma4Cb, dma7Cb, dma4Fval, dma4Flag, dma7Fval, dma7Flag;
    private int irq9Cb, irq9Fval, irq9Flag;

    private int gpuStat;
    private int heapAddr;
    private int entryInt;
    private boolean softcallTarget;
    private boolean intrSusp;
    private boolean irqMutex;
    private long sysTime;
    private boolean timerExp;
    private int fcnt;

    private final int[] irqRegs = new int[37];

    private final Random random = new Random();

    public PsxHw() {
        for (int i = 0; i < threads.length; i++) threads[i] = new Thread();
        for (int i = 0; i < semaphores.length; i++) semaphores[i] = new Semaphore();
        for (int i = 0; i < evflags.length; i++) evflags[i] = new EventFlag();
        for (int i = 0; i < iopTimers.length; i++) iopTimers[i] = new IOPTimer();
        for (int i = 0; i < rootCnts.length; i++) rootCnts[i] = new Counter();
        for (int i = 0; i < regLibs.length; i++) regLibs[i] = new ExternLib();
    }

    // ---- ram helpers ----

    /** the word at a byte address, wrapped into the 2 MB the hardware has */
    private int ramWord(int byteAddress) {
        return ram[(byteAddress & 0x1fffff) >> 2];
    }

    public void setRamWord(int byteAddress, int value) {
        ram[(byteAddress & 0x1fffff) >> 2] = value;
    }

    /** an index straight into the word array, wrapped so a stray kernel pointer cannot throw */
    private int ramAt(int wordIndex) {
        return ram[wordIndex & (ram.length - 1)];
    }

    private void setRamAt(int wordIndex, int value) {
        ram[wordIndex & (ram.length - 1)] = value;
    }

    private int ramByte(int byteAddress) {
        int a = byteAddress & 0x1fffff;
        return (ram[a >> 2] >>> ((a & 3) * 8)) & 0xff;
    }

    public void setRamByte(int byteAddress, int value) {
        int a = byteAddress & 0x1fffff;
        int shift = (a & 3) * 8;
        ram[a >> 2] = (ram[a >> 2] & ~(0xff << shift)) | ((value & 0xff) << shift);
    }

    /** a NUL terminated string out of ram */
    private String ramString(int byteAddress) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 0x10000; i++) {
            int c = ramByte(byteAddress + i);
            if (c == 0) break;
            sb.append((char) c);
        }
        return sb.toString();
    }

    private void setRamString(int byteAddress, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        for (int i = 0; i < b.length; i++) {
            setRamByte(byteAddress + i, b[i]);
        }
        setRamByte(byteAddress + b.length, 0);
    }

    // ---- the bus ----

    @Override
    public int read8(int address) {
        int shift = (address & 3) * 8;
        return (read(address, ~(0xff << shift)) >>> shift) & 0xff;
    }

    @Override
    public int read16(int address) {
        if ((address & 2) != 0) {
            return (read(address, 0x0000ffff) >>> 16) & 0xffff;
        }
        return read(address, 0xffff0000) & 0xffff;
    }

    @Override
    public int read32(int address) {
        return read(address, 0);
    }

    @Override
    public void write8(int address, int data) {
        int shift = (address & 3) * 8;
        write(address, (data & 0xff) << shift, ~(0xff << shift));
    }

    @Override
    public void write16(int address, int data) {
        if ((address & 2) != 0) {
            write(address, data << 16, 0x0000ffff);
            return;
        }
        write(address, data & 0xffff, 0xffff0000);
    }

    @Override
    public void write32(int address, int data) {
        write(address, data, 0);
    }

    /**
     * @param memMask has the bits of the bytes that are <em>not</em> being accessed set,
     *                which is how MAME spells it
     */
    private int read(int offset, int memMask) {
        // 0x00000000-0x007fffff and 0x80000000-0x807fffff, the ram and its kseg0 mirror
        if ((offset & 0x7f800000) == 0) {
            return ram[(offset & 0x1fffff) >> 2];
        }

        long o = offset & 0xffffffffL;

        if (o == 0xbfc00180L || o == 0xbfc00184L) { // exception vector
            return R3000.FUNCT_HLECALL;
        }

        if (o == 0x1f801014L || o == 0xbf801014L) {
            return spuDelay;
        }

        if (o == 0x1f801814L) {
            gpuStat ^= 0xffffffff;
            return gpuStat;
        }

        if (o >= 0x1f801c00L && o <= 0x1f801dffL) {
            if (memMask == 0xffff0000 || memMask == 0xffffff00) {
                return spuReadRegister(offset) & ~memMask;
            } else if (memMask == 0x0000ffff) {
                return spuReadRegister(offset) << 16;
            } else {
                logger.log(Level.DEBUG, "SPU: read unknown mask %08x".formatted(memMask));
            }
        }

        if (o >= 0xbf900000L && o <= 0xbf9007ffL) {
            if (memMask == 0xffff0000 || memMask == 0xffffff00) {
                return spu2Read(offset) & ~memMask;
            } else if (memMask == 0x0000ffff) {
                return spu2Read(offset) << 16;
            } else if (memMask == 0) {
                return spu2Read(offset) | (spu2Read(offset + 2) << 16);
            } else {
                logger.log(Level.DEBUG, "SPU2: read unknown mask %08x".formatted(memMask));
            }
        }

        if (o >= 0x1f801100L && o <= 0x1f801128L) {
            int cnt = (offset >> 4) & 0xf;
            return switch (offset & 0xf) {
                case 0 -> rootCnts[cnt].count;
                case 4 -> rootCnts[cnt].mode;
                case 8 -> rootCnts[cnt].target;
                default -> 0;
            };
        }

        if (o == 0x1f8010f4L) {
            return dmaIcr;
        } else if (o == 0x1f801070L) {
            return irqData;
        } else if (o == 0x1f801074L) {
            return irqMask;
        }

        if (o == 0xbf920344L) {
            return 0x80808080;
        }

        return 0;
    }

    /** @param memMask see {@link #read} */
    private void write(int offset, int data, int memMask) {
        if ((offset & 0x7f800000) == 0) {
            int i = (offset & 0x1fffff) >> 2;
            ram[i] &= memMask;
            ram[i] |= data;
            return;
        }

        long o = offset & 0xffffffffL;

        if (o == 0x1f801014L || o == 0xbf801014L) {
            spuDelay &= memMask;
            spuDelay |= data;
            return;
        }

        if (o >= 0x1f801c00L && o <= 0x1f801dffL) {
            if (memMask == 0xffff0000) {
                spuWriteRegister(offset, data & 0xffff);
                return;
            } else if (memMask == 0x0000ffff) {
                spuWriteRegister(offset, (data >>> 16) & 0xffff);
                return;
            } else {
                logger.log(Level.DEBUG, "SPU: write unknown mask %08x".formatted(memMask));
            }
        }

        if (o >= 0xbf900000L && o <= 0xbf9007ffL) {
            if (memMask == 0xffff0000) {
                spu2Write(offset, data & 0xffff);
                return;
            } else if (memMask == 0x0000ffff) {
                spu2Write(offset, (data >>> 16) & 0xffff);
                return;
            } else if (memMask == 0) {
                spu2Write(offset, data & 0xffff);
                spu2Write(offset + 2, (data >>> 16) & 0xffff);
                return;
            } else {
                logger.log(Level.DEBUG, "SPU2: write unknown mask %08x".formatted(memMask));
            }
        }

        if (o >= 0x1f801100L && o <= 0x1f801128L) {
            int cnt = (offset >> 4) & 0xf;
            switch (offset & 0xf) {
            case 0 -> rootCnts[cnt].count = data;
            case 4 -> rootCnts[cnt].mode = data;
            case 8 -> rootCnts[cnt].target = data;
            default -> { /* nothing */ }
            }
            return;
        }

        // DMA4
        if (o == 0x1f8010c0L) {
            dma4Madr = data;
            return;
        } else if (o == 0x1f8010c4L) {
            dma4Bcr = data;
            return;
        } else if (o == 0x1f8010c8L) {
            dma4Chcr = data;
            psxDma4(dma4Madr, dma4Bcr, dma4Chcr);
            if ((dmaIcr & (1 << (16 + 4))) != 0) {
                dmaTimer = 3;
            }
            return;
        } else if (o == 0x1f8010f4L) {
            dmaIcr = (dmaIcr & memMask)
                    | (~memMask & 0x80000000 & dmaIcr)
                    | (~data & ~memMask & 0x7f000000 & dmaIcr)
                    | (data & ~memMask & 0x00ffffff);
            if ((dmaIcr & 0x7f000000) != 0) {
                dmaIcr &= ~0x80000000;
            }
            return;
        } else if (o == 0x1f801070L) {
            irqData = (irqData & memMask) | (irqData & irqMask & data);
            irqUpdate();
            return;
        } else if (o == 0x1f801074L) {
            irqMask &= memMask;
            irqMask |= data;
            irqUpdate();
            return;
        }

        // PS2 DMA4
        if (o == 0xbf8010c0L) {
            dma4Madr = data;
            return;
        } else if (o == 0xbf8010c8L) {
            dma4Chcr = data;
            ps2Dma4(dma4Madr, dma4Bcr, dma4Chcr);
            if ((dmaIcr & (1 << (16 + 4))) != 0) {
                dmaTimer = 3;
            }
            return;
        }

        if (o == 0xbf8010c4L || o == 0xbf8010c6L) {
            dma4Bcr &= memMask;
            dma4Bcr |= data;
            return;
        }

        // PS2 DMA7
        if (o == 0xbf801500L) {
            dma7Madr = data;
            return;
        } else if (o == 0xbf801504L || o == 0xbf801506L) {
            dma7Bcr &= memMask;
            dma7Bcr |= data;
            return;
        }

        if (o == 0xbf801508L || o == 0xbf80150aL) {
            dma7Chcr = data;
            ps2Dma7(dma7Madr, dma7Bcr, dma7Chcr);
        }
    }

    private int spuReadRegister(int reg) {
        return spu != null ? spu.readRegister(reg) & 0xffff : 0;
    }

    private void spuWriteRegister(int reg, int val) {
        if (spu != null) spu.writeRegister(reg, val);
    }

    private int spu2Read(int reg) {
        return spu2 != null ? spu2.read(reg) & 0xffff : 0;
    }

    private void spu2Write(int reg, int val) {
        if (spu2 != null) spu2.write(reg, val);
    }

    private void psxDma4(int madr, int bcr, int chcr) {
        int size = (bcr >>> 16) * (bcr & 0xffff) * 2;
        if (chcr == 0x01000201) { // cpu to SPU
            if (spu != null) spu.writeDMAMem(madr & 0x1fffff, size);
        } else {
            if (spu != null) spu.readDMAMem(madr & 0x1fffff, size);
        }
    }

    private void ps2Dma4(int madr, int bcr, int chcr) {
        int size = (bcr >>> 16) * (bcr & 0xffff) * 2;
        if (chcr == 0x01000201) { // cpu to SPU2
            if (spu2 != null) spu2.writeDMA4Mem(madr & 0x1fffff, size);
        } else {
            if (spu2 != null) spu2.readDMA4Mem(madr & 0x1fffff, size);
        }
        dma4Delay = 80;
    }

    private void ps2Dma7(int madr, int bcr, int chcr) {
        int size = (bcr >>> 16) * (bcr & 0xffff) * 2;
        if (chcr == 0x01000201 || chcr == 0x00100010 || chcr == 0x000f0010 || chcr == 0x00010010) {
            if (spu2 != null) spu2.writeDMA7Mem(madr & 0x1fffff, size);
        } else {
            // aosdk leaves the SPU2 to ram direction out on purpose
            logger.log(Level.TRACE, "DMA7: SPU2 to RAM %08x".formatted(madr));
        }
        dma7Delay = 80;
    }

    // ---- interrupts ----

    private void irqUpdate() {
        if ((irqData & irqMask) != 0) {
            wai = 0;
            cpu.setIrqLine(R3000.MIPS_IRQ0, R3000.ASSERT_LINE);
        } else {
            cpu.setIrqLine(R3000.MIPS_IRQ0, R3000.CLEAR_LINE);
        }
    }

    private void irqSet(int irq) {
        irqData |= irq;
        irqUpdate();
    }

    // ---- timing ----

    /** one sample of a PS1, i.e. 768 clocks */
    public void slice() {
        runCounters();

        if (wai == 0) {
            cpu.execute(768 / CLOCK_DIV);
        }

        if (dmaTimer != 0) {
            dmaTimer--;
            if (dmaTimer == 0) {
                dmaIcr |= (1 << (24 + 4));
                irqSet(0x0008);
            }
        }
    }

    /** one sample of a PS2 IOP, i.e. 836 clocks */
    public void ps2Slice() {
        timerExp = false;
        runCounters();

        if (iCurThread != -1) {
            cpu.execute(836 / CLOCK_DIV);
        } else {
            // no thread, don't run the cpu, just let the counters move
            if (timerExp) {
                reschedule();
                if (iCurThread != -1) {
                    cpu.execute(836 / CLOCK_DIV);
                }
            }
        }
    }

    /** one video frame of a PS1 */
    public void frame() {
        if (psfRefresh == 50) {
            fcnt++;
            if (fcnt < 6) {
                irqSet(1);
            } else {
                fcnt = 0;
            }
        } else { // NTSC
            irqSet(1);
        }
    }

    /** one video frame of a PS2 IOP */
    public void ps2Frame() {
        reschedule();
    }

    private void runCounters() {
        // no irq source runs while interrupts are suspended
        if (!intrSusp) {
            if (dma4Delay != 0) {
                dma4Delay--;
                if (dma4Delay == 0) {
                    if (spu2 != null) spu2.interruptDMA4();
                    if (dma4Cb != 0) {
                        callIrqRoutine(dma4Cb, dma4Flag);
                    }
                }
            }

            if (dma7Delay != 0) {
                dma7Delay--;
                if (dma7Delay == 0) {
                    if (spu2 != null) spu2.interruptDMA7();
                    if (dma7Cb != 0) {
                        callIrqRoutine(dma7Cb, dma7Flag);
                    }
                }
            }

            for (int i = 0; i < iNumThreads; i++) {
                if (threads[i].iState == TS_WAITDELAY) {
                    if (Integer.compareUnsigned(threads[i].waitparm, CLOCK_DIV) > 0) {
                        threads[i].waitparm -= CLOCK_DIV;
                    } else { // time's up
                        threads[i].waitparm = 0;
                        threads[i].iState = TS_READY;
                        timerExp = true;
                        reschedule();
                    }
                }
            }

            sysTime += 836;

            for (int i = 0; i < iNumTimers; i++) {
                if (iopTimers[i].iActive > 0) {
                    iopTimers[i].count += 836;
                    if (Integer.compareUnsigned(iopTimers[i].count, iopTimers[i].target) >= 0) {
                        iopTimers[i].count -= iopTimers[i].target;
                        callIrqRoutine(iopTimers[i].handler, iopTimers[i].hparam);
                        timerExp = true;
                    }
                }
            }
        }

        // PS1 root counters
        for (int i = 0; i < 3; i++) {
            if ((rootCnts[i].mode & RC_EN) == 0 && rootCnts[i].mode != 0) {
                if ((rootCnts[i].mode & RC_DIV8) != 0) {
                    rootCnts[i].count += 768 / 8;
                } else {
                    rootCnts[i].count += 768;
                }

                if (Integer.compareUnsigned(rootCnts[i].count, rootCnts[i].target) >= 0) {
                    if ((rootCnts[i].mode & RC_RESET) == 0) {
                        rootCnts[i].mode |= RC_EN;
                    } else {
                        rootCnts[i].count = Integer.remainderUnsigned(rootCnts[i].count, rootCnts[i].target);
                    }
                    irqSet(1 << (4 + i));
                }
            }
        }
    }

    // ---- threads ----

    /**
     * @param flag true when a thread freezes itself from inside a kernel call, where the
     *             return address rather than the pc is where it must come back to
     */
    private void freezeThread(int iThread, boolean flag) {
        Thread t = threads[iThread];
        System.arraycopy(cpu.r, 0, t.saveRegs, 0, 32);
        t.saveRegs[32] = cpu.hi;
        t.saveRegs[33] = cpu.lo;
        t.saveRegs[35] = cpu.delayv;
        t.saveRegs[36] = cpu.delayr;
        t.saveRegs[34] = flag ? cpu.r[31] : cpu.pc;

        if (t.iState == TS_RUNNING) {
            t.iState = TS_READY;
        }
    }

    private void thawThread(int iThread) {
        Thread t = threads[iThread];

        // the first time a thread runs it needs its stack and entry point put in place
        if (t.iState == TS_CREATED) {
            t.saveRegs[34] = t.routine - 4; // compensate for weird delay slot effects
            t.saveRegs[29] = (t.stackloc + t.stacksize) - 16;
            t.saveRegs[29] |= 0x80000000;
            t.saveRegs[35] = 0;
            t.saveRegs[36] = 0;
        }

        System.arraycopy(t.saveRegs, 0, cpu.r, 0, 32);
        cpu.hi = t.saveRegs[32];
        cpu.lo = t.saveRegs[33];
        cpu.setPc(t.saveRegs[34]);
        cpu.delayv = t.saveRegs[35];
        cpu.delayr = t.saveRegs[36];

        t.iState = TS_RUNNING;
    }

    private void reschedule() {
        int iNextThread = -1;

        int i = iCurThread + 1;
        if (i >= iNumThreads) {
            i = 0;
        }
        int starti = i;

        while (i < iNumThreads) {
            if (i != iCurThread && threads[i].iState == TS_READY) {
                iNextThread = i;
                break;
            }
            i++;
        }

        if (starti > 0 && iNextThread == -1) {
            for (i = 0; i < iNumThreads; i++) {
                if (i != iCurThread && threads[i].iState == TS_READY) {
                    iNextThread = i;
                    break;
                }
            }
        }

        if (iNextThread != -1) {
            if (iCurThread != -1) {
                freezeThread(iCurThread, false);
            }
            thawThread(iNextThread);
            iCurThread = iNextThread;
            threads[iCurThread].iState = TS_RUNNING;
        } else {
            // nothing to switch to, so keep going only if the current thread is still running
            if (iCurThread != -1) {
                if (threads[iCurThread].iState != TS_RUNNING) {
                    cpu.shortenFrame();
                    iCurThread = -1;
                }
            } else {
                cpu.shortenFrame();
                iCurThread = -1;
            }
        }
    }

    // ---- init ----

    public void init() {
        timerExp = false;

        Arrays.fill(fileStat, 0);
        Arrays.fill(fileData, null);
        Arrays.fill(fileSize, 0);
        Arrays.fill(filePos, 0);

        dma4Cb = dma7Cb = 0;
        sysTime = 0;

        for (ExternLib l : regLibs) {
            l.name = null;
            l.dispatch = 0;
        }
        iNumLibs = 0;

        for (EventFlag f : evflags) {
            f.type = f.value = f.param = 0;
            f.inUse = false;
        }
        iNumFlags = 0;

        for (Thread t : threads) {
            t.iState = 0;
            t.flags = t.routine = t.stackloc = t.stacksize = t.refCon = t.waitparm = 0;
            Arrays.fill(t.saveRegs, 0);
        }
        iNumThreads = 1; // there is always at least one thread

        for (Semaphore s : semaphores) {
            s.attr = s.option = s.init = s.current = s.max = s.threadsWaiting = 0;
            s.inuse = false;
        }
        iNumSema = 0;

        threads[0].iState = TS_RUNNING;
        iCurThread = 0;

        for (IOPTimer t : iopTimers) {
            t.iActive = t.count = t.target = t.source = t.prescale = t.handler = t.hparam = t.mode = 0;
        }
        iNumTimers = 0;

        // the PS1 kernel entry points trap straight into the HLE
        setRamWord(0xa0, R3000.FUNCT_HLECALL);
        setRamWord(0xb0, R3000.FUNCT_HLECALL);
        setRamWord(0xc0, R3000.FUNCT_HLECALL);

        dmaIcr = 0;
        spuDelay = 0;
        irqData = 0;
        irqMask = 0;
        softcallTarget = false;
        gpuStat = 0;
        dma4Madr = dma4Bcr = dma4Chcr = 0;
        heapAddr = 0;
        entryInt = 0;
        intrSusp = false;
        irqMutex = false;
        fcnt = 0;
        dma4Delay = dma7Delay = 0;
        dmaTimer = 0;
        songDone = false;

        wai = 0;

        for (int i = 0; i < 3; i++) {
            rootCnts[i].mode = RC_EN;
            rootCnts[i].sysclock = 0;
            rootCnts[i].count = 0;
            rootCnts[i].target = 0;
        }
    }

    // ---- event control blocks, which live in ram ----

    private int eventStatus(int ev, int spec) {
        return ramWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 4);
    }

    private void setEventStatus(int ev, int spec, int value) {
        setRamWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 4, value);
    }

    private int eventMode(int ev, int spec) {
        return ramWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 8);
    }

    private void setEventMode(int ev, int spec, int value) {
        setRamWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 8, value);
    }

    private int eventHandler(int ev, int spec) {
        return ramWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 12);
    }

    private void setEventHandler(int ev, int spec, int value) {
        setRamWord(EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 12, value);
    }

    private int counterEventStatus(int ev, int spec) {
        return ramWord(COUNTER_EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 4);
    }

    private int counterEventHandler(int ev, int spec) {
        return ramWord(COUNTER_EVENT_BASE + ev * EVENT_BLOCK_SIZE + spec * 16 + 12);
    }

    private static int calcEv(int a0) {
        int ev = (a0 >>> 24) & 0xf;
        if (ev == 0xf) {
            ev = 0x5;
        }
        ev *= 32;
        ev += (a0 & 0x1f);
        return ev;
    }

    private static int calcSpec(int a1) {
        int spec = 0;
        if (a1 == 0x301) {
            spec = 16;
        } else if (a1 == 0x302) {
            spec = 17;
        } else {
            for (int i = 0; i < 16; i++) {
                if ((a1 & (1 << i)) != 0) {
                    spec = i;
                    break;
                }
            }
        }
        return spec;
    }

    // ---- calling back into emulated code ----

    /** runs an emulated routine to completion, the way the BIOS would dispatch a handler */
    private void runSoftcall(int routine) {
        cpu.setPc(routine);
        cpu.r[31] = 0x80001000;
        setRamWord(0x1000, R3000.FUNCT_HLECALL);

        softcallTarget = false;
        int oldICount = cpu.getICount();
        while (!softcallTarget) {
            cpu.execute(10);
        }
        cpu.setICount(oldICount);
    }

    private void callIrqRoutine(int routine, int parameter) {
        if (!irqMutex) {
            irqMutex = true;
        } else {
            logger.log(Level.WARNING, "IOP: IRQ reentry");
            return;
        }

        System.arraycopy(cpu.r, 0, irqRegs, 0, 32);
        irqRegs[32] = cpu.hi;
        irqRegs[33] = cpu.lo;
        irqRegs[34] = cpu.pc;
        irqRegs[35] = cpu.delayv;
        irqRegs[36] = cpu.delayr;

        cpu.r[4] = parameter;
        runSoftcall(routine);

        System.arraycopy(irqRegs, 0, cpu.r, 0, 32);
        cpu.hi = irqRegs[32];
        cpu.lo = irqRegs[33];
        cpu.setPc(irqRegs[34]);
        cpu.delayv = irqRegs[35];
        cpu.delayr = irqRegs[36];

        irqMutex = false;
    }

    // ---- the PS1 kernel, in software ----

    private void biosException(int pc) {
        int a0 = cpu.r[4];

        switch (cpu.cp0r[R3000.CP0_CAUSE] & 0x3c) {
        case 0: { // IRQ
            System.arraycopy(cpu.r, 0, irqRegs, 0, 32);
            irqRegs[32] = cpu.hi;
            irqRegs[33] = cpu.lo;

            if ((irqData & 1) != 0) { // vsync
                if (counterEventStatus(3, 1) == EvStACTIVE) {
                    runSoftcall(counterEventHandler(3, 1));
                    irqData &= ~1; // the vbl irq is handled, so drop it
                }
            } else if ((irqData & 0x70) != 0) { // root counters
                for (int i = 0; i < 3; i++) {
                    if ((irqData & (1 << (i + 4))) != 0) {
                        if (counterEventStatus(i, 1) == EvStACTIVE) {
                            runSoftcall(counterEventHandler(i, 1));
                            irqData &= ~(1 << (i + 4));
                        }
                    }
                }
            }

            if (entryInt != 0) {
                write(0x1f801070, 0xffffffff, 0);

                a0 = entryInt;

                // RA (and PC)
                int ra = ramWord(a0);
                cpu.r[31] = ra;
                cpu.setPc(ra);
                // SP
                cpu.r[29] = ramWord(a0 + 4);
                // FP
                cpu.r[30] = ramWord(a0 + 8);
                // S0-S7
                for (int i = 0; i < 8; i++) {
                    cpu.r[16 + i] = ramWord(a0 + 12 + (i * 4));
                }
                // GP
                cpu.r[28] = ramWord(a0 + 44);
                // v0 = 1
                cpu.r[2] = 1;
            } else {
                write(0x1f801070, 0, 0xffff0000);

                // nothing else is going to bail us out, so return from the exception ourselves
                System.arraycopy(irqRegs, 0, cpu.r, 0, 32);
                cpu.hi = irqRegs[32];
                cpu.lo = irqRegs[33];
                cpu.setPc(cpu.cp0r[R3000.CP0_EPC]);

                int status = cpu.cp0r[R3000.CP0_SR];
                status = (status & 0xfffffff0) | ((status & 0x3c) >>> 2);
                cpu.cp0r[R3000.CP0_SR] = status;
            }
            break;
        }
        case 0x20: { // syscall
            int status = cpu.cp0r[R3000.CP0_SR];

            switch (a0) {
            case 1 -> status &= ~0x0404; // EnterCritical
            case 2 -> status |= 0x0404;  // ExitCritical
            default -> logger.log(Level.DEBUG, "HLEBIOS: unknown syscall %x".formatted(a0));
            }

            cpu.setPc(cpu.cp0r[R3000.CP0_EPC] + 4);

            status = (status & 0xfffffff0) | ((status & 0x3c) >>> 2);
            cpu.cp0r[R3000.CP0_SR] = status;
            break;
        }
        default:
            logger.log(Level.DEBUG, "HLEBIOS: unknown exception %x".formatted(cpu.cp0r[R3000.CP0_CAUSE]));
            break;
        }
    }

    @Override
    public void biosHle(int pc) {
        if (pc == 0 || pc == 0x80000000) { // the IOP null state
            logger.log(Level.DEBUG, "IOP 'null' state");
            songDone = true;
            return;
        }

        if (pc == 0xbfc00180 || pc == 0xbfc00184) { // an exception, not a BIOS call
            biosException(pc);
            return;
        }

        if (pc == 0x80001000) {
            softcallTarget = true;
            return;
        }

        int subcall = cpu.r[9] & 0xff;

        int a0 = cpu.r[4];
        int a1 = cpu.r[5];
        int a2 = cpu.r[6];
        int a3 = cpu.r[7];

        switch (pc) {
        case 0xa0:
            switch (subcall) {
            case 0x13: { // setjmp
                setRamWord(a0, cpu.r[31]);
                setRamWord(a0 + 4, cpu.r[29]);
                setRamWord(a0 + 8, cpu.r[30]);
                for (int i = 0; i < 8; i++) {
                    setRamWord(a0 + 12 + (i * 4), cpu.r[16 + i]);
                }
                setRamWord(a0 + 44, cpu.r[28]);
                cpu.r[2] = 0;
                break;
            }
            case 0x18: { // strncmp
                int result = 0;
                for (int i = 0; i < a2; i++) {
                    int c1 = ramByte(a0 + i);
                    int c2 = ramByte(a1 + i);
                    if (c1 != c2) {
                        result = c1 - c2;
                        break;
                    }
                    if (c1 == 0) {
                        break;
                    }
                }
                cpu.r[2] = result;
                break;
            }
            case 0x19: { // strcpy
                int i = 0;
                int c;
                while ((c = ramByte(a1 + i)) != 0) {
                    setRamByte(a0 + i, c);
                    i++;
                }
                // aosdk leaves the terminator to whatever was already there
                cpu.r[2] = a0;
                break;
            }
            case 0x28: { // bzero
                for (int i = 0; i < a1; i++) {
                    setRamByte(a0 + i, 0);
                }
                break;
            }
            case 0x2a: { // memcpy
                for (int i = 0; i < a2; i++) {
                    setRamByte(a0 + i, ramByte(a1 + i));
                }
                cpu.r[2] = a0;
                break;
            }
            case 0x2b: { // memset
                for (int i = 0; i < a2; i++) {
                    setRamByte(a0 + i, a1);
                }
                cpu.r[2] = a0;
                break;
            }
            case 0x2f: // rand
                cpu.r[2] = 1 + random.nextInt(32767);
                break;
            case 0x30: // srand
                random.setSeed(a0);
                break;
            case 0x33: { // malloc
                int chunk = heapAddr;

                // find a free block that is big enough
                while (Integer.compareUnsigned(a0, ramWord(chunk + BLK_SIZE)) > 0
                        || ramWord(chunk + BLK_STAT) == 1) {
                    // aosdk indexes the word array by a byte offset here, and this port keeps
                    // that so the heap comes out laid out the same way
                    chunk = ramAt(chunk + BLK_FD);
                }

                // split the free block
                int fd = chunk + 16 + a0; // the free block starts after the record and the allocation
                setRamWord(fd + BLK_STAT, ramWord(chunk + BLK_STAT));
                setRamWord(fd + BLK_SIZE, ramWord(chunk + BLK_SIZE) - a0);
                setRamWord(fd + BLK_FD, ramWord(chunk + BLK_FD));
                setRamWord(fd + BLK_BK, chunk);

                setRamWord(chunk + BLK_STAT, 1);
                setRamWord(chunk + BLK_SIZE, a0);
                setRamWord(chunk + BLK_FD, fd);

                cpu.r[2] = (chunk + 16) | 0x80000000;
                break;
            }
            case 0x39: // InitHeap
                heapAddr = a0 & 0x3fffffff;
                setRamWord(heapAddr + BLK_STAT, 0);
                setRamWord(heapAddr + BLK_FD, 0);
                setRamWord(heapAddr + BLK_BK, 0);
                if (((a0 & 0x1fffff) + a1) >= 2 * 1024 * 1024) {
                    setRamWord(heapAddr + BLK_SIZE, 0x1ffffc - (a0 & 0x1fffff));
                } else {
                    setRamWord(heapAddr + BLK_SIZE, a1);
                }
                break;
            case 0x3f: // printf
                logger.log(Level.TRACE, "HLEBIOS: printf: " + ramString(a0));
                break;
            case 0x72: // __96_remove
                break;
            default:
                logger.log(Level.DEBUG, "unknown BIOS A0 call = %x".formatted(subcall));
                break;
            }
            break;

        case 0xb0:
            switch (subcall) {
            case 0x07: { // DeliverEvent
                int ev = calcEv(a0);
                int spec = calcSpec(a1);

                if (eventStatus(ev, spec) != EvStACTIVE) {
                    // Nothing to deliver, but the call still has to return: aosdk leaves the
                    // function here instead, which skips the "PC = RA" at the bottom and leaves
                    // the pc on the HLECALL that got us here, so the next step runs the same
                    // call again and the song spins at 0xb0 for ever. Fourteen of the forty six
                    // tracks of Dragon Quest Monsters 1+2 are silent in aosdk for this reason.
                    break;
                }

                if (eventMode(ev, spec) == EvMdINTR) {
                    logger.log(Level.TRACE, "HLEBIOS: INTR type event, handler %x not called"
                            .formatted(eventHandler(ev, spec)));
                } else {
                    setEventStatus(ev, spec, EvStALREADY);
                }
                break;
            }
            case 0x08: { // OpenEvent
                int ev = calcEv(a0);
                int spec = calcSpec(a1);

                setEventStatus(ev, spec, EvStWAIT);
                setEventMode(ev, spec, a2);
                setEventHandler(ev, spec, a3);

                cpu.r[2] = ev | (spec << 8);
                break;
            }
            case 0x0a: { // WaitEvent
                setEventStatus(a0 & 0xff, (a0 >>> 8) & 0xff, EvStACTIVE);
                cpu.r[2] = 1;

                wai = 1;
                cpu.shortenFrame();
                break;
            }
            case 0x0b: { // TestEvent
                int ev = a0 & 0xff;
                int spec = (a0 >>> 8) & 0xff;

                int result;
                if (eventStatus(ev, spec) == EvStALREADY) {
                    setEventStatus(ev, spec, EvStACTIVE);
                    result = 1;
                } else {
                    result = 0;
                }

                wai = 1;

                cpu.r[2] = result;
                // v1 gets it too, which Crash 2 and 3 rely on
                cpu.r[3] = result;
                break;
            }
            case 0x0c: { // EnableEvent
                setEventStatus(a0 & 0xff, (a0 >>> 8) & 0xff, EvStACTIVE);
                cpu.r[2] = 1;
                break;
            }
            case 0x0d: { // DisableEvent
                setEventStatus(a0 & 0xff, (a0 >>> 8) & 0xff, EvStWAIT);
                cpu.r[2] = 1;
                break;
            }
            case 0x17: { // ReturnFromException
                System.arraycopy(irqRegs, 0, cpu.r, 0, 32);
                cpu.hi = irqRegs[32];
                cpu.lo = irqRegs[33];
                cpu.setPc(cpu.cp0r[R3000.CP0_EPC]);

                int status = cpu.cp0r[R3000.CP0_SR];
                status = (status & 0xfffffff0) | ((status & 0x3c) >>> 2);
                cpu.cp0r[R3000.CP0_SR] = status;
                return; // do not fall through to PC = RA
            }
            case 0x19: // HookEntryInt
                entryInt = a0;
                break;
            case 0x3f: // puts
                break;
            case 0x5b: // ChangeClearPAD
                break;
            default:
                logger.log(Level.DEBUG, "unknown BIOS B0 call = %x".formatted(subcall));
                break;
            }
            break;

        case 0xc0:
            switch (subcall) {
            case 0xa: // ChangeClearRCnt
                cpu.r[2] = ramWord((a0 << 2) + 0x8600);
                setRamWord((a0 << 2) + 0x8600, a1);
                break;
            default:
                logger.log(Level.DEBUG, "unknown BIOS C0 call = %x".formatted(subcall));
                break;
            }
            break;

        default:
            break;
        }

        // PC = RA
        cpu.setPc(cpu.r[31]);
    }

    // ---- printf, which the IOP kernel offers and some modules use to build strings ----

    /**
     * @param firstReg the cpu register the first variadic parameter is in, the way aosdk
     *                 walks the registers instead of the o32 stack
     */
    private String iopSprintf(String fmt, int firstReg) {
        StringBuilder out = new StringBuilder();
        int curparm = firstReg;

        int cf = 0;
        while (cf < fmt.length()) {
            char c = fmt.charAt(cf);
            if (c != '%') {
                if (c == 27) {
                    out.append("[ESC]");
                } else {
                    out.append(c);
                }
                cf++;
                continue;
            }

            cf++;
            StringBuilder tfmt = new StringBuilder("%");
            while (cf < fmt.length() && ((fmt.charAt(cf) >= '0' && fmt.charAt(cf) <= '9') || fmt.charAt(cf) == '.')) {
                tfmt.append(fmt.charAt(cf));
                cf++;
            }
            if (cf >= fmt.length()) {
                break;
            }
            char conv = fmt.charAt(cf);
            cf++;

            int value = curparm < 32 ? cpu.r[curparm] : 0;
            curparm++;

            // what was collected between the '%' and the conversion, e.g. "04" or "4.4"
            String spec = tfmt.substring(1);

            try {
                switch (conv) {
                case 'x' -> out.append(integer(spec, Integer.toUnsignedLong(value), 16, false, false));
                case 'X' -> out.append(integer(spec, Integer.toUnsignedLong(value), 16, true, false));
                case 'd', 'D' -> out.append(integer(spec, value, 10, false, true));
                case 'u', 'U' -> out.append(integer(spec, Integer.toUnsignedLong(value), 10, false, false));
                case 'c', 'C' -> out.append(pad(String.valueOf((char) value), spec));
                case 's' -> out.append((tfmt + "s").formatted(ramString(value)));
                case '%' -> {
                    out.append('%');
                    curparm--;
                }
                default -> {
                    out.append(tfmt).append(conv);
                    curparm--;
                }
                }
            } catch (RuntimeException e) {
                out.append(tfmt).append(conv);
            }
        }

        return out.toString();
    }

    /**
     * One of C's integer conversions, which {@link String#format} cannot spell: it has no
     * {@code %u} at all, and it rejects a precision on {@code %d} and {@code %x} - where C reads
     * one as "at least this many digits, zero filled". Square's IOP driver asks for its wave bank
     * as {@code wave%4.4u.wd}, so getting that wrong looks for {@code wave103.wd} instead of
     * {@code wave0103.wd} and the song plays silence.
     *
     * @param spec what stood between the '%' and the conversion; the caller only ever collects
     *             digits and dots, so it is {@code [0]?width[.precision]}
     */
    static String integer(String spec, long value, int radix, boolean upperCase, boolean signed) {
        boolean negative = signed && value < 0;
        String digits = Long.toString(negative ? -value : value, radix);
        if (upperCase) {
            digits = digits.toUpperCase();
        }

        boolean zeroPad = spec.startsWith("0");
        int dot = spec.indexOf('.');
        int width = number(dot < 0 ? spec : spec.substring(0, dot));
        int precision = dot < 0 ? -1 : number(spec.substring(dot + 1));

        StringBuilder sb = new StringBuilder(digits);
        if (precision >= 0) {
            zeroPad = false; // a precision makes the zero flag mean nothing, as in C
            while (sb.length() < precision) {
                sb.insert(0, '0');
            }
        }

        String sign = negative ? "-" : "";
        if (zeroPad) {
            while (sign.length() + sb.length() < width) {
                sb.insert(0, '0');
            }
        }

        sb.insert(0, sign);
        while (sb.length() < width) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }

    /** the width of a conversion that has nothing to zero fill */
    private static String pad(String value, String spec) {
        int dot = spec.indexOf('.');
        int width = number(dot < 0 ? spec : spec.substring(0, dot));
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < width) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }

    private static int number(String s) {
        int value = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    // ---- the PS2 IOP kernel, in software ----

    @Override
    public void iopCall(int pc, int callNumber) {
        int a0 = cpu.r[4];
        int a1 = cpu.r[5];
        int a2 = cpu.r[6];
        int a3 = cpu.r[7];

        // the module a call belongs to is named by the link signature just above it
        int scan = (pc & 0x0fffffff) / 4;
        while (ramAt(scan) != 0x41e00000 && scan >= (0x10000 / 4)) {
            scan--;
        }

        if (ramAt(scan) != 0x41e00000) {
            logger.log(Level.ERROR, "couldn't find IOP link signature");
            return;
        }

        scan += 3; // skip zero and version
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int ch = (ramAt(scan + (i / 4)) >>> ((i % 4) * 8)) & 0xff;
            if (ch == 0) break;
            nameBuilder.append((char) ch);
        }
        String name = nameBuilder.toString();

        switch (name) {
        case "stdio" -> {
            if (callNumber == 4) { // printf
                logger.log(Level.TRACE, "IOP printf: " + iopSprintf(ramString(a0), 5));
            } else {
                unhandled(callNumber, name);
            }
        }
        case "sifman" -> {
            switch (callNumber) {
            case 5 -> cpu.r[2] = 0;   // sceSifInit
            case 7 -> cpu.r[2] = 1;   // sceSifSetDma, nonzero is success
            case 8 -> cpu.r[2] = -1;  // sceSifDmaStat, dma completed
            case 29 -> cpu.r[2] = 1;  // sceSifCheckInit
            default -> unhandled(callNumber, name);
            }
        }
        case "thbase" -> iopThbase(callNumber, a0, a1, a2, a3, name);
        case "thevent" -> iopThevent(callNumber, a0, a1, name);
        case "thsemap" -> iopThsemap(callNumber, a0, name);
        case "timrman" -> iopTimrman(callNumber, a0, a1, a2, a3, name);
        case "sysclib" -> iopSysclib(callNumber, a0, a1, a2, name);
        case "intrman" -> iopIntrman(callNumber, a0, a1, a2, a3, name);
        case "loadcore" -> {
            switch (callNumber) {
            case 5 -> { /* FlushDcache */ }
            case 6 -> { // RegisterLibraryEntries
                int p = a0 & 0x1fffff;
                if (ramWord(p) == 0x41c00000) {
                    p += 3 * 4;
                    StringBuilder lib = new StringBuilder();
                    for (int i = 0; i < 8; i++) {
                        int ch = ramByte(p + i);
                        if (ch == 0) break;
                        lib.append((char) ch);
                    }
                    p += 2 * 4;
                    regLibs[iNumLibs].name = lib.toString();
                    regLibs[iNumLibs].dispatch = p;
                    iNumLibs++;
                } else {
                    logger.log(Level.ERROR, "entry table signature missing");
                }
                cpu.r[2] = 0;
            }
            default -> unhandled(callNumber, name);
            }
        }
        case "sysmem" -> iopSysmem(callNumber, a0, a1, name);
        case "modload" -> iopModload(callNumber, a0, a1, a2, name);
        case "ioman" -> iopIoman(callNumber, a0, a1, a2, name);
        default -> {
            for (int lib = 0; lib < iNumLibs; lib++) {
                if (name.equals(regLibs[lib].name)) {
                    // the call happens in a delay slot, so the pending branch has to go
                    cpu.delayv = 0;
                    cpu.delayr = 0;

                    int target = ramWord(regLibs[lib].dispatch + callNumber * 4);
                    cpu.setPc(target - 4);
                    return;
                }
            }
            unhandled(callNumber, name);
        }
        }
    }

    private void unhandled(int callNumber, String name) {
        logger.log(Level.DEBUG, "IOP: unhandled service %d for module %s".formatted(callNumber, name));
    }

    private void iopThbase(int callNumber, int a0, int a1, int a2, int a3, String name) {
        switch (callNumber) {
        case 4: { // CreateThread
            int p = (a0 & 0x1fffff) / 4;

            int newAlloc = filesystem.getLoadAddr();
            if ((newAlloc & 0xf) != 0) { // 16 byte alignment
                newAlloc &= ~0xf;
                newAlloc += 16;
            }
            filesystem.setLoadAddr(newAlloc + ramAt(p + 3));

            threads[iNumThreads].iState = TS_CREATED;
            threads[iNumThreads].stackloc = newAlloc;
            threads[iNumThreads].flags = ramAt(p);
            threads[iNumThreads].routine = ramAt(p + 2);
            threads[iNumThreads].stacksize = ramAt(p + 3);
            threads[iNumThreads].refCon = ramAt(p + 4);

            cpu.r[2] = iNumThreads;
            iNumThreads++;
            break;
        }
        case 6: // StartThread
            freezeThread(iCurThread, true);
            thawThread(a0);
            iCurThread = a0;
            break;
        case 20: // GetThreadID
            cpu.r[2] = iCurThread;
            break;
        case 24: // SleepThread
            freezeThread(iCurThread, true);
            threads[iCurThread].iState = TS_SLEEPING;
            iCurThread = -1;
            reschedule();
            break;
        case 25: // WakeupThread
            threads[a0].iState = TS_READY;
            break;
        case 26: // iWakeupThread
            if (threads[a0].iState != TS_RUNNING) {
                threads[a0].iState = TS_READY;
            }
            break;
        case 33: { // DelayThread
            int usec = a0;
            if (Integer.compareUnsigned(usec, 100) < 0) {
                usec = 100;
            }
            double ticks = (double) (usec & 0xffffffffL);

            freezeThread(iCurThread, true);
            threads[iCurThread].iState = TS_WAITDELAY;
            ticks /= 1000000.0;
            ticks *= 36864000.0; // 768*48000, the IOP native mode clock
            threads[iCurThread].waitparm = (int) (long) ticks;
            iCurThread = -1;

            reschedule();
            break;
        }
        case 34: { // GetSystemTime
            int p = (a0 & 0x1fffff) / 4;
            setRamAt(p, (int) sysTime);
            setRamAt(p + 1, (int) (sysTime >>> 32));
            cpu.r[2] = 0;
            break;
        }
        case 39: { // USec2SysClock
            long ticks = a0 & 0xffffffffL;
            ticks *= 36864000L;
            ticks /= 1000000L;
            setRamAt((a1 & 0x1fffff) / 4, (int) ticks);
            setRamAt((a1 & 0x1fffff) / 4 + 1, (int) (ticks >>> 32));
            cpu.r[2] = 0;
            break;
        }
        case 40: { // SysClock2USec
            int p0 = (a0 & 0x1fffff) / 4;
            int p1 = (a1 & 0x1fffff) / 4;
            int p2 = (a2 & 0x1fffff) / 4;

            long temp = ramAt(p0) & 0xffffffffL;
            temp |= (long) ramAt(p0 + 1) << 32;

            temp *= 1000000L;
            temp /= 36864000L;

            setRamAt(p1, (int) (temp / 1000000L));
            setRamAt(p2, (int) (temp % 1000000L));
            break;
        }
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopThevent(int callNumber, int a0, int a1, String name) {
        switch (callNumber) {
        case 4: { // CreateEventFlag
            int p = (a0 & 0x1fffff) / 4;
            evflags[iNumFlags].type = ramAt(p);
            evflags[iNumFlags].value = ramAt(p + 1);
            evflags[iNumFlags].param = ramAt(p + 2);
            evflags[iNumFlags].inUse = true;

            cpu.r[2] = iNumFlags + 1;
            iNumFlags++;
            break;
        }
        case 6: // SetEventFlag
            evflags[a0 - 1].value |= a1;
            cpu.r[2] = 0;
            break;
        case 7: { // iSetEventFlag
            int f = a0 - 1;
            evflags[f].value |= a1;
            cpu.r[2] = 0;
            for (int i = 0; i < iNumThreads; i++) {
                if (threads[i].iState == TS_WAITEVFLAG && threads[i].waitparm == f) {
                    threads[i].iState = TS_READY;
                }
            }
            break;
        }
        case 8: // ClearEventFlag
            evflags[a0 - 1].value &= a1;
            cpu.r[2] = 0;
            break;
        case 9: // iClearEventFlag
            evflags[a0 - 1].value &= a1;
            cpu.r[2] = 0;
            break;
        case 10: { // WaitEventFlag
            int f = a0 - 1;
            if ((evflags[f].value & a1) == 0) {
                freezeThread(iCurThread, true);
                threads[iCurThread].iState = TS_WAITEVFLAG;
                threads[iCurThread].waitparm = f;
                iCurThread = -1;
                reschedule();
            } else {
                cpu.r[2] = 0;
            }
            break;
        }
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopThsemap(int callNumber, int a0, String name) {
        switch (callNumber) {
        case 4: { // CreateSema
            int slot = -1;
            for (int i = 0; i < SEMA_MAX; i++) {
                if (!semaphores[i].inuse) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1) {
                logger.log(Level.WARNING, "IOP: out of semaphores");
            } else {
                int p = (a0 & 0x7fffffff) / 4;
                semaphores[slot].attr = ramAt(p);
                semaphores[slot].option = ramAt(p + 1);
                semaphores[slot].init = ramAt(p + 2);
                semaphores[slot].max = ramAt(p + 3);
                semaphores[slot].current = semaphores[slot].init;
                semaphores[slot].inuse = true;
            }

            cpu.r[2] = slot;
            break;
        }
        case 6, 7: { // SignalSema, iSignalSema
            boolean foundThread = false;
            for (int i = 0; i < iNumThreads; i++) {
                if (threads[i].iState == TS_WAITSEMA && threads[i].waitparm == a0) {
                    threads[i].iState = TS_READY;
                    semaphores[a0].threadsWaiting--;
                    foundThread = true;
                    break;
                }
            }

            int result = 0;
            if (!foundThread) {
                if (semaphores[a0].current < semaphores[a0].max) {
                    semaphores[a0].current++;
                } else {
                    result = -420; // semaphore overflow
                }
            }
            cpu.r[2] = result;
            break;
        }
        case 8: // WaitSema
            if (semaphores[a0].current > 0) {
                semaphores[a0].current--;
            } else {
                freezeThread(iCurThread, true);
                threads[iCurThread].iState = TS_WAITSEMA;
                threads[iCurThread].waitparm = a0;
                reschedule();
            }
            cpu.r[2] = 0;
            break;
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopTimrman(int callNumber, int a0, int a1, int a2, int a3, String name) {
        switch (callNumber) {
        case 4: // AllocHardTimer
            if (a1 != 32) {
                logger.log(Level.WARNING, "IOP: AllocHardTimer does not support 16 bit timers");
            }
            iopTimers[iNumTimers].source = a0;
            iopTimers[iNumTimers].prescale = a2;
            cpu.r[2] = iNumTimers + 1;
            iNumTimers++;
            break;
        case 6: // FreeHardTimer
            cpu.r[2] = 0;
            break;
        case 10: // GetTimerCounter
            cpu.r[2] = iopTimers[a0 - 1].count;
            break;
        case 20: // SetTimerHandler
            iopTimers[a0 - 1].target = a1;
            iopTimers[a0 - 1].handler = a2;
            iopTimers[a0 - 1].hparam = a3;
            cpu.r[2] = 0;
            break;
        case 22: // SetupHardTimer
            iopTimers[a0 - 1].source = a1;
            iopTimers[a0 - 1].mode = a2;
            iopTimers[a0 - 1].prescale = a3;
            cpu.r[2] = 0;
            break;
        case 23: // StartHardTimer
            iopTimers[a0 - 1].iActive = 1;
            iopTimers[a0 - 1].count = 0;
            cpu.r[2] = 0;
            break;
        case 24: // StopHardTimer
            iopTimers[a0 - 1].iActive = 0;
            cpu.r[2] = 0;
            break;
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopSysclib(int callNumber, int a0, int a1, int a2, String name) {
        switch (callNumber) {
        case 12: { // memcpy
            for (int i = 0; i < a2; i++) {
                setRamByte(a0 + i, ramByte(a1 + i));
            }
            cpu.r[2] = a0;
            break;
        }
        case 13: { // memmove
            for (int i = a2 - 1; i >= 0; i--) {
                setRamByte(a0 + i, ramByte(a1 + i));
            }
            cpu.r[2] = a0;
            break;
        }
        case 14: { // memset
            for (int i = 0; i < a2; i++) {
                setRamByte(a0 + i, a1);
            }
            break;
        }
        case 17: { // bzero
            for (int i = 0; i < a1; i++) {
                setRamByte(a0 + i, 0);
            }
            break;
        }
        case 19: { // sprintf
            String result = iopSprintf(ramString(a1), 6); // a2 is the first parameter
            setRamString(a0, result);
            break;
        }
        case 23: { // strcpy
            int i = 0;
            int c;
            while ((c = ramByte(a1 + i)) != 0) {
                setRamByte(a0 + i, c);
                i++;
            }
            setRamByte(a0 + i, 0);
            cpu.r[2] = a0;
            break;
        }
        case 27: // strlen
            cpu.r[2] = ramString(a0).length();
            break;
        case 30: { // strncpy
            int i = 0;
            int c;
            int n = a2;
            while (n > 0 && (c = ramByte(a1 + i)) != 0) {
                setRamByte(a0 + i, c);
                i++;
                n--;
            }
            setRamByte(a0 + i, 0);
            cpu.r[2] = a0;
            break;
        }
        case 36: { // strtol
            if (a1 != 0) {
                logger.log(Level.DEBUG, "IOP: strtol with a non-null second parameter");
            }
            String s = ramString(a0).trim();
            int radix = a2 == 0 ? 10 : a2;
            int value = 0;
            int end = 0;
            if (end < s.length() && (s.charAt(end) == '-' || s.charAt(end) == '+')) {
                end++;
            }
            while (end < s.length() && Character.digit(s.charAt(end), radix) >= 0) {
                end++;
            }
            if (end > 0) {
                try {
                    value = (int) Long.parseLong(s.substring(0, end), radix);
                } catch (NumberFormatException e) {
                    value = 0;
                }
            }
            cpu.r[2] = value;
            break;
        }
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopIntrman(int callNumber, int a0, int a1, int a2, int a3, String name) {
        switch (callNumber) {
        case 4: // RegisterIntrHandler
            if (a0 == 9) {
                irq9Fval = a1;
                irq9Cb = a2;
                irq9Flag = a3;
            }
            if (a0 == 36) { // DMA4
                dma4Fval = a1;
                dma4Cb = a2;
                dma4Flag = a3;
            }
            if (a0 == 40) { // DMA7
                dma7Fval = a1;
                dma7Cb = a2;
                dma7Flag = a3;
            }
            break;
        case 5, 6, 7, 8, 9: // Release/Enable/Disable/CpuDisable/CpuEnable Intr
            break;
        case 17: // CpuSuspendIntr
            cpu.r[2] = intrSusp ? -102 : 0;
            intrSusp = true;
            break;
        case 18: // CpuResumeIntr
            intrSusp = false;
            cpu.r[2] = 0;
            break;
        case 23: // QueryIntrContext
            cpu.r[2] = 0;
            break;
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopSysmem(int callNumber, int a0, int a1, String name) {
        switch (callNumber) {
        case 4: { // AllocMemory
            int newAlloc = filesystem.getLoadAddr();
            if ((newAlloc & 15) != 0) {
                newAlloc &= ~15;
                newAlloc += 16;
            }

            int size = a1;
            if ((size & 15) != 0) {
                size &= ~15;
                size += 16;
            }

            if (size == 1114112) { // the Shadow Hearts rip assumes where its buffer lands
                logger.log(Level.DEBUG, "SH hack: was %x now %x".formatted(newAlloc, 0x60000));
                newAlloc = 0x60000;
            }

            filesystem.setLoadAddr(newAlloc + size);
            cpu.r[2] = newAlloc;
            break;
        }
        case 5: // FreeMemory
            break;
        case 7, 8: // QueryMaxFreeMemSize, QueryTotalFreeMemSize
            cpu.r[2] = (2 * 1024 * 1024) - filesystem.getLoadAddr();
            break;
        case 14: { // Kprintf
            String out = iopSprintf(ramString(a0), 5);
            logger.log(Level.TRACE, "KTTY: " + out.replace((char) 27, ']'));
            break;
        }
        default:
            unhandled(callNumber, name);
            break;
        }
    }

    private void iopModload(int callNumber, int a0, int a1, int a2, String name) {
        if (callNumber != 7) { // LoadStartModule
            unhandled(callNumber, name);
            return;
        }

        String moduleName = ramString((a0 & 0x1fffff) + 8);

        // 2k for the arguments
        int newAlloc = filesystem.getLoadAddr();
        if ((newAlloc & 0xf) != 0) {
            newAlloc &= ~0xf;
            newAlloc += 16;
        }
        filesystem.setLoadAddr(newAlloc + 2048);

        byte[] tempmem = new byte[2 * 1024 * 1024];
        if (filesystem.loadFile(moduleName, tempmem, tempmem.length) != Psf2Filesystem.NOT_FOUND) {
            int start = filesystem.loadElf(tempmem, tempmem.length);

            if (start != Psf2Filesystem.NOT_FOUND) {
                int[] args = new int[20];
                int numargs = 1;
                args[0] = a0; // the program name is argv[0]

                int argofs = 0;
                int len = a1;
                if (len > 0) {
                    args[numargs] = a2;
                    numargs++;

                    while (len != 0) {
                        if (ramByte(a2 + argofs) == 0 && len > 1 && numargs < args.length) {
                            args[numargs] = a2 + argofs + 1;
                            numargs++;
                        }
                        argofs++;
                        len--;
                    }
                }

                for (int i = 0; i < numargs; i++) {
                    setRamAt((newAlloc / 4) + i, args[i]);
                }

                cpu.r[4] = numargs;
                cpu.r[5] = 0x80000000 | newAlloc;

                // RA stays put, the pc goes to the module, and we are in a delay slot
                cpu.setPc(start - 4);
            }
        }
    }

    private void iopIoman(int callNumber, int a0, int a1, int a2, String name) {
        switch (callNumber) {
        case 4: { // open
            int slot = -1;
            for (int i = 0; i < MAX_FILE_SLOTS; i++) {
                if (fileStat[i] == 0) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1) {
                logger.log(Level.WARNING, "IOP: out of file slots");
                cpu.r[2] = 0xffffffff;
                return;
            }

            String fileName = ramString(a0 & 0x1fffff);
            if (fileName.startsWith("aofile:") || fileName.startsWith("hefile:")) {
                fileName = fileName.substring(8);
            } else if (fileName.startsWith("host0:")) {
                fileName = fileName.substring(7);
            }

            fileData[slot] = new byte[6 * 1024 * 1024];
            fileSize[slot] = filesystem.loadFile(fileName, fileData[slot], fileData[slot].length);
            filePos[slot] = 0;
            fileStat[slot] = 1;

            cpu.r[2] = fileSize[slot] == Psf2Filesystem.NOT_FOUND ? Psf2Filesystem.NOT_FOUND : slot;
            break;
        }
        case 5: // close
            fileData[a0] = null;
            filePos[a0] = 0;
            fileSize[a0] = 0;
            fileStat[a0] = 0;
            break;
        case 6: { // read
            if (filePos[a0] >= fileSize[a0]) {
                cpu.r[2] = 0;
            } else {
                int len = a2;
                if (filePos[a0] + len > fileSize[a0]) {
                    len = fileSize[a0] - filePos[a0];
                }
                for (int i = 0; i < len; i++) {
                    setRamByte(a1 + i, fileData[a0][filePos[a0] + i]);
                }
                filePos[a0] += len;
                cpu.r[2] = len;
            }
            break;
        }
        case 8: // lseek
            switch (a2) {
            case 0 -> { // SEEK_SET
                if (a1 <= fileSize[a0]) {
                    filePos[a0] = a1;
                }
            }
            case 1 -> { // SEEK_CUR
                if (a1 + filePos[a0] < fileSize[a0]) {
                    filePos[a0] += a1;
                }
            }
            case 2 -> filePos[a0] = fileSize[a0] - a1; // SEEK_END
            default -> { /* nothing */ }
            }
            cpu.r[2] = filePos[a0];
            break;
        case 20, 21: // AddDrv, DelDrv
            cpu.r[2] = 0;
            break;
        default:
            unhandled(callNumber, name);
            break;
        }
    }
}
