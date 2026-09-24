package mdplayer.emu.nise68;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


class NiseIOCS {

    private static final Logger logger = getLogger(NiseIOCS.class.getName());

    private final Memory68 mem;
    private final Register68 reg;
    private final Runnable[] cmdTbl;
    public int interruptOPM;

    public NiseIOCS(Memory68 mem, Register68 reg) {
        this.mem = mem;
        this.reg = reg;
        cmdTbl = new Runnable[] {
                // 00
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, this::_DEFCHR,
                // 10
                null, null, null, null, null, null, null, null, null, this::_FNTGET, null, null, null, null, null, null,
                // 20
                null, null, null, this::_B_LOCATE, null, null, null, null, null, null, this::_B_CLR_ST, null, null, null, null, null,
                // 30
                this::_SET232C, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // 40
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // 50
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // 60
                this::_ADPCMOUT, null, null, null, null, null, null, this::_ADPCMMOD, this::_OPMSET, null, this::_OPMINTST, null, null, null, null, null,
                // 70
                this::_MS_INIT, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // 80
                this::_B_INTVCS, this::_B_SUPER, null, null, null, null, null, null, null, null, this::_DMAMOVE, null, null, null, null, null,
                // 90
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // a0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // b0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // c0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // d0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // e0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // f0
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        };
    }

    public void call() {
        int n = reg.getD()[0] & 0xff;
        if (cmdTbl[n] != null) {
            cmdTbl[n].run();
        } else {
            throw new UnsupportedOperationException("IOCS is not implemented!! [%04x]".formatted(n));
        }
    }

    private void _DEFCHR() {
        logger.log(Level.TRACE, "IOCS _DEFCHR");

        reg.setSR( mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        short fontSize = (short) (reg.getDl(1) >> 16);
        int jisCode = reg.getDw(1);
        int ptr = reg.getAl(1);
    }

    private void _FNTGET() {
        logger.log(Level.TRACE, "IOCS _FNTGET");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        short fontSize = (short) (reg.getDl(1) >> 16);
        int jisCode = reg.getDw(1);
        int ptr = reg.getAl(1);
    }

    private void _B_CLR_ST() {
        logger.log(Level.TRACE, "IOCS _B_CLR_ST");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        byte clrArea = reg.getDb(1);
    }

    /** text cursor position: there is no text screen, so a move is dropped and a query (d1 = -1) returns 0,0 */
    private void _B_LOCATE() {
        logger.log(Level.TRACE, "IOCS _B_LOCATE");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        reg.setDl(0, 0x0000_0000); // previous position x << 16 | y
    }

    private void _SET232C() {
        logger.log(Level.TRACE, "IOCS _SET232C");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        short settingNumber = reg.getDw(1);

        reg.setDl(0, 0x0000_0000); // Returns the previous setting
    }

    private void _ADPCMOUT() {
        logger.log(Level.TRACE, "IOCS _ADPCMOUT");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        short frq_pan = reg.getDw(1);
        int length = reg.getDl(2);
        int ptr = reg.getAl(1);
    }

    private void _ADPCMMOD() {
        logger.log(Level.TRACE, "IOCS _ADPCMMOD");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        int mode = reg.getDl(1); // 0 stop 1 pause 2 resume
    }

    private void _OPMSET() {
        logger.log(Level.TRACE, "IOCS _OPMSET");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        byte rAdr = reg.getDb(1);
        byte rDat = reg.getDb(2);
    }

    private void _OPMINTST() {
        logger.log(Level.TRACE, "IOCS _OPMINTST");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        interruptOPM = reg.getAl(1);
        reg.setDl(0, 0);
    }

    private void _MS_INIT() {
        logger.log(Level.TRACE, "IOCS _MS_INIT");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        // Mouse initialization
    }

    private void _B_INTVCS() {
        logger.log(Level.TRACE, "IOCS _B_INTVCS");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        short vctNum = reg.getDw(1);
        int ptr = reg.getAl(1);
        reg.setDl(0, mem.peekL((vctNum & 0xffff) * 4));
        mem.pokeL((vctNum & 0xffff) * 4, ptr);
    }

    private void _B_SUPER() {
        logger.log(Level.TRACE, "IOCS _B_SUPER");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        if (reg.getA().get(1) == 0) {
            reg.setSR((short) (reg.getSR() | 0x2000)); // super
            // reg.getD()[0] = -;
        } else {
            reg.setSR((short) (reg.getSR() & 0xdfff)); // user
            reg.getD()[0] = 0;
        }
    }

    private void _DMAMOVE() {
        logger.log(Level.TRACE, "IOCS _DMAMOVE");

        reg.setSR(mem.peekW(reg.getSSP()));
        reg.setSSP(reg.getSSP() + 2);
        reg.pc = mem.peekL(reg.getSSP());
        reg.setSSP(reg.getSSP() + 4);

        int cm = reg.getDb(1) & 0x80;
        int a1m = (reg.getDb(1) & 0x0c) >> 2;
        int a2m = reg.getDb(1) & 0x03;
        int size = reg.getDl(2);
        int a1 = reg.getAl(1);
        int a2 = reg.getAl(2);

        while (size > 0) {
            int src = cm == 0 ? a1 : a2;
            int dst = cm == 0 ? a2 : a1;
            byte b = mem.peekB(src);
            mem.pokeB(dst, b);
            a1 = a1 + (a1m == 0 ? 0 : (a1m == 1 ? 1 : -1));
            a2 = a2 + (a2m == 0 ? 0 : (a2m == 1 ? 1 : -1));
            size--;
        }
    }
}
