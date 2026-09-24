package mdplayer.emu.nise68;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.function.Function;

import static java.lang.System.getLogger;


public class NiseM68 {

    private static final Logger logger = getLogger(NiseM68.class.getName());

    /**
     * Disassembly text is built on every emulated instruction, so the trace level is
     * resolved once and folded away by the JIT while tracing is off.
     */
    private static final boolean TRACE = logger.isLoggable(Level.TRACE);

    private final Memory68 mem;
    private final Register68 reg;
    public NiseHuman hmn;
    private final Function<Short, Integer>[] cmdTbl;

    @SuppressWarnings("unchecked")
    public NiseM68(Memory68 mem, Register68 reg) {
        this.mem = mem;
        this.reg = reg;
        cmdTbl = Arrays.<Function<Short, Integer>>asList(
                // 00
                this::cori, this::cbsetbtst, this::candi, this::cbsetbtst, this::csubi, this::cbsetbtst, this::caddi, this::cbsetbtst,
                this::cbtst08, this::cbsetbtst, this::ceori, this::cbsetbtst, this::ccmpi, this::cbsetbtst, null, this::cbsetbtst,
                // 10
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                // 20
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                // 30
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove, this::cmove,
                // 40
                this::cmove, this::clea, this::cclr, this::clea, this::cmoveccr, this::clea, this::cmoveToSR, this::clea,
                this::cpea, this::clea, this::ctst, this::clea, this::cmovem, this::clea, this::crts, this::clea,
                // 50
                this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs,
                this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs, this::caqsqdbs,
                // 60
                this::cbra, this::cbsr, this::cbra, this::cbra, this::cbra, this::cbra, this::cbra, this::cbra,
                this::cbra, this::cbra, this::cbra, this::cbra, this::cbra, this::cbra, this::cbra, this::cbra,
                // 70
                this::cmoveq, null, this::cmoveq, null, this::cmoveq, null, this::cmoveq, null,
                this::cmoveq, null, this::cmoveq, null, this::cmoveq, null, this::cmoveq, null,
                // 80
                this::cor, this::cor, this::cor, this::cor, this::cor, this::cor, this::cor, this::cor,
                this::cor, this::cor, this::cor, this::cor, this::cor, this::cor, this::cor, this::cor,
                // 90
                this::csub, this::csub, this::csub, this::csub, this::csub, this::csub, this::csub, this::csub,
                this::csub, this::csub, this::csub, this::csub, this::csub, this::csub, this::csub, this::csub,
                // a0
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                // b0
                this::ccmp, this::ceor, this::ccmp, this::ceor, this::ccmp, this::ceor, this::ccmp, this::ceor,
                this::ccmp, this::ceor, this::ccmp, this::ceor, this::ccmp, this::ceor, this::ccmp, this::ceor,
                // c0
                this::cmulu, this::cand, this::cmulu, this::cand, this::cmulu, this::cand, this::cmulu, this::cand,
                this::cmulu, this::cand, this::cmulu, this::cand, this::cmulu, this::cand, this::cmulu, this::cand,
                // d0
                this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd,
                this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd, this::cadd,
                // e0
                this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift,
                this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift, this::cshift,
                // f0
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, this::cfeFunc, this::cdos
        ).toArray(Function[]::new);
    }

    public int stepExecute() {
        short n = fetchW();
        int cycle = 0;

        if (cmdTbl[(n & 0xffff) >> 8] != null) {
            cycle += cmdTbl[(n & 0xffff) >> 8].apply(n);
        } else {
            throw new UnsupportedOperationException("not implemented yet!! [%04x]".formatted(n & 0xffff));
        }

        return cycle;
    }

    private byte fetchB() {
        byte n = mem.peekB(reg.pc);
        reg.pc++;
        return n;
    }

    private short fetchW() {
        short n = mem.peekW(reg.pc);
        reg.pc += 2;
        return n;
    }

    private int fetchL() {
        int n = mem.peekL(reg.pc);
        reg.pc += 4;
        return n;
    }

    private int cori(short n) {
        if (n == 0x007c) {
            return coriToSr(n);
        }

        int size = (n & 0x00c0) >> 6;

        return switch (size) {
            case 0 -> // byte
                    corib(n);
            case 1 -> // word
                    coriw(n);
            case 2 -> // long
                    coril(n);
            default ->
                    throw new UnsupportedOperationException("Invalid ORI size %d at PC: %08x, opcode: %04x".formatted(size, reg.pc - 2, n & 0xffff));
        };
    }

    private int corib(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ORI.b ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        byte val = (byte) fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(val);
//#endif

        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = (short) (reg.getDb(r) & 0xff);
                after = (short) ((val & 0xff) | (before & 0xffff));
                reg.setDb(r, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Ori_b[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) | (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_b[1];
                break;
            case 3: // (An)+
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) | (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Ori_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) | (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = (short) (mem.peekB(reg.getA().get(r) + d16) & 0xff);
                after = (short) ((val & 0xff) | (before & 0xffff));
                mem.pokeB(reg.getA().get(r) + d16, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Ori_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekB(ptr);
                after = (short) ((val & 0xff) | (before & 0xffff));
                mem.pokeB(ptr, (byte) after);
                cycle = Cycle.Ori_b[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr & 0xffff);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((val & 0xff) | (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Ori_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((val & 0xff) | (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Ori_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int coriw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ORI.w ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        short val = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%04x,".formatted(val);
//#endif


        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getDw(r);
                after = (short) ((val & 0xffff) | (before & 0xffff));
                reg.setDw(r, after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Ori_w[0];
                break;
            case 2: // (An)
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) | (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_w[1];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) | (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Ori_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) | (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = mem.peekW(reg.getA().get(r) + d16);
                after = (short) ((val & 0xffff) | (before & 0xffff));
                mem.pokeW(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Ori_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekW(ptr);
                after = (short) ((val & 0xffff) | (before & 0xffff));
                mem.pokeW(ptr, after);
                cycle = Cycle.Ori_w[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) | (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.Ori_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) | (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.Ori_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int coril(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ORI.l ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        int val = fetchL();
//#if DEBUG
        if (TRACE) nimo += "#$%08x,".formatted(val);
//#endif

        int after = 0;
        int before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getD()[r];
                after = val | before;
                reg.getD()[r] = after;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Ori_l[0];
                break;
            case 2: // (An)
                before = mem.peekL(reg.getA().get(r));
                after = val | before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_l[1];
                break;
            case 3: // (An)+
                before = mem.peekL(reg.getA().get(r));
                after = val | before;
                mem.pokeL(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Ori_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                before = mem.peekL(reg.getA().get(r));
                after = val | before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Ori_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = mem.peekL(reg.getA().get(r) + d16);
                after = val | before;
                mem.pokeL(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16 & 0xffff, r);
//#endif

                cycle = Cycle.Ori_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekL(ptr);
                after = val | before;
                mem.pokeL(ptr, after);
                cycle = Cycle.Ori_l[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr & 0xffff);
//#endif

                        before = mem.peekL(ptr);
                        after = val | before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Ori_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val | before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Ori_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int coriToSr(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ORI.w ";
//#endif

        short val = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%04x,sr".formatted(val & 0xffff);
//#endif


        reg.setSR((short) ((reg.getSR() & 0xffff) | (val & 0b1010_0111_0001_1111)));

        //if ((val & 0b1000_0000_0000_0000) != 0) reg.T = true;
        //if ((val & 0b0010_0000_0000_0000) != 0) reg.S = true;
        //if ((val & 0b0000_0100_0000_0000) != 0) reg.SR|= 0b0000_0100_0000_0000; // I2 flag
        //if ((val & 0b0000_0010_0000_0000) != 0) reg.SR|= 0b0000_0010_0000_0000; // I1 flag
        //if ((val & 0b0000_0001_0000_0000) != 0) reg.SR|= 0b0000_0001_0000_0000; // I0 flag
        //if ((val & 0b0000_0000_0001_0000) != 0) reg.X = true;
        //if ((val & 0b0000_0000_0000_1000) != 0) reg.setN(true);
        //if ((val & 0b0000_0000_0000_0100) != 0) reg.setZ(true);
        //if ((val & 0b0000_0000_0000_0010) != 0) reg.V = true;
        //if ((val & 0b0000_0000_0000_0001) != 0) reg.C = true;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return 20;
    }

    private int candi(short n) {
        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> // byte
                    candib(n);
            case 1 -> // word
                    candiw(n);
            case 2 -> // long
                    candil(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };
    }

    private int candib(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ANDI.b ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        byte val = (byte) fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(val & 0xff);
//#endif

        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getDb(r);
                after = (short) ((val & 0xff) & (before & 0xffff));
                reg.setDb(r, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Andi_b[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_b[1];
                break;
            case 3: // (An)+
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Andi_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = mem.peekB(reg.getA().get(r) + d16);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r) + d16, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Andi_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekB(ptr);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(ptr, (byte) after);
                cycle = Cycle.Andi_b[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekB(ptr);
                        after = (short) ((val & 0xff) & (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Andi_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((val & 0xff) & (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Andi_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candiw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ANDI.w ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        short val = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%04x,".formatted(val & 0xffff);
//#endif


        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getDw(r);
                after = (short) ((val & 0xffff) & (before & 0xffff));
                reg.setDw(r, after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Andi_w[0];
                break;
            case 2: // (An)
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_w[1];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Andi_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = mem.peekW(reg.getA().get(r) + d16);
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Andi_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekW(ptr);
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(ptr, after);
                cycle = Cycle.Andi_w[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) & (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.Andi_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) & (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.Andi_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candil(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ANDI.l ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        int val = fetchL();
//#if DEBUG
        if (TRACE) nimo += "#$%08x,".formatted(val);
//#endif


        int after = 0;
        int before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getD()[r];
                after = val & before;
                reg.getD()[r] = after;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Andi_l[0];
                break;
            case 2: // (An)
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_l[1];
                break;
            case 3: // (An)+
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Andi_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Andi_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW();
                before = mem.peekL(reg.getA().get(r) + d16);
                after = val & before;
                mem.pokeL(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Andi_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekL(ptr);
                after = val & before;
                mem.pokeL(ptr, after);
                cycle = Cycle.Andi_l[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val & before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Andi_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val & before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Andi_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cand(short n) {
        if ((n & 0xf1f8) == 0xc140 || (n & 0xf1f8) == 0xc148 || (n & 0xf1f8) == 0xc188) {
            return cexg(n);
        }
        if ((n & 0xf1c0) == 0xc1c0) {
            return cmuls(n);
        }

        int size = (n & 0x01c0) >> 6;
        return switch (size) {
            case 0 -> // byte
                    candbEADn(n);
            case 1 -> // word
                    candwEADn(n);
            case 2 -> // long
                    candlEADn(n);
            case 4 -> // byte
                    candbDnEA(n);
            case 5 -> // word
                    candwDnEA(n);
            case 6 -> // long
                    candlDnEA(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };
    }

    private int candbDnEA(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.b ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        byte val = reg.getDb(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif


        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_bDnEA[0];
                break;
            case 3: // (An)+
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.And_bDnEA[1];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                before = mem.peekB(reg.getA().get(r));
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_bDnEA[2];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekB(reg.getA().get(r) + d16);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(reg.getA().get(r) + d16, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.And_bDnEA[3];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekB(ptr);
                after = (short) ((val & 0xff) & (before & 0xffff));
                mem.pokeB(ptr, (byte) after);
                cycle = Cycle.And_bDnEA[4];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((val & 0xff) & (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.And_bDnEA[5];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((val & 0xff) & (before & 0xffff));
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.And_bDnEA[6];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candwDnEA(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.w ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        short val = reg.getDw(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif


        short after = 0;
        short before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_wDnEA[0];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.And_wDnEA[1];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                before = mem.peekW(reg.getA().get(r));
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_wDnEA[2];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // singed
                before = mem.peekW(reg.getA().get(r) + d16);
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.And_wDnEA[3];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekW(ptr);
                after = (short) ((val & 0xffff) & (before & 0xffff));
                mem.pokeW(ptr, after);
                cycle = Cycle.And_wDnEA[4];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) & (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.And_wDnEA[5];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) ((val & 0xffff) & (before & 0xffff));
                        mem.pokeW(ptr, after);
                        cycle = Cycle.And_wDnEA[6];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candlDnEA(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.l ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDl(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int after = 0;
        int before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_lDnEA[0];
                break;
            case 3: // (An)+
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.And_lDnEA[1];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                before = mem.peekL(reg.getA().get(r));
                after = val & before;
                mem.pokeL(reg.getA().get(r), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.And_lDnEA[2];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekL(reg.getA().get(r) + d16);
                after = val & before;
                mem.pokeL(reg.getA().get(r) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.And_lDnEA[3];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekL(ptr);
                after = val & before;
                mem.pokeL(ptr, after);
                cycle = Cycle.And_lDnEA[4];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val & before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.And_lDnEA[5];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val & before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.And_lDnEA[6];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candbEADn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.b ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        byte src = 0;
        byte dst = reg.getDb(sr);
        byte after = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                src = reg.getDb(r);
                after = (byte) (src & dst);
                reg.setDb(sr, after);
//#if DEBUG
                if (TRACE) nimo += "D%d,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_bEADn[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                src = mem.peekB(reg.getA().get(r));
                after = (byte) (src & dst);
                reg.setDb(sr, after);
//#if DEBUG
                if (TRACE) nimo += "(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_bEADn[1];
                break;
            case 3: // (An)+
                src = mem.peekB(reg.getA().get(r));
                after = (byte) (src & dst);
                reg.setDb(sr, after);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_bEADn[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                src = mem.peekB(reg.getA().get(r));
                after = (byte) (src & dst);
                reg.setDb(sr, after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_bEADn[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekB(reg.getA().get(r) + d16);
                after = (byte) (src & dst);
                reg.setDb(sr, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d),D%d".formatted(d16, r, sr);
//#endif

                cycle = Cycle.And_bEADn[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s),D%d".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                src = mem.peekB(ptr);
                after = (byte) (src & dst);
                reg.setDb(sr, after);
                cycle = Cycle.And_bEADn[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekB(ptr);
                        after = (byte) (src & dst);
                        reg.setDb(sr, after);
                        cycle = Cycle.And_bEADn[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekB(ptr);
                        after = (byte) (src & dst);
                        reg.setDb(sr, after);
                        cycle = Cycle.And_bEADn[7];
                        break;
                    case 2: // d16(PC)
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC),D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekB(ptr + reg.pc - 2);
                        after = (byte) (src & dst);
                        reg.setDb(sr, after);
                        cycle = Cycle.And_bEADn[8];
                        break;
                    case 3: // d8(PC,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%d.%s),D%d".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        src = mem.peekB(ptr);
                        after = (byte) (src & dst);
                        reg.setDb(sr, after);
                        cycle = Cycle.And_bEADn[9];
                        break;
                    case 4: // #Imm
                        src = (byte) fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%02x,D%d".formatted(src & 0xff, sr);
//#endif

                        after = (byte) (src & dst);
                        reg.setDb(sr, after);
                        cycle = Cycle.And_bEADn[10];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candwEADn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.w ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        short src = 0;
        short dst = reg.getDw(sr);
        short after = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                src = reg.getDw(r);
                after = (short) (src & dst);
                reg.setDw(sr, after);
//#if DEBUG
                if (TRACE) nimo += "D%d,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_wEADn[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                src = mem.peekW(reg.getA().get(r));
                after = (short) (src & dst);
                reg.setDw(sr, after);
//#if DEBUG
                if (TRACE) nimo += "(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_wEADn[1];
                break;
            case 3: // (An)+
                src = mem.peekW(reg.getA().get(r));
                after = (short) (src & dst);
                reg.setDw(sr, after);
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_wEADn[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                src = mem.peekW(reg.getA().get(r));
                after = (short) (src & dst);
                reg.setDw(sr, after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_wEADn[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekW(reg.getA().get(r) + d16);
                after = (short) (src & dst);
                reg.setDw(sr, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d),D%d".formatted(d16, r, sr);
//#endif

                cycle = Cycle.And_wEADn[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s),D%d".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                src = mem.peekW(ptr);
                after = (short) (src & dst);
                reg.setDw(sr, after);
                cycle = Cycle.And_wEADn[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekW(ptr);
                        after = (short) (src & dst);
                        reg.setDw(sr, after);
                        cycle = Cycle.And_wEADn[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekW(ptr);
                        after = (short) (src & dst);
                        reg.setDw(sr, after);
                        cycle = Cycle.And_wEADn[7];
                        break;
                    case 2: // d16(PC)
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC),D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekW(ptr + reg.pc - 2);
                        after = (short) (src & dst);
                        reg.setDw(sr, after);
                        cycle = Cycle.And_wEADn[8];
                        break;
                    case 3: // d8(PC,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s),D%d".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        src = mem.peekW(ptr);
                        after = (short) (src & dst);
                        reg.setDw(sr, after);
                        cycle = Cycle.And_wEADn[9];
                        break;
                    case 4: // #Imm
                        src = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%02x,D%d".formatted((int) src, sr);
//#endif

                        after = (short) (src & dst);
                        reg.setDw(sr, after);
                        cycle = Cycle.And_wEADn[10];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        //reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int candlEADn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "AND.l ";
//#endif

        int cycle = 0;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int src = 0;
        int dst = reg.getDl(sr);
        int after = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                src = reg.getDl(r);
                after = (src & dst);
                reg.setDl(sr, after);
//#if DEBUG
                if (TRACE) nimo += "D%d,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_lEADn[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                src = mem.peekL(reg.getA().get(r));
                after = (src & dst);
                reg.setDl(sr, after);
//#if DEBUG
                if (TRACE) nimo += "(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_lEADn[1];
                break;
            case 3: // (An)+
                src = mem.peekL(reg.getA().get(r));
                after = (src & dst);
                reg.setDl(sr, after);
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+,D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_lEADn[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                src = mem.peekL(reg.getA().get(r));
                after = (src & dst);
                reg.setDl(sr, after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d),D%d".formatted(r, sr);
//#endif

                cycle = Cycle.And_lEADn[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekL(reg.getA().get(r) + d16);
                after = (src & dst);
                reg.setDl(sr, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d),D%d".formatted(d16, r, sr);
//#endif

                cycle = Cycle.And_lEADn[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s),D%d".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                src = mem.peekL(ptr);
                after = (src & dst);
                reg.setDl(sr, after);
                cycle = Cycle.And_lEADn[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekL(ptr);
                        after = (src & dst);
                        reg.setDl(sr, after);
                        cycle = Cycle.And_lEADn[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x,D%d".formatted(ptr, sr);
//#endif

                        src = mem.peekL(ptr);
                        after = (src & dst);
                        reg.setDl(sr, after);
                        cycle = Cycle.And_lEADn[7];
                        break;
                    case 2: // d16(PC)
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC),D%d".formatted((short) ptr, sr);
//#endif

                        src = mem.peekL(ptr + reg.pc - 2);
                        after = (src & dst);
                        reg.setDl(sr, after);
                        cycle = Cycle.And_lEADn[8];
                        break;
                    case 3: // d8(PC,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%d.%s),D%d".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w", sr);
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        src = mem.peekL(ptr);
                        after = (src & dst);
                        reg.setDl(sr, after);
                        cycle = Cycle.And_lEADn[9];
                        break;
                    case 4: // #Imm
                        src = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%02x,D%d".formatted(src, sr);
//#endif

                        after = (src & dst);
                        reg.setDl(sr, after);
                        cycle = Cycle.And_lEADn[10];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        // reg.setN((after & 0x80) != 0);
        reg.setZ(after == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cexg(short n) {
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007) >> 0;
        int opm = (n & 0x00f8) >> 3;
        opm = (opm == 0b0_1000) ? 0 : (opm == 0b0_1001 ? 1 : 2);

        int src;
        int dst;

        switch (opm) {
            case 0: // EXG Dn,Dn
                src = reg.getDl(sr);
                dst = reg.getDl(dr);
                reg.setDl(sr, dst);
                reg.setDl(dr, src);
                break;
            case 1: // EXG An,An
                src = reg.getAl(sr);
                dst = reg.getAl(dr);
                reg.setAl(sr, dst);
                reg.setAl(dr, src);
                break;
            case 2: // EXG Dn,An
                src = reg.getDl(sr);
                dst = reg.getAl(dr);
                reg.setDl(sr, dst);
                reg.setAl(dr, src);
                break;
        }

        return 6;
    }

    private int cor(short n) {
        // 0x8ffc
        // if ((n & 0x0100) == 0) return Ccmp(n);
        // if ((n & 0xf138) == 0xb108)
        // return Ccmp(n);
        if ((n & 0xf1c0) == 0x81c0) return cdivs(n);
        if ((n & 0xf1c0) == 0x80c0) return cdivu(n);

        int size = (n & 0x00c0) >> 6;
        boolean isA = (n & 0x0100) == 0;
        if (!isA) {
            switch (size) {
                case 0: // byte
                    return corDnEab(n);
                case 1: // word
                    return corDnEaw(n);
                case 2: // long
                    return corDnEal(n);
            }
        } else {
            switch (size) {
                case 0: // byte
                    return corEaDnb(n);
                case 1: // word
                    return corEaDnw(n);
                case 2: // long
                    return corEaDnl(n);
            }
        }

        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int corDnEab(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "OR.b ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDb(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int after = 0;
        int before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 2: // (An)
                before = mem.peekB(reg.getA().get(dr)) & 0xff;
                after = val | before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Or_b[0];
                break;
            case 3: // (An)+
                before = mem.peekB(reg.getA().get(dr)) & 0xff;
                after = val | before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Or_b[1];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                before = mem.peekB(reg.getA().get(dr));
                after = val | before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Or_b[2];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekB(reg.getA().get(dr) + d16);
                after = val | before;
                mem.pokeB(reg.getA().get(dr) + d16, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Or_b[3];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekB(ptr);
                after = val | before;
                mem.pokeB(ptr, (byte) after);
                cycle = Cycle.Or_b[4];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekB(ptr);
                        after = val | before;
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Or_b[5];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekB(ptr);
                        after = val | before;
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Or_b[6];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ((byte) after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int corDnEaw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "OR.w ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDw(sr) & 0xffff;
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int after = 0;
        int before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 2: // (An)
                before = mem.peekW(reg.getA().get(dr)) & 0xffff;
                after = val | before;
                mem.pokeW(reg.getA().get(dr), (short) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Or_w[0];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(dr)) & 0xffff;
                after = val | before;
                mem.pokeW(reg.getA().get(dr), (short) after);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Or_w[1];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                before = mem.peekW(reg.getA().get(dr));
                after = val | before;
                mem.pokeW(reg.getA().get(dr), (short) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Or_w[2];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekW(reg.getA().get(dr) + d16);
                after = val | before;
                mem.pokeW(reg.getA().get(dr) + d16, (short) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Or_w[3];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekW(ptr);
                after = val | before;
                mem.pokeW(ptr, (short) after);
                cycle = Cycle.Or_w[4];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = val | before;
                        mem.pokeW(ptr, (short) after);
                        cycle = Cycle.Or_w[5];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = val | before;
                        mem.pokeW(ptr, (short) after);
                        cycle = Cycle.Or_w[6];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ((short) after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int corDnEal(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int corEaDnb(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("OR.b ");
//#endif

        int[] cycle = {0};
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);
        int dr = (n & 0x0e00) >> 9;

        byte val = reg.getDb(dr);

        byte after = 0;
        byte before;

        //if (sm == 7 && sr == 4) {
        //    throw new UnsupportedOperationException("OR <ea>,Dn #Imm pattern is not implemented yet");
        //}
        before = (byte) srcAddressingByte(/* ref */ nimo, /* ref */ cycle, sm, sr, 0b1111_1111_1101, true);

        cycle[0] = Cycle.OrEaDn_b[cycle[0]];
        after = (byte) (val | before);
//#if DEBUG
        if (TRACE) nimo.append(",D%d".formatted(dr));
//#endif

        reg.setDb(dr, after);

        // flag
        // reg.X
        reg.setN((after & 0x800) != 0);
        reg.setZ(after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int corEaDnw(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("OR.w ");
//#endif

        int[] cycle = {0};
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);
        int dr = (n & 0x0e00) >> 9;

        int val = reg.getDw(dr);

        int after = 0;
        int before;

        before = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0b1111_1111_1101, true);

        cycle[0] = Cycle.OrEaDn_w[cycle[0]];
        after = val | before;
//#if DEBUG
        if (TRACE) nimo.append(",D%d".formatted(dr));
//#endif

        reg.setDw(dr, (short) after);

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ((short) after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int corEaDnl(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("OR.l ");
//#endif

        int[] cycle = {0};
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);
        int dr = (n & 0x0e00) >> 9;

        int val = reg.getDl(dr);

        int after = 0;
        int before;

        before = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0b1111_1111_1101, true, 0);
        cycle[0] = Cycle.OrEaDn_l[cycle[0]];
        after = val | before;
//#if DEBUG
        if (TRACE) nimo.append(",D%d".formatted(dr));
//#endif

        reg.setDl(dr, after);

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        reg.setZ(after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int ceor(short n) {
        if ((n & 0x0100) == 0) return ccmp(n);
        if ((n & 0xf138) == 0xb108)
            return ccmp(n);

        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> // byte
                    ceorb(n);
            case 1 -> // word
                    ceorw(n);
            case 2 -> // long
                    ceorl(n);
            case 3 -> // cmpa.l
                    ccmpa_l(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };
    }

    private int ceorb(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "EOR.b ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDb(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int after = 0;
        int before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                before = reg.getDb(dr) & 0xff;
                after = val ^ before;
                reg.setDb(dr, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Eor_b[0];
                break;
            case 2: // (An)
                before = mem.peekB(reg.getA().get(dr)) & 0xff;
                after = val ^ before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_b[1];
                break;
            case 3: // (An)+
                before = mem.peekB(reg.getA().get(dr)) & 0xff;
                after = val ^ before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Eor_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                before = mem.peekB(reg.getA().get(dr)) & 0xff;
                after = val ^ before;
                mem.pokeB(reg.getA().get(dr), (byte) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekB(reg.getA().get(dr) + d16);
                after = val ^ before;
                mem.pokeB(reg.getA().get(dr) + d16, (byte) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Eor_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekB(ptr) & 0xff;
                after = val ^ before;
                mem.pokeB(ptr, (byte) after);
                cycle = Cycle.Eor_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekB(ptr) & 0xff;
                        after = val ^ before;
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Eor_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekB(ptr) & 0xff;
                        after = val ^ before;
                        mem.pokeB(ptr, (byte) after);
                        cycle = Cycle.Eor_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x80) != 0);
        reg.setZ((byte) after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ceorw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "EOR.w ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDw(sr) & 0xffff;
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif


        int after = 0;
        int before;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                before = reg.getDw(dr) & 0xffff;
                after = val ^ before;
                reg.setDw(dr, (short) after);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Eor_w[0];
                break;
            case 2: // (An)
                before = mem.peekW(reg.getA().get(dr)) & 0xffff;
                after = val ^ before;
                mem.pokeW(reg.getA().get(dr), (short) after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_w[1];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(dr)) & 0xffff;
                after = val ^ before;
                mem.pokeW(reg.getA().get(dr), (short) after);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Eor_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                before = mem.peekW(reg.getA().get(dr)) & 0xffff;
                after = val ^ before;
                mem.pokeW(reg.getA().get(dr), (short) after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekW(reg.getA().get(dr) + d16) & 0xffff;
                after = val ^ before;
                mem.pokeW(reg.getA().get(dr) + d16, (short) after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Eor_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekW(ptr) & 0xffff;
                after = val ^ before;
                mem.pokeW(ptr, (short) after);
                cycle = Cycle.Eor_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr) & 0xffff;
                        after = val ^ before;
                        mem.pokeW(ptr, (short) after);
                        cycle = Cycle.Eor_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr) & 0xffff;
                        after = val ^ before;
                        mem.pokeW(ptr, (short) after);
                        cycle = Cycle.Eor_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000) != 0);
        reg.setZ((short) after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ceorl(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "EOR.l ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        int val = reg.getDl(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif


        int after = 0;
        int before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                before = reg.getDl(dr);
                after = val ^ before;
                reg.getD()[dr] = after;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Eor_l[0];
                break;
            case 2: // (An)
                before = mem.peekL(reg.getA().get(dr));
                after = val ^ before;
                mem.pokeL(reg.getA().get(dr), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_l[1];
                break;
            case 3: // (An)+
                before = mem.peekL(reg.getA().get(dr));
                after = val ^ before;
                mem.pokeL(reg.getA().get(dr), after);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Eor_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                before = mem.peekL(reg.getA().get(dr));
                after = val ^ before;
                mem.pokeL(reg.getA().get(dr), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eor_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekL(reg.getA().get(dr) + d16);
                after = val ^ before;
                mem.pokeL(reg.getA().get(dr) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Eor_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekL(ptr);
                after = val ^ before;
                mem.pokeL(ptr, after);
                cycle = Cycle.Eor_l[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val ^ before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Eor_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = val ^ before;
                        mem.pokeL(ptr, after);
                        cycle = Cycle.Eor_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((after & 0x8000_0000) != 0);
        reg.setZ(after);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ceori(short n) {
        if ((n & 0x00c0) == 0x00c0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));

        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> // byte
                    ceorib(n);
            case 1 -> // word
                    ceoriw(n);
            case 2 -> // long
                    ceoril(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };
    }

    private int ceorib(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int ceoriw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "EORI.w ";
//#endif

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short src = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#%04x,".formatted(src);
//#endif


        short ans = 0;
        short dst;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                dst = reg.getDw(dr);
                ans = (short) (src ^ dst);
                reg.setDw(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Eori_w[0];
                break;
            case 2: // (An)
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (src ^ dst);
                mem.pokeW(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eori_w[1];
                break;
            case 3: // (An)+
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (src ^ dst);
                mem.pokeW(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Eori_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (src ^ dst);
                mem.pokeW(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Eori_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekW(reg.getA().get(dr) + d16);
                ans = (short) (src ^ dst);
                mem.pokeW(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Eori_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                dst = mem.peekW(ptr);
                ans = (short) (src ^ dst);
                mem.pokeW(ptr, ans);
                cycle = Cycle.Eori_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) (src ^ dst);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Eori_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) (src ^ dst);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Eori_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((ans & 0x8000) != 0);
        reg.setZ(ans);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ceoril(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cbtst08(short n) {
        if ((n & 0xffc0) == 0x08c0) {
            return cbset_imm(n);
        }

        if ((n & 0xffc0) == 0x0880) {
            return cbclr_imm(n);
        }

        if ((n & 0xffc0) != 0x0800) {
            throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        int cycle = 10;
        int data = fetchW() & 0xff; // byte size
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BTST";
//#endif


        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l" : ".b";
//#endif


//#if DEBUG
        if (TRACE) nimo += " #$%02x,".formatted(data);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Btst08[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Btst08[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Btst08[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Btst08[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Btst08[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr) & 0xff;
                cycle = Cycle.Btst08[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        cycle = Cycle.Btst08[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        cycle = Cycle.Btst08[7];
                        break;
                    case 2: // d16(PC)
                        d16 = fetchW(); // signed
                        dst = mem.peekB(reg.pc + d16 - 2);
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted(d16);
//#endif

                        cycle = Cycle.Btst08[8];
                        break;
                    case 3: // d8(An,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        dst = mem.peekB(ptr);
                        cycle = Cycle.Btst08[9];
                        break;
                }
                break;
        }

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;
        ans = (dst & (1 << data)) == 0;

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbsetbtst(short n) {
        if ((n & 0xf1c0) == 0x0100) {
            return cbtst(n);
        }
        if ((n & 0xf1c0) == 0x01c0) {
            return cbset(n);
        }
        if ((n & 0xf1c0) == 0x0180) {
            return cbclr_Dn(n);
        }
        throw new UnsupportedOperationException("dummy");
    }

    private int cbtst(short n) {
        int cycle;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BTST";
//#endif

        int sr = (n & 0x0e00) >> 9;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l " : ".b ";
//#endif

        int data = reg.getDl(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                // reg.getD()[r] = dst | (int)(1 << data);
                cycle = Cycle.Btst[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                //mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Btst[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                //mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
                cycle = Cycle.Btst[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                //mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Btst[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                ans = (dst & (1 << data)) == 0;
                //mem.pokeB(reg.getA().get(r) + d16, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Btst[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr) & 0xff;
                ans = (dst & (1 << data)) == 0;
                //mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Btst[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        //mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Btst[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        //mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Btst[7];
                        break;
                    case 2: // d16(PC)
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr + reg.pc - 2) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        //mem.pokeB(ptr + reg.PC - 2, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Btst[8];
                        break;
                    case 3: // d8(PC,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        //mem.pokeB(ptr + reg.PC - 2, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Btst[9];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbset(short n) {
        int cycle;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BSET";
//#endif

        int sr = (n & 0x0e00) >> 9;
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l " : ".b ";
//#endif


        int data = reg.getDl(sr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                reg.getD()[r] = dst | (1 << data);
                cycle = Cycle.Bset[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
                cycle = Cycle.Bset[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r) + d16, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr) & 0xff;
                ans = (dst & (1 << data)) == 0;
                mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Bset[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Bset[7];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbset_imm(short n) {
        int cycle;
        int data = fetchW() & 0xff;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BSET";
//#endif

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l " : ".b ";
//#endif

//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(data);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                reg.getD()[r] = dst | (1 << data);
                cycle = Cycle.Bset_i[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset_i[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
                cycle = Cycle.Bset_i[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset_i[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r) + d16, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset_i[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr);
                ans = (dst & (1 << data)) == 0;
                mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                cycle = Cycle.Bset_i[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Bset_i[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst | ((1 << data) & 0xff)));
                        cycle = Cycle.Bset_i[7];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbclr_Dn(short n) {
        int cycle;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BCLR";
//#endif

        int sr = (n & 0x0e00) >> 9;
        int data = reg.getDl(sr);
        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l " : ".b ";
//#endif


//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                reg.getD()[r] = dst & ~(1 << data);
                cycle = Cycle.Bclr_i[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r));
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r));
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
                cycle = Cycle.Bclr_i[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r) + d16, (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr) & 0xff;
                ans = (dst & (1 << data)) == 0;
                mem.pokeB(ptr, (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr);
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst & (~(1 << data) & 0xff)));
                        cycle = Cycle.Bclr_i[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst & (~(1 << data) & 0xff)));
                        cycle = Cycle.Bclr_i[7];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbclr_imm(short n) {
        int cycle;
        int data = fetchW() & 0xff;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "BCLR";
//#endif

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += m == 0 ? ".l " : ".b ";
//#endif

//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(data);
//#endif

        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short d16;

        // compute
        boolean ans;
        if (m == 0) data %= 32;
        else data %= 8;

        switch (m) {
            case 0: // Dn
                dst = reg.getD()[r];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                reg.getD()[r] = dst & ~(1 << data);
                cycle = Cycle.Bclr_i[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
                cycle = Cycle.Bclr_i[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r), (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[3];
                break;
            case 5: // d16(An)
                d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                ans = (dst & (1 << data)) == 0;
                mem.pokeB(reg.getA().get(r) + d16, (byte) (dst & (~(1 << data) & 0xff)));
                cycle = Cycle.Bclr_i[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr) & 0xff;
                ans = (dst & (1 << data)) == 0;
                mem.pokeB(ptr, (byte) (dst & (byte) ~(1 << data)));
                cycle = Cycle.Bclr_i[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst & (~(1 << data) & 0xff)));
                        cycle = Cycle.Bclr_i[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr) & 0xff;
                        ans = (dst & (1 << data)) == 0;
                        mem.pokeB(ptr, (byte) (dst & (~(1 << data) & 0xff)));
                        cycle = Cycle.Bclr_i[7];
                        break;
                    default:
                        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // reg.X
        // reg.N
        reg.setZ(ans);
        // reg.V
        // reg.C

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmpi(short n) {
        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> ccmpib(n);
            case 1 -> ccmpiw(n);
            case 2 -> ccmpil(n);
            default -> throw new UnsupportedOperationException("dummy");
        };
    }

    private int ccmpib(short n) {

//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMPI.b ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        short val = (short) (fetchW() & 0xff);
//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(val & 0xff);
//#endif

        short after = 0;
        short before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = (short) (reg.getD()[r] & 0xff);
                after = (short) ((before - val) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Cmpi_b[0];
                break;
            case 2: // (An)
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((before - val) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_b[1];
                break;
            case 3: // (An)+
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((before - val) & 0xffff);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Cmpi_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                before = (short) (mem.peekB(reg.getA().get(r)) & 0xff);
                after = (short) ((before - val) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = (short) (mem.peekB(reg.getA().get(r) + d16) & 0xff);
                after = (short) ((before - val) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Cmpi_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = (short) (mem.peekB(ptr) & 0xff);
                after = (short) ((before - val) & 0xffff);
                cycle = Cycle.Cmpi_b[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((before - val) & 0xffff);
                        cycle = Cycle.Cmpi_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = (short) (mem.peekB(ptr) & 0xff);
                        after = (short) ((before - val) & 0xffff);
                        cycle = Cycle.Cmpi_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((byte) after);
        reg.setZ((byte) after);
        // reg.SetVcmp((byte)before, (byte)val, (byte)after);
        // reg.SetCcmp((byte)before, (byte)val, (byte)after);
        reg.setVcmp((byte) val, (byte) before, (byte) after);
        reg.setCcmp((byte) val, (byte) before, (byte) after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmpiw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMPI.w ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        int val = fetchW() & 0xffff;
//#if DEBUG
        if (TRACE) nimo += "#$%04x,".formatted(val);
//#endif

        int after = 0;
        int before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getDw(r) & 0xffff;
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Cmpi_w[0];
                break;
            case 2: // (An)
                before = mem.peekW(reg.getA().get(r)) & 0xffff;
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_w[1];
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(r)) & 0xffff;
                after = before - val;
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Cmpi_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                before = mem.peekW(reg.getA().get(r)) & 0xffff;
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekW(reg.getA().get(r) + d16) & 0xffff;
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Cmpi_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekW(ptr) & 0xffff;
                after = before - val;
                cycle = Cycle.Cmpi_w[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr) & 0xffff;
                        after = before - val;
                        cycle = Cycle.Cmpi_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr) & 0xffff;
                        after = before - val;
                        cycle = Cycle.Cmpi_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((short) after);
        reg.setZ((short) after);
        // reg.SetVcmp((short)before, (short)val, (short)after);
        // reg.SetCcmp((short)before, (short)val, (short)after);
        reg.setVcmp((short) val, (short) before, (short) after);
        reg.setCcmp((short) val, (short) before, (short) after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmpil(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMPI.l ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        long val = fetchL();
//#if DEBUG
        if (TRACE) nimo += "#$%08x,".formatted(val);
//#endif

        long after = 0;
        long before = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                before = reg.getD()[r];
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                cycle = Cycle.Cmpi_l[0];
                break;
            case 2: // (An)
                before = mem.peekL(reg.getA().get(r));
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_l[1];
                break;
            case 3: // (An)+
                before = mem.peekL(reg.getA().get(r));
                after = before - val;
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Cmpi_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                before = mem.peekL(reg.getA().get(r));
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Cmpi_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekL(reg.getA().get(r) + d16);
                after = before - val;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Cmpi_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                before = mem.peekL(ptr);
                after = before - val;
                cycle = Cycle.Cmpi_l[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = before - val;
                        cycle = Cycle.Cmpi_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekL(ptr);
                        after = before - val;
                        cycle = Cycle.Cmpi_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN((int) after);
        reg.setZ((int) after);
        // reg.SetVcmp((int)before, (int)val, (int)after);
        // reg.SetCcmp((int)before, (int)val, (int)after);
        reg.setVcmp((int) val, (int) before, (int) after);
        reg.setCcmp((int) val, (int) before, (int) after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubi(short n) {
        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> csubib(n);
            case 1 -> csubiw(n);
            case 2 -> csubil(n);
            default -> throw new UnsupportedOperationException("dummy");
        };
    }

    private int csubib(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBI.b ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        byte src = (byte) (fetchW() & 0xff);
//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(src);
//#endif

        byte ans = 0;
        byte dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                dst = reg.getDb(r);
                ans = (byte) (dst - src);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                reg.setDb(r, ans);
                cycle = Cycle.Subi_b[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(r));
                ans = (byte) (dst - src);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                mem.pokeB(reg.getA().get(r), ans);
                cycle = Cycle.Subi_b[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(r));
                ans = (byte) (dst - src);
                mem.pokeB(reg.getA().get(r), ans);
                reg.getA().set(r, reg.getA().get(r) + 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Subi_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 1);
                if (r == 7) reg.getA().set(r, reg.getA().get(r) - 1);
                dst = mem.peekB(reg.getA().get(r));
                ans = (byte) (dst - src);
                mem.pokeB(reg.getA().get(r), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Subi_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(r) + d16);
                ans = (byte) (dst - src);
                mem.pokeB(reg.getA().get(r) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Subi_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekB(ptr);
                ans = (byte) (dst - src);
                mem.pokeB(ptr, ans);
                cycle = Cycle.Subi_b[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr);
                        ans = (byte) (dst - src);
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Subi_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr);
                        ans = (byte) (dst - src);
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Subi_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVcmp(src, dst, ans);
        reg.setCcmp(src, dst, ans);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubiw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBI.w ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        short src = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%04x,".formatted(src);
//#endif

        short ans = 0;
        short dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                dst = reg.getDw(r);
                ans = (short) ((dst - src) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                reg.setDw(r, ans);
                cycle = Cycle.Subi_w[0];
                break;
            case 2: // (An)
                dst = mem.peekW(reg.getA().get(r));
                ans = (short) ((dst - src) & 0xffff);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                mem.pokeW(reg.getA().get(r), ans);
                cycle = Cycle.Subi_w[1];
                break;
            case 3: // (An)+
                dst = mem.peekW(reg.getA().get(r));
                ans = (short) ((dst - src) & 0xffff);
                mem.pokeW(reg.getA().get(r), ans);
                reg.getA().set(r, reg.getA().get(r) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Subi_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 2);
                dst = mem.peekW(reg.getA().get(r));
                ans = (short) ((dst - src) & 0xffff);
                mem.pokeW(reg.getA().get(r), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Subi_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekW(reg.getA().get(r) + d16);
                ans = (short) ((dst - src) & 0xffff);
                mem.pokeW(reg.getA().get(r) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Subi_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekW(ptr);
                ans = (short) ((dst - src) & 0xffff);
                mem.pokeW(ptr, ans);
                cycle = Cycle.Subi_w[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) ((dst - src) & 0xffff);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Subi_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) ((dst - src) & 0xffff);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Subi_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVcmp(src, dst, ans);
        reg.setCcmp(src, dst, ans);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubil(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBI.l ";
//#endif

        int cycle = 0;

        int m = (n & 0x0038) >> 3;
        int r = (n & 0x0007);

        int src = fetchL();
//#if DEBUG
        if (TRACE) nimo += "#$%08x,".formatted(src);
//#endif

        int ans = 0;
        int dst = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (m) {
            case 0: // Dn
                dst = reg.getDl(r);
                ans = dst - src;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(r);
//#endif

                reg.setDl(r, ans);
                cycle = Cycle.Subi_l[0];
                break;
            case 2: // (An)
                dst = mem.peekL(reg.getA().get(r));
                ans = dst - src;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(r);
//#endif

                mem.pokeL(reg.getA().get(r), ans);
                cycle = Cycle.Subi_l[1];
                break;
            case 3: // (An)+
                dst = mem.peekL(reg.getA().get(r));
                ans = dst - src;
                mem.pokeL(reg.getA().get(r), ans);
                reg.getA().set(r, reg.getA().get(r) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(r);
//#endif

                cycle = Cycle.Subi_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(r, reg.getA().get(r) - 4);
                dst = mem.peekL(reg.getA().get(r));
                ans = dst - src;
                mem.pokeL(reg.getA().get(r), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(r);
//#endif

                cycle = Cycle.Subi_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekL(reg.getA().get(r) + d16);
                ans = dst - src;
                mem.pokeL(reg.getA().get(r) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, r);
//#endif

                cycle = Cycle.Subi_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, r, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
                dst = mem.peekL(ptr);
                ans = dst - src;
                mem.pokeL(ptr, ans);
                cycle = Cycle.Subi_l[5];
                break;
            case 7: // etc.
                switch (r) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst - src;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Subi_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst - src;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Subi_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVcmp(src, dst, ans);
        reg.setCcmp(src, dst, ans);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cmovea(short n) {
        int size = (n & 0x3000) >> 12;
        return switch (size) {
            case 2 -> // long
                    cmoveal(n);
            case 3 -> // word
                    cmoveaw(n);
            default -> throw new UnsupportedOperationException("dummy");
        };

    }

    private int cmoveaw(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVEA.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int val = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

        reg.setAw(dr, (short) val);
//#if DEBUG
        if (TRACE) nimo.append(",A%s".formatted(dr));
//#endif

        cycle[0] = Cycle.Movea_w[cycle[0]];

        // flag
        // Everything remains unchanged

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmoveal(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVEA.l ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int val = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);

        reg.getA().set(dr, val);
//#if DEBUG
        if (TRACE) nimo.append(",A%s".formatted(dr));
//#endif

        cycle[0] = Cycle.Movea_l[cycle[0]];

        // flag
        // Everything remains unchanged

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmoveFromSR(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "MOVE.w sr,";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // src
        short val = reg.getSR();

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDw(dr, val);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.MoveFromSr_w[0];
                break;
            case 2: // (An)
                mem.pokeW(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.MoveFromSr_w[1];
                break;
            case 3: // (An)+
                mem.pokeW(reg.getA().get(dr), val);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.MoveFromSr_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                mem.pokeW(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.MoveFromSr_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeW(reg.getA().get(dr) + d16, val);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.MoveFromSr_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeW(ptr, val);
                cycle = Cycle.MoveFromSr_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        mem.pokeW(ptr, val);
                        cycle = Cycle.MoveFromSr_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        mem.pokeW(ptr, val);
                        cycle = Cycle.MoveFromSr_w[7];
                        break;
                }
                break;
        }

        // flag

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cmoveToSR(short n) {
        if ((n & 0xffc0) != 0x46c0) {
            return cnot(n);
        }

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVE.w ");
//#endif

        int[] cycle = {0};

        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        short val = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",sr");
//#endif

        cycle[0] = Cycle.MoveToSr_w[cycle[0]];

        // dst
        // flag
        reg.setSR((short) ((reg.getSR() & 0b1010_0111_0001_1111) | (val & 0xffff)));

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmove(short n) {
        if ((n & 0xffc0) == 0x40c0) {
            return cmoveFromSR(n);
        }

        int size = (n & 0x3000) >> 12;

        if ((size == 2 || size == 3) && (n & 0xc1c0) == 0x40) {
            return cmovea(n);
        }

        if ((n & 0x3000) == 0x00) {
            // It looks like a command other than MOVE
            throw new UnsupportedOperationException("Not implemented!! [%04x]".formatted(n));
        }

        return switch (size) {
            case 1 -> // byte
                    cmoveb(n);
            case 2 -> // long
                    cmovel(n);
            case 3 -> // word
                    cmovew(n);
            default -> throw new UnsupportedOperationException("dummy");
        };

    }

    private int cmoveb(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVE.b ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // src
        byte val = (byte) srcAddressingByte(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDb(dr, val);
//#if DEBUG
                if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_b[cycle[0]][0];
                break;
            case 2: // (An)
                mem.pokeB(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_b[cycle[0]][1];
                break;
            case 3: // (An)+
                mem.pokeB(reg.getA().get(dr), val);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)+".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_b[cycle[0]][2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                mem.pokeB(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("-(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_b[cycle[0]][3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeB(reg.getA().get(dr) + d16, val);
//#if DEBUG
                if (TRACE) nimo.append("$%04x(A%d)".formatted(d16, dr));
//#endif

                cycle[0] = Cycle.Move_b[cycle[0]][4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeB(ptr, val);
                cycle[0] = Cycle.Move_b[cycle[0]][5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("$%04x".formatted(ptr));
//#endif

                        mem.pokeB(ptr, val);
                        cycle[0] = Cycle.Move_b[cycle[0]][6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo.append("$%08x".formatted(ptr));
//#endif

                        mem.pokeB(ptr, val);
                        cycle[0] = Cycle.Move_b[cycle[0]][7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(val);
        reg.setZ(val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmovew(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVE.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // src
        short val = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDw(dr, val);
//#if DEBUG
                if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_w[cycle[0]][0];
                break;
            case 2: // (An)
                mem.pokeW(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_w[cycle[0]][1];
                break;
            case 3: // (An)+
                mem.pokeW(reg.getA().get(dr), val);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)+".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_w[cycle[0]][2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                mem.pokeW(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("-(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_w[cycle[0]][3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeW(reg.getA().get(dr) + d16, val);
//#if DEBUG
                if (TRACE) nimo.append("$%04x(A%d)".formatted(d16, dr));
//#endif

                cycle[0] = Cycle.Move_w[cycle[0]][4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeW(ptr, val);
                cycle[0] = Cycle.Move_w[cycle[0]][5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("$%04x".formatted(ptr));
//#endif

                        mem.pokeW(ptr, val);
                        cycle[0] = Cycle.Move_w[cycle[0]][6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo.append("$%08x".formatted(ptr));
//#endif

                        mem.pokeW(ptr, val);
                        cycle[0] = Cycle.Move_w[cycle[0]][7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(val);
        reg.setZ(val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmovel(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVE.l ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // src
        int val = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        switch (dm) {
            case 0: // Dn
                reg.getD()[dr] = val;
//#if DEBUG
                if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_l[cycle[0]][0];
                break;
            case 2: // (An)
                mem.pokeL(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_l[cycle[0]][1];
                break;
            case 3: // (An)+
                mem.pokeL(reg.getA().get(dr), val);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)+".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_l[cycle[0]][2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                mem.pokeL(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo.append("-(A%d)".formatted(dr));
//#endif

                cycle[0] = Cycle.Move_l[cycle[0]][3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeL(reg.getA().get(dr) + d16, val);
//#if DEBUG
                if (TRACE) nimo.append("$%04x(A%d)".formatted(d16, dr));
//#endif

                cycle[0] = Cycle.Move_l[cycle[0]][4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeL(ptr, val);
                cycle[0] = Cycle.Move_l[cycle[0]][5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("($%04x)".formatted(ptr));
//#endif

                        mem.pokeL(ptr, val);
                        cycle[0] = Cycle.Move_l[cycle[0]][6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo.append("($%08x)".formatted(ptr));
//#endif

                        mem.pokeL(ptr, val);
                        cycle[0] = Cycle.Move_l[cycle[0]][7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(val);
        reg.setZ(val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int clea(short n) {
        int cycle = 0;

        if ((n & 0xc0) != 0xc0) {
            // Looks like a non-LEA order
            throw new UnsupportedOperationException("Not implemented!! [%04x]".formatted(n));
        }

        int a = (n & 0x0e00) >> 9;
        int mode = (n & 0x0038) >> 3;
        int r = (n & 0x0007);
        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (mode) {
            case 0:
            case 1:
            case 3:
            case 4:
                throw new UnsupportedOperationException("LEA Invalid Addressing Mode %04x".formatted(n));
            case 2: // (An)
//#if DEBUG
                if (TRACE) logger.log(Level.TRACE, "LEA (A%d),A%s".formatted(r, a));
//#endif
                reg.getA().set(a, reg.getA().get(r));
                cycle = 4;
                break;
            case 5: // d16(An)
                vw = fetchW();
//#if DEBUG
                if (TRACE) logger.log(Level.TRACE, "LEA $%04x(A%d),A%s ; d16+A%d=$%08x"
                        .formatted(vw, r, a, r, reg.getA().get(r) + vw));
//#endif
                reg.getA().set(a, reg.getA().get(r) + vw);
                cycle = 8;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
                if (!isL) ptr = reg.getA().get(r) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(r) + (byte) vw + IX;
//#if DEBUG
                if (TRACE) logger.log(Level.TRACE, "LEA $%02x(A%d,%s),A%s ; d8+A%d+IX=$%08x"
                        .formatted(vw, r, isA ? "A%d".formatted(ni) : "D%d".formatted(ni), a, r, ptr));
//#endif
                reg.getA().set(a, ptr);
                cycle = 12;
                break;
            case 7: // etc
                if (r == 0) { // Abs.W
                    ptr = fetchW();
//#if DEBUG
                    if (TRACE) logger.log(Level.TRACE, "LEA $%08x,A%s".formatted(ptr, a));
//#endif
                    reg.getA().set(a, ptr);
                    cycle = 8;
                } else if (r == 1) { // Abs.L
                    ptr = fetchL();
//#if DEBUG
                    if (TRACE) logger.log(Level.TRACE, "LEA $%08x,A%s".formatted(ptr, a));
//#endif
                    reg.getA().set(a, ptr);
                    cycle = 12;
                } else if (r == 2) { // d16(PC)
                    ptr = fetchW();
//#if DEBUG
                    // d16 is relative to the extension word, which fetchW() has already stepped past
                    if (TRACE) logger.log(Level.TRACE, "LEA $%04x(PC),A%s ; d16+PC=$%08x".formatted(ptr, a, ptr + reg.pc - 2));
//#endif
                    reg.getA().set(a, ptr + reg.pc - 2);
                    cycle = 8;
                } else if (r == 3) { // d8(PC,IX)
                    vw = fetchW();
                    isA = (vw & 0x8000) != 0;
                    ni = (vw & 0x7000) >> 12;
                    isL = (vw & 0x0800) != 0;
                    IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
                    if (!isL) ptr = reg.pc - 2 + (byte) vw + (short) (IX & 0xffff);
                    else ptr = reg.pc - 2 + (byte) vw + IX;
//#if DEBUG
                    if (TRACE) logger.log(Level.TRACE, "LEA $%02x(PC,%s.%s),A%s ; d8+PC+%s.%s=$%08x".formatted(
                            vw,
                            isA ? "A%d".formatted(ni) : "D%d".formatted(ni),
                            isL ? "l" : "w",
                            a,
                            isA ? "A%d".formatted(ni) : "D%d".formatted(ni),
                            isL ? "l" : "w",
                            ptr));
//#endif
                    reg.getA().set(a, ptr);
                    cycle = 12;
                } else
                    throw new UnsupportedOperationException("LEA unknown mode %04x".formatted(n));
                break;
        }

        return cycle;
    }

    private int cnot(short n) {
        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0x0 -> cnot_b(n);
            case 0x1 -> cnot_w(n);
            case 0x2 -> cnot_l(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };

    }

    private int cnot_b(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "NOT.b ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        byte ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                ans = reg.getDb(dr);
                ans = (byte) ~ans;
                reg.setDb(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Not_b[0];
                break;
            case 2: // (An)
                ans = mem.peekB(reg.getA().get(dr));
                ans = (byte) ~ans;
                mem.pokeB(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Not_b[1];
                break;
            case 3: // (An)+
                ans = mem.peekB(reg.getA().get(dr));
                ans = (byte) ~ans;
                mem.pokeB(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Not_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                ans = mem.peekB(reg.getA().get(dr));
                ans = (byte) ~ans;
                mem.pokeB(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Not_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                ans = mem.peekB(reg.getA().get(dr) + d16);
                ans = (byte) ~ans;
                mem.pokeB(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Not_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                ans = mem.peekB(ptr);
                ans = (byte) ~ans;
                mem.pokeB(ptr, ans);
                cycle = Cycle.Not_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        ans = mem.peekB(ptr);
                        ans = (byte) ~ans;
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Not_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        ans = mem.peekB(ptr);
                        ans = (byte) ~ans;
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Not_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(false);
        reg.setZ(true);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cnot_w(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cnot_l(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cclr(short n) {
        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0x0 -> cclr_b(n);
            case 0x1 -> cclr_w(n);
            case 0x2 -> cclr_l(n);
            default -> throw new IndexOutOfBoundsException("CLR-handled range error");
        };

    }

    private int cclr_b(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CLR.b ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDb(dr, (byte) 0);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Clr_b[0];
                break;
            case 2: // (An)
                mem.pokeB(reg.getA().get(dr), (byte) 0);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_b[1];
                break;
            case 3: // (An)+
                mem.pokeB(reg.getA().get(dr), (byte) 0);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Clr_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                mem.pokeB(reg.getA().get(dr), (byte) 0);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeB(reg.getA().get(dr) + d16, (byte) 0);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Clr_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeB(ptr, (byte) 0);
                cycle = Cycle.Clr_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        mem.pokeB(ptr, (byte) 0);
                        cycle = Cycle.Clr_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        mem.pokeB(ptr, (byte) 0);
                        cycle = Cycle.Clr_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(false);
        reg.setZ(true);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cclr_w(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CLR.w ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDw(dr, (short) 0);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Clr_w[0];
                break;
            case 2: // (An)
                mem.pokeW(reg.getA().get(dr), (short) 0);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_w[1];
                break;
            case 3: // (An)+
                mem.pokeW(reg.getA().get(dr), (short) 0);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Clr_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                mem.pokeW(reg.getA().get(dr), (short) 0);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeW(reg.getA().get(dr) + d16, (short) 0);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Clr_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeW(ptr, (short) 0);
                cycle = Cycle.Clr_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        mem.pokeW(ptr, (short) 0);
                        cycle = Cycle.Clr_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        mem.pokeW(ptr, (short) 0);
                        cycle = Cycle.Clr_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(false);
        reg.setZ(true);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cclr_l(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CLR.l ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        // dst
        switch (dm) {
            case 0: // Dn
                reg.getD()[dr] = 0;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Clr_l[0];
                break;
            case 2: // (An)
                mem.pokeL(reg.getA().get(dr), 0);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_l[1];
                break;
            case 3: // (An)+
                mem.pokeL(reg.getA().get(dr), 0);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Clr_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                mem.pokeL(reg.getA().get(dr), 0);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Clr_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeL(reg.getA().get(dr) + d16, 0);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Clr_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeL(ptr, 0);
                cycle = Cycle.Clr_l[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        mem.pokeL(ptr, 0);
                        cycle = Cycle.Clr_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        mem.pokeL(ptr, 0);
                        cycle = Cycle.Clr_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X
        reg.setN(false);
        reg.setZ(true);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cmoveccr(short n) {
        if ((n & 0xc0) != 0xc0) {
            return cneg(n);
        }

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVE.w ");
//#endif

        int[] cycle = {0};

        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        short val = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",ccr");
//#endif

        cycle[0] = Cycle.MoveToCcr_w[cycle[0]];

        reg.setCCR((byte) val);
//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cpea(short n) {
        if ((n & 0xfff8) == 0x4840) {
            return cswap(n);
        }

        if ((n & 0xfe38) == 0x4800) {
            return cext(n);
        }

        if ((n & 0xc0) != 0x40) {
            // Looks like a non-PEA command

            if ((n & 0x0b80) == 0x0880) {
                return cmovem(n);
            }

            throw new UnsupportedOperationException("Not implemented!! [%04x]".formatted(n));
        }

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("PEA.l ");
//#endif

        int[] cycle = {0};

        // src
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);
        int val = srcAddressingLongLea(/* ref */ nimo, /* ref */ cycle, sm, sr);

        // compute
        push(val);

        // flag
        // none

        // cycle
        cycle[0] = Cycle.Pea_l[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cswap(short n) {
        int r = n & 0x0007;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SWAP D%d".formatted(r);
//#endif

        // compute
        int a = reg.getDl(r);
        a = (a << 16) | (a >>> 16);
        reg.setDl(r, a);

        // flag
        // X unchanged
        reg.setN((a & 0x8000_0000) != 0);
        reg.setZ(a == 0);
        reg.setV(false);
        reg.setC(false);

        // cycle
        int cycle = 4;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cext(short n) {
        int r = n & 0x0007;
        int op = (n & 0x01c0) >> 6;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "EXT.%s D%d".formatted(op == 2 ? "w" : "l", r);
//#endif

        int a;
        // compute
        switch (op) {
            case 2:
                a = reg.getDb(r) & 0xff;
                a = (byte) a;
                reg.setDw(r, (short) a);
                break;
            case 3:
                a = reg.getDw(r) & 0xffff;
                a = (short) a;
                reg.setDl(r, a);
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        // flag
        // X unchanged
        reg.setN((a & 0x8000_0000) != 0);
        reg.setZ(a == 0);
        reg.setV(false);
        reg.setC(false);

        // cycle
        int cycle = 4;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cmovem(short n) {
        boolean r2m = ((n & 0x0400) == 0);
        if (r2m) return cmovemFromReg(n);
        else return cmovemToReg(n);
    }

    private int cmovemFromReg(short n) {
        boolean wordTrns = ((n & 0x0040) == 0);
        if (wordTrns) return cmovemFromReg_w(n);
        else return cmovemFromReg_l(n);
    }

    private int cmovemFromReg_w(short n) {
        int cycle = 0;
        int cyc = 0;

//#if DEBUG
        StringBuilder nimo = null;
        if (TRACE) nimo = new StringBuilder("MOVEM.w ");
//#endif

        // dst
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw = 0;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr = 0;

        short rl = fetchW();
//#if DEBUG
        StringBuilder dnimo = null;
        if (TRACE) dnimo = new StringBuilder();
//#endif

        boolean ff = false;
        short d16 = 0;
        int i = 15;
        int shift = 0;

        for (int rb = 0x0001; i >= 0; rb <<= 1, i--) {
            if ((rl & rb) == 0) continue;
            int val;
            if (rb > 0xff) {
                if (dm == 4) {
                    // Pre-decrement mode
                    val = reg.getD()[i % 8];
//#if DEBUG
                    if (TRACE) nimo.append("D%d".formatted(i % 8));
//#endif

                } else {
                    val = reg.getA().get(7 - (i % 8));
//#if DEBUG
                    if (TRACE) nimo.append("A%d".formatted(7 - (i % 8)));
//#endif

                }
            } else {
                if (dm == 4) {
                    // Pre-decrement mode
                    val = reg.getA().get(i % 8);
//#if DEBUG
                    if (TRACE) nimo.append("A%d".formatted(i % 8));
//#endif

                } else {
                    val = reg.getD()[7 - (i % 8)];
//#if DEBUG
                    if (TRACE) nimo.append("D%d".formatted(7 - (i % 8)));
//#endif

                }
            }

            // dst
            switch (dm) {
                case 2: // (An)
                    mem.pokeW(reg.getA().get(dr) + shift, (short) val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("(A%d)".formatted(dr));
//#endif

                    cyc = 4;
                    cycle += Cycle.MovemFromReg_w[0];
                    break;
                case 4: // -(An)
                    reg.getA().set(dr, reg.getA().get(dr) - 2);
                    mem.pokeW(reg.getA().get(dr), (short) val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("-(A%d)".formatted(dr));
//#endif

                    cyc = 4;
                    cycle += Cycle.MovemFromReg_w[1];
                    break;
                case 5: // d16(An)
                    if (!ff) {
                        d16 = fetchW(); // signed
                        ff = true;
                    }
                    mem.pokeW(reg.getA().get(dr) + d16 + shift, (short) val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("$%04x(A%d)".formatted(d16, dr));
//#endif

                    cyc = 6;
                    cycle += Cycle.MovemFromReg_w[2];
                    break;
                case 6: // d8(An,IX)
                    if (!ff) {
                        vw = fetchW();
                        ff = true;
                    }
                    isA = (vw & 0x8000) != 0;
                    ni = (vw & 0x7000) >> 12;
                    isL = (vw & 0x0800) != 0;
                    IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                    if (TRACE) dnimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                    if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                    else ptr = reg.getA().get(dr) + (byte) vw + IX;
                    mem.pokeW(ptr + shift, (short) val);
                    cyc = 6;
                    cycle += Cycle.MovemFromReg_w[3];
                    break;
                case 7: // etc.
                    switch (dr) {
                        case 0: // Abs.W
                            if (!ff) {
                                ptr = fetchW();
                                ff = true;
                            }
//#if DEBUG
                            if (TRACE) dnimo = new StringBuilder("$%04x".formatted(ptr));
//#endif

                            mem.pokeW(ptr + shift, (short) val);
                            cyc = 6;
                            cycle += Cycle.MovemFromReg_w[4];
                            break;
                        case 1: // Abs.L
                            if (!ff) {
                                ptr = fetchL();
                                ff = true;
                            }
//#if DEBUG
                            if (TRACE) dnimo = new StringBuilder("$%08x".formatted(ptr));
//#endif

                            mem.pokeW(ptr + shift, (short) val);
                            cyc = 8;
                            cycle += Cycle.MovemFromReg_w[5];
                            break;
                    }
                    break;
            }

            shift += 2;
        }

//#if DEBUG
        if (TRACE) nimo.append(" , ").append(dnimo);
//#endif

        cycle += cyc;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle;
    }

    private int cmovemFromReg_l(short n) {
        int cycle = 0;
        int cyc = 0;

//#if DEBUG
        StringBuilder nimo = null;
        if (TRACE) nimo = new StringBuilder("MOVEM ");
//#endif

        // dst
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw = 0;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr = 0;

        short rl = fetchW();
//#if DEBUG
        StringBuilder dnimo = null;
        if (TRACE) dnimo = new StringBuilder();
//#endif

        boolean ff = false;
        short d16 = 0;
        int i = 15;
        int shift = 0;

        for (int rb = 0x0001; i >= 0; rb <<= 1, i--) {
            if ((rl & rb) == 0) continue;
            int val;
            if (rb > 0xff) {
                if (dm == 4) {
                    // Pre-decrement mode
                    val = reg.getD()[i % 8];
//#if DEBUG
                    if (TRACE) nimo.append("D%d".formatted(i % 8));
//#endif

                } else {
                    val = reg.getA().get(7 - (i % 8));
//#if DEBUG
                    if (TRACE) nimo.append("A%s".formatted(7 - (i % 8)));
//#endif

                }
            } else {
                if (dm == 4) {
                    // Pre-decrement mode
                    val = reg.getA().get(i % 8);
//#if DEBUG
                    if (TRACE) nimo.append("A%s".formatted(i % 8));
//#endif
                } else {
                    val = reg.getD()[7 - (i % 8)];
//#if DEBUG
                    if (TRACE) nimo.append("D%d".formatted(7 - (i % 8)));
//#endif
                }
            }

            // dst
            switch (dm) {
                case 2: // (An)
                    mem.pokeL(reg.getA().get(dr) + shift, val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("(A%d)".formatted(dr));
//#endif

                    cyc = 4;
                    cycle += Cycle.MovemFromReg_l[0];
                    break;
                case 4: // -(An)
                    reg.getA().set(dr, reg.getA().get(dr) - 4);
                    mem.pokeL(reg.getA().get(dr), val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("-(A%d)".formatted(dr));
//#endif

                    cyc = 4;
                    cycle += Cycle.MovemFromReg_l[1];
                    break;
                case 5: // d16(An)
                    if (!ff) {
                        d16 = fetchW(); // signed
                        ff = true;
                    }
                    mem.pokeL(reg.getA().get(dr) + d16 + shift, val);
//#if DEBUG
                    if (TRACE) dnimo = new StringBuilder("$%04x(A%d)".formatted(d16, dr));
//#endif

                    cyc = 6;
                    cycle += Cycle.MovemFromReg_l[2];
                    break;
                case 6: // d8(An,IX)
                    if (!ff) {
                        vw = fetchW();
                        ff = true;
                    }
                    isA = (vw & 0x8000) != 0;
                    ni = (vw & 0x7000) >> 12;
                    isL = (vw & 0x0800) != 0;
                    IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                    if (TRACE) dnimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                    if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                    else ptr = reg.getA().get(dr) + (byte) vw + IX;
                    mem.pokeL(ptr + shift, val);
                    cyc = 6;
                    cycle += Cycle.MovemFromReg_l[3];
                    break;
                case 7: // etc.
                    switch (dr) {
                        case 0: // Abs.W
                            if (!ff) {
                                ptr = fetchW();
                                ff = true;
                            }
//#if DEBUG
                            if (TRACE) dnimo = new StringBuilder("$%04x".formatted(ptr));
//#endif

                            mem.pokeL(ptr + shift, val);
                            cyc = 6;
                            cycle += Cycle.MovemFromReg_l[4];
                            break;
                        case 1: // Abs.L
                            if (!ff) {
                                ptr = fetchL();
                                ff = true;
                            }
//#if DEBUG
                            if (TRACE) dnimo = new StringBuilder("$%08x".formatted(ptr));
//#endif

                            mem.pokeL(ptr + shift, val);
                            cyc = 8;
                            cycle += Cycle.MovemFromReg_l[5];
                            break;
                    }
                    break;
            }

            shift += 4;
        }

//#if DEBUG
        if (TRACE) nimo.append(" , ").append(dnimo);
//#endif

        cycle += cyc;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle;
    }

    private int cmovemToReg(short n) {
        boolean wordTrns = ((n & 0x0040) == 0);
        if (wordTrns) return cmovemToReg_w(n);
        else return cmovemToReg_l(n);
    }

    private int cmovemToReg_w(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cmovemToReg_l(short n) {
        int[] cycle = {0};
        int cyc = 0;

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MOVEM.l ");
//#endif

        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        short rl = fetchW();
//#if DEBUG
        StringBuilder dnimo = null;
        if (TRACE) dnimo = new StringBuilder();
//#endif

        boolean nimoSw = true;

        // int i = 0;

        // for (int rb = 0x8000; rb != 0; rb >>= 1, i++)
        int i = 15;
        int shift = 0;

        for (int rb = 0x0001; i >= 0; rb <<= 1, i--) {
            if ((rl & rb) == 0) continue;

            // src
            int val = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, nimoSw, shift);

            shift += 4;
            if (sm > 4) {
                reg.pc -= 2;
                if (sm == 7 && (sr == 1 || sr == 4)) reg.pc -= 2;
            }
//#if DEBUG
            if (TRACE) nimoSw = false;
//#endif

            if (rb > 0xff) {
                // dst
                reg.getA().set(7 - (i % 8), val);
//#if DEBUG
                if (TRACE) dnimo.append("A%s".formatted(7 - (i % 8)));
//#endif

                cyc += Cycle.MovemToReg_l1[cycle[0]];
            } else {
                // dst
                reg.getD()[7 - (i % 8)] = val;
//#if DEBUG
                if (TRACE) dnimo.append("D%d".formatted(7 - (i % 8)));
//#endif

                cyc += Cycle.MovemToReg_l1[cycle[0]];
            }
        }

        if (shift != 0) {
            if (sm > 4) {
                reg.pc += 2;
                if (sm == 7 && (sr == 1 || sr == 4)) reg.pc += 2;
            }
        }

//#if DEBUG
        if (TRACE) nimo.append(",").append(dnimo);
//#endif

        cycle[0] = Cycle.MovemToReg_l0[cycle[0]] + cyc;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cneg(short n) {
        int size = (n & 0x00c0) >> 6;

        return switch (size) {
            case 0 -> cnegb(n); // byte
            case 1 -> cnegw(n); // word
            case 2 -> cnegl(n); // long
            default -> throw new UnsupportedOperationException("dummy");
        };
    }

    private int cnegb(short n) {
//#if DEBUG
        StringBuilder nimo = null;
        if (TRACE) nimo = new StringBuilder("NEG.b ");
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        byte val = 0;
        byte ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getDb(dr);
//#if DEBUG
                if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

                ans = (byte) (-val & 0xff);
                reg.setDb(dr, ans);
                cycle = Cycle.Neg_b[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                val = mem.peekB(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo.append("(A%d)".formatted(dr));
//#endif

                ans = (byte) (-val & 0xff);
                mem.pokeB(reg.getA().get(dr), ans);
                cycle = Cycle.Neg_b[1];
                break;
            case 3: // (An)+
                val = mem.peekB(reg.getA().get(dr));
                ans = (byte) (-val & 0xff);
                mem.pokeB(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo.append("(A%d)+".formatted(dr));
//#endif

                cycle = Cycle.Neg_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                val = mem.peekB(reg.getA().get(dr));
                ans = (byte) (-val & 0xff);
                mem.pokeB(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo.append("-(A%d)".formatted(dr));
//#endif

                cycle = Cycle.Neg_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekB(reg.getA().get(dr) + d16);
                ans = (byte) -(byte) val;
                mem.pokeB(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo.append("$%04x(A%d)".formatted(d16, dr));
//#endif

                cycle = Cycle.Neg_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekB(ptr);
                ans = (byte) -(byte) val;
                mem.pokeB(ptr, ans);
                cycle = Cycle.Neg_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("($%04x)".formatted(ptr));
//#endif

                        val = mem.peekB(ptr);
                        ans = (byte) -(byte) val;
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Neg_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo.append("($%08x)".formatted(ptr));
//#endif

                        val = mem.peekB(ptr);
                        ans = (byte) -(byte) val;
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Neg_b[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVneg(val, ans);
        reg.setCneg(val, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle;
    }

    private int cnegw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "NEG.w ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short val = 0;
        short ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getDw(dr);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                ans = (short) (-val & 0xffff);
                reg.setDw(dr, ans);
                cycle = Cycle.Neg_w[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                val = mem.peekW(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                ans = (short) (-val & 0xffff);
                mem.pokeW(reg.getA().get(dr), ans);
                cycle = Cycle.Neg_w[1];
                break;
            case 3: // (An)+
                val = mem.peekW(reg.getA().get(dr));
                ans = (short) (-val & 0xffff);
                mem.pokeW(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Neg_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                val = mem.peekW(reg.getA().get(dr));
                ans = (short) (-val & 0xffff);
                mem.pokeW(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Neg_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekW(reg.getA().get(dr) + d16);
                ans = (short) (-val & 0xffff);
                mem.pokeW(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Neg_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekW(ptr);
                ans = (short) (-val & 0xffff);
                mem.pokeW(ptr, ans);
                cycle = Cycle.Neg_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekW(ptr);
                        ans = (short) (-val & 0xffff);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Neg_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekW(ptr);
                        ans = (short) (-val & 0xffff);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Neg_w[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVneg(val, ans);
        reg.setCneg(val, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cnegl(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "NEG.l ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getD()[dr];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                reg.setDl(dr, -val);
                cycle = Cycle.Neg_l[0];
                break;
            case 1:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                val = mem.peekL(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                mem.pokeL(reg.getA().get(dr), -val);
                cycle = Cycle.Neg_l[1];
                break;
            case 3: // (An)+
                val = mem.peekL(reg.getA().get(dr));
                mem.pokeL(reg.getA().get(dr), -val);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Neg_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                val = mem.peekL(reg.getA().get(dr));
                mem.pokeL(reg.getA().get(dr), -val);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Neg_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekL(reg.getA().get(dr) + d16);
                mem.pokeL(reg.getA().get(dr) + d16, -val);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Neg_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekL(ptr);
                mem.pokeL(ptr, -val);
                cycle = Cycle.Neg_l[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekL(ptr);
                        mem.pokeL(ptr, -val);
                        cycle = Cycle.Neg_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekL(ptr);
                        mem.pokeL(ptr, -val);
                        cycle = Cycle.Neg_l[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(-val);
        reg.setZ(-val);
        reg.setVneg(val, -val);
        reg.setCneg(val, -val);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ctst(short n) {
        int size = (n & 0x00c0) >> 6;

        switch (size) {
            case 0: // byte
                return ctstb(n);
            case 1: // word
                return ctstw(n);
            case 2: // long
                return ctstl(n);
        }

        if ((n & 0x00c0) == 0x00c0) {
            return ctas(n);
        }

        throw new UnsupportedOperationException("dummy");
    }

    private int ctstb(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "TST.b ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getD()[dr];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Tst_b[0];
                break;
            case 2: // (An)
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_b[1];
                break;
            case 3: // (An)+
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Tst_b[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekB(reg.getA().get(dr) + d16) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Tst_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekB(ptr) & 0xff;
                cycle = Cycle.Tst_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekB(ptr) & 0xff;
                        cycle = Cycle.Tst_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekB(ptr) & 0xff;
                        cycle = Cycle.Tst_b[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X -
        reg.setN((byte) val);
        reg.setZ((byte) val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ctstw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "TST.w ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getD()[dr];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Tst_w[0];
                break;
            case 2: // (An)
                val = mem.peekW(reg.getA().get(dr)) & 0xffff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_w[1];
                break;
            case 3: // (An)+
                val = mem.peekW(reg.getA().get(dr)) & 0xffff;
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Tst_w[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                val = mem.peekW(reg.getA().get(dr)) & 0xffff;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekW(reg.getA().get(dr) + d16) & 0xffff;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Tst_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekW(ptr) & 0xffff;
                cycle = Cycle.Tst_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekW(ptr) & 0xffff;
                        cycle = Cycle.Tst_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekW(ptr) & 0xffff;
                        cycle = Cycle.Tst_w[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X -
        reg.setN((short) val);
        reg.setZ((short) val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ctstl(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "TST.l ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getD()[dr];
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Tst_l[0];
                break;
            case 2: // (An)
                val = mem.peekL(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_l[1];
                break;
            case 3: // (An)+
                val = mem.peekL(reg.getA().get(dr));
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Tst_l[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                val = mem.peekL(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tst_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekL(reg.getA().get(dr) + d16);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Tst_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekL(ptr);
                cycle = Cycle.Tst_l[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekL(ptr);
                        cycle = Cycle.Tst_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekL(ptr);
                        cycle = Cycle.Tst_l[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X -
        reg.setN(val);
        reg.setZ(val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ctas(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "TAS.b ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                val = reg.getDb(dr) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Tas[0];
                reg.setDb(dr, (byte) (val | 0x80));
                break;
            case 1:
                throw new IndexOutOfBoundsException();
            case 2: // (An)
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tas[1];
                mem.pokeB(reg.getA().get(dr), (byte) (val | 0x80));
                break;
            case 3: // (An)+
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
                mem.pokeB(reg.getA().get(dr), (byte) (val | 0x80));
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Tas[2];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                val = mem.peekB(reg.getA().get(dr)) & 0xff;
                mem.pokeB(reg.getA().get(dr), (byte) (val | 0x80));
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Tas[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                val = mem.peekB(reg.getA().get(dr) + d16) & 0xff;
                mem.pokeB(reg.getA().get(dr) + d16, (byte) (val | 0x80));
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Tas[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = mem.peekB(ptr) & 0xff;
                mem.pokeB(ptr, (byte) (val | 0x80));
                cycle = Cycle.Tas[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = mem.peekB(ptr) & 0xff;
                        mem.pokeB(ptr, (byte) (val | 0x80));
                        cycle = Cycle.Tas[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = mem.peekB(ptr) & 0xff;
                        mem.pokeB(ptr, (byte) (val | 0x80));
                        cycle = Cycle.Tas[7];
                        break;
                }
                break;
        }

        // flag
        // reg.X -
        reg.setN((byte) val);
        reg.setZ((byte) val);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int crts(short n) {
        if ((n & 0xfff0) == 0x4e40) {
            return ctrap(n);
        }

        if ((n & 0xffc0) == 0x4e80) {
            return cjsr(n);
        }

        if ((n & 0xffc0) == 0x4ec0) {
            return cjmp(n);
        }

        if ((n & 0xfff8) == 0x4e50) {
            return clink(n);
        }

        if ((n & 0xfff8) == 0x4e58) {
            return cunlk(n);
        }

        if (n == 0x4e71) {
            return cnop(n);
        }

        if (n == 0x4e73) {
            return crte(n);
        }

        if (n != 0x4e75) {
            throw new UnsupportedOperationException("Not implemented!");
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "RTS");
//#endif
        reg.pc = pop();
        return 16;
    }

    private int crte(short n) {
        logger.log(Level.TRACE, "RTE");
        reg.setSR(popw());
        reg.pc = pop();
        return 20;
    }

    private int ctrap(short n) {
        int t = n & 0xf;
//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "TRAP #%02x".formatted(t));
//#endif
        t += 32; // vector32～
        t *= 4; // 4byte
        reg.setSRbk(reg.getSR());
        reg.setS(true);
        reg.setT(false);
        pushSSPw((short) reg.pc);
        pushSSPw((short) (reg.pc >>> 16));
        pushSSPw(reg.getSRbk());
        reg.pc = mem.peekL(t);

        return 34; // cycle
    }

    public int ctrap2(short n) {
        int t = n & 0xff;
//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "TRAP #%02x".formatted(t));
//#endif
        t *= 4; // 4byte
        reg.setSRbk(reg.getSR());
        reg.setS(true);
        reg.setT(false);
        pushSSPw((short) reg.pc);
        pushSSPw((short) (reg.pc >>> 16));
        pushSSPw(reg.getSRbk());
        reg.pc = mem.peekL(t);

        return 34; // cycle
    }

    public int ctrapPtr(int n) {
//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "TRAP $%08x".formatted(n));
//#endif
        reg.setSRbk(reg.getSR());
        reg.setS(true);
        reg.setT(false);
        pushSSPw((short) reg.pc);
        pushSSPw((short) (reg.pc >>> 16));
        pushSSPw(reg.getSRbk());
        reg.pc = n;

        return 34; // cycle
    }

    private static int cnop(short n) {
//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "NOP");
//#endif
        return 4; // cycle
    }

    private int caqsqdbs(short n) {
        // 0b0101_xxxx_1100_1xxx -> DBcc
        // 0b0101_xxxx_11xx_xxxx -> Scc
        // 0b0101_xxx1_xxxx_xxxx -> SUBQ
        // 0b0101_xxx0_xxxx_xxxx -> ADDQ
        if ((n & 0xf0f8) == 0x50c8) return cdbcc(n);
        if ((n & 0xf0c0) == 0x50c0) return cscc(n);
        if ((n & 0xf100) == 0x5100) return csubq(n);
        if ((n & 0xf100) == 0x5000) return caddq(n);

        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cdbcc(short n) {
        int cnd = (n & 0x0f00) >> 8;
        int dr = (n & 0x7);
        int cycle = 10;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "DB%s.w D%d,#$%04x";
//#endif

        String[] cs = new String[1];
        boolean v = getCond(cnd, /* out */ cs);
        short ptr = fetchW();
//#if DEBUG
        if (TRACE) nimo = nimo.formatted(cs[0].equals("f") ? "ra" : cs[0], dr, ptr);
//#endif

        if (!v) {
            short d = reg.getDw(dr);
            d--;
            reg.setDw(dr, d);
            if (d != (short) 0xffff) {
                reg.pc = reg.pc + (ptr - 2);
            } else {
                cycle = 14;
            }
        } else {
            cycle = 12;
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cscc(short n) {
        int cycle;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "S%s.b ";
//#endif

        int cond = (n & 0x0f00) >> 8;
        boolean b = false;
        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        byte val = 0;

        String[] cs = new String[1];
        b = getCond(cond, /* out */ cs);
//#if DEBUG
        if (TRACE) nimo = nimo.formatted(cs[0]);
//#endif

        cycle = Cycle.Scc_b[dm];
        if (!b) {
            if (dm == 0) cycle = 4;
        } else
            val = (byte) 0xff;

        // dst
        switch (dm) {
            case 0: // Dn
                reg.setDb(dr, val);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                break;
            case 2: // (An)
                mem.pokeB(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                break;
            case 3: // (An)+
                mem.pokeB(reg.getA().get(dr), val);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                mem.pokeB(reg.getA().get(dr), val);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                mem.pokeB(reg.getA().get(dr) + d16, val);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                mem.pokeB(ptr, val);
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        mem.pokeB(ptr, val);
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        mem.pokeB(ptr, val);
                        break;
                }
                break;
        }

        // flag
        // no change

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddq(short n) {
        int data = (n & 0x0e00) >> 9;
        int size = (n & 0x00c0) >> 6;

        if (data == 0) data = 8;

        return switch (size) {
            case 0 -> // byte
                    caddqb(n, data);
            case 1 -> // word
                    caddqw(n, data);
            case 2 -> // long
                    caddql(n, data);
            default -> throw new UnsupportedOperationException("dummy");
        };

    }

    private int caddqb(short n, int data) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDQ.b #%x,".formatted(data);
//#endif

        int cycle = 0;

        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0; // signed
        int dst = data; // signed
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDb(dr);
                ans = src + dst;
                reg.setDb(dr, (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Addq_b[0];
                break;
            case 1: // An
                throw new IndexOutOfBoundsException();
            case 2: // (An)
                src = mem.peekB(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_b[2];
                break;
            case 3: // (An)+
                src = mem.peekB(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Addq_b[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                src = mem.peekB(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_b[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekB(reg.getA().get(dr) + d16);
                ans = src + dst;
                mem.pokeB(reg.getA().get(dr) + d16, (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addq_b[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekB(ptr);
                ans = src + dst;
                mem.pokeB(ptr, (byte) ans);
                cycle = Cycle.Addq_b[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekB(ptr);
                        ans = src + dst;
                        mem.pokeB(ptr, (byte) ans);
                        cycle = Cycle.Addq_b[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekB(ptr);
                        ans = src + dst;
                        mem.pokeB(ptr, (byte) ans);
                        cycle = Cycle.Addq_b[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) { // In the case of An, it does not affect CCR!!
            reg.setN((byte) ans);
            reg.setZ((byte) ans);
            reg.setV((byte) src, (byte) ans);
            reg.setC((short) src, (short) ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddqw(short n, int data) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDQ.w #%x,".formatted(data);
//#endif

        int cycle = 0;

        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0; // signed
        int dst = data;
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDw(dr);
                ans = src + dst;
                reg.setDw(dr, (short) ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Addq_w[0];
                break;
            case 1: // An
                src = reg.getAl(dr); // An is 32-bit operation
                ans = src + dst;
                reg.setAl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "A%s".formatted(dr);
//#endif

                cycle = Cycle.Addq_w[1];
                break;
            case 2: // (An)
                src = mem.peekW(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_w[2];
                break;
            case 3: // (An)+
                src = mem.peekW(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Addq_w[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                src = mem.peekW(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_w[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekW(reg.getA().get(dr) + d16);
                ans = src + dst;
                mem.pokeW(reg.getA().get(dr) + d16, (short) ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addq_w[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekW(ptr);
                ans = src + dst;
                mem.pokeW(ptr, (short) ans);
                cycle = Cycle.Addq_w[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekW(ptr);
                        ans = src + dst;
                        mem.pokeW(ptr, (short) ans);
                        cycle = Cycle.Addq_w[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekW(ptr);
                        ans = src + dst;
                        mem.pokeW(ptr, (short) ans);
                        cycle = Cycle.Addq_w[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) { // In the case of An, it does not affect CCR!!
            reg.setN((short) ans);
            reg.setZ((short) ans);
            reg.setV((short) src, (short) ans);
            reg.setC((short) src, (short) ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddql(short n, int data) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDQ.l #%x,".formatted(data);
//#endif

        int cycle = 0;

        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0;
        int dst = data;
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDl(dr);
                ans = src + dst;
                reg.setDl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Addq_l[0];
                break;
            case 1: // An
                src = reg.getAl(dr); // An is 32-bit operation
                ans = src + dst;
                reg.setAl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "A%s".formatted(dr);
//#endif

                cycle = Cycle.Addq_l[1];
                break;
            case 2: // (An)
                src = mem.peekL(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeL(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_l[2];
                break;
            case 3: // (An)+
                src = mem.peekL(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeL(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Addq_l[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                src = mem.peekL(reg.getA().get(dr));
                ans = src + dst;
                mem.pokeL(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addq_l[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekL(reg.getA().get(dr) + d16);
                ans = src + dst;
                mem.pokeL(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addq_l[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekL(ptr);
                ans = src + dst;
                mem.pokeL(ptr, ans);
                cycle = Cycle.Addq_l[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekL(ptr);
                        ans = src + dst;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Addq_l[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekL(ptr);
                        ans = src + dst;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Addq_l[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) // In the case of An, it does not affect CCR!!
        {
            reg.setN(ans);
            reg.setZ(ans);
            reg.setV(src, ans);
            reg.setC(src, ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubq(short n) {

        int size = (n & 0x00c0) >> 6;
        return switch (size) {
            case 0 -> csubqb(n);
            case 1 -> csubqw(n);
            case 2 -> csubql(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };

    }

    private int csubqb(short n) {
        int imm = (n & 0x0e00) >> 9;
        if (imm == 0) imm = 8;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBQ.b #%x,".formatted(imm);
//#endif

        int cycle = 0;
        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0; // signed
        int dst = imm; // signed
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDb(dr);
                ans = src - dst;
                reg.setDb(dr, (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Subq_b[0];
                break;
            case 1: // An
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                src = mem.peekB(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_b[2];
                break;
            case 3: // (An)+
                src = mem.peekB(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Subq_b[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                src = mem.peekB(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeB(reg.getA().get(dr), (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_b[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekB(reg.getA().get(dr) + d16);
                ans = src - dst;
                mem.pokeB(reg.getA().get(dr) + d16, (byte) ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Subq_b[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekB(ptr);
                ans = src - dst;
                mem.pokeB(ptr, (byte) ans);
                cycle = Cycle.Subq_b[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekB(ptr);
                        ans = src - dst;
                        mem.pokeB(ptr, (byte) ans);
                        cycle = Cycle.Subq_b[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekB(ptr);
                        ans = src - dst;
                        mem.pokeB(ptr, (byte) ans);
                        cycle = Cycle.Subq_b[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) { // In the case of An, it does not affect CCR!!
            reg.setN((byte) ans);
            reg.setZ((byte) ans);
            reg.setV((byte) src, (byte) ans);
            reg.setCcmp((byte) src, (byte) dst, (byte) ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubqw(short n) {
        int imm = (n & 0x0e00) >> 9;
        if (imm == 0) imm = 8;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBQ.w #%x,".formatted(imm);
//#endif

        int cycle = 0;
        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0; // signed
        int dst = imm; // signed
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDw(dr);
                ans = src - dst;
                reg.setDw(dr, (short) ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Subq_w[0];
                break;
            case 1: // An
                src = reg.getAl(dr); // In the case of An, 32-bit calculations are performed!!
                ans = src - dst;
                reg.setAl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "A%s".formatted(dr);
//#endif

                cycle = Cycle.Subq_w[1];
                break;
            case 2: // (An)
                src = mem.peekW(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_w[2];
                break;
            case 3: // (An)+
                src = mem.peekW(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Subq_w[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                src = mem.peekW(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeW(reg.getA().get(dr), (short) ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_w[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekW(reg.getA().get(dr) + d16);
                ans = src - dst;
                mem.pokeW(reg.getA().get(dr) + d16, (short) ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Subq_w[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekW(ptr);
                ans = src - dst;
                mem.pokeW(ptr, (short) ans);
                cycle = Cycle.Subq_w[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekW(ptr);
                        ans = src - dst;
                        mem.pokeW(ptr, (short) ans);
                        cycle = Cycle.Subq_w[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekW(ptr);
                        ans = src - dst;
                        mem.pokeW(ptr, (short) ans);
                        cycle = Cycle.Subq_w[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) { // In the case of An, it does not affect CCR!!
            reg.setN((short) ans);
            reg.setZ((short) ans);
            reg.setV((short) src, (short) ans);
            reg.setCcmp((short) src, (short) dst, (short) ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubql(short n) {
        int imm = (n & 0x0e00) >> 9;
        if (imm == 0) imm = 8;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUBQ.l #%x,".formatted(imm);
//#endif

        int cycle = 0;
        int dr = (n & 0x0007);
        int dm = (n & 0x0038) >> 3;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        int src = 0;
        int dst = imm;
        int ans = 0;

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDl(dr);
                ans = src - dst;
                reg.setDl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Subq_l[0];
                break;
            case 1: // An
                src = reg.getAl(dr);
                ans = src - dst;
                reg.setAl(dr, ans);
//#if DEBUG
                if (TRACE) nimo += "A%s".formatted(dr);
//#endif

                cycle = Cycle.Subq_l[1];
                break;
            case 2: // (An)
                src = mem.peekL(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeL(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_l[2];
                break;
            case 3: // (An)+
                src = mem.peekL(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeL(reg.getA().get(dr), ans);
                reg.getA().set(dr, reg.getA().get(dr) + 4);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Subq_l[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                src = mem.peekL(reg.getA().get(dr));
                ans = src - dst;
                mem.pokeL(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Subq_l[4];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                src = mem.peekL(reg.getA().get(dr) + d16);
                ans = src - dst;
                mem.pokeL(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Subq_l[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekL(ptr);
                ans = src - dst;
                mem.pokeL(ptr, ans);
                cycle = Cycle.Subq_l[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        src = mem.peekL(ptr);
                        ans = src - dst;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Subq_l[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        src = mem.peekL(ptr);
                        ans = src - dst;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Subq_l[8];
                        break;
                }
                break;
        }

        // flag
        if (dm != 1) { // In the case of An, it does not affect CCR!!
            reg.setN(ans);
            reg.setZ(ans);
            reg.setV(src, ans);
            reg.setCcmp(src, dst, ans);
            reg.setX(reg.getC());
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cbsr(short n) {
        int cycle = 20;
        short ptr = (byte) n;
        int size = 0;
        if (ptr == 0) {
            ptr = fetchW();
            size = 2;
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "BSR $%04x ; ptr+PC=$%08x".formatted(ptr, reg.pc + ptr - size));
//#endif

        push(reg.pc);
        reg.pc += ptr - size;

        return cycle;
    }

    private int cjsr(short n) {
        int cycle = 0;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "JSR ";
//#endif

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 2: // (An)
                val = reg.getA().get(dr);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Jsr_l[0];
                break;
            case 5: // d16(An)
                int d16 = fetchW(); // signed
                val = reg.getA().get(dr) + d16;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted((short) d16, dr);
//#endif

                cycle = Cycle.Jsr_l[1];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = ptr;
                cycle = Cycle.Jsr_l[2];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = ptr;
                        cycle = Cycle.Jsr_l[3];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = ptr;
                        cycle = Cycle.Jsr_l[4];
                        break;
                    case 2:
                        int ed16 = fetchW(); // signed
                        val = reg.pc + ed16 - 2;
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted((short) ed16);
//#endif

                        cycle = Cycle.Jsr_l[5];
                        break;
                    case 3:
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        val = ptr;
                        cycle = Cycle.Jsr_l[6];
                        break;
                }
                break;
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif


        push(reg.pc);
        reg.pc = val;

        return cycle;
    }

    private int cjmp(short n) {
        int cycle = 0;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "JMP ";
//#endif


        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        // dst
        switch (dm) {
            case 0:
            case 1:
            case 3:
            case 4:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                val = reg.getA().get(dr);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Jmp[0];
                break;
            case 5: // d16(An)
                int d16 = fetchW(); // signed
                val = reg.getA().get(dr) + d16;
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted((short) d16, dr);
//#endif

                cycle = Cycle.Jmp[1];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                val = ptr;
                cycle = Cycle.Jmp[2];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        val = ptr;
                        cycle = Cycle.Jmp[3];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        val = ptr;
                        cycle = Cycle.Jmp[4];
                        break;
                    case 2:
                        int ed16 = fetchW(); // signed
                        val = reg.pc + ed16 - 2;
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted(ed16 & 0xffff);
//#endif

                        cycle = Cycle.Jmp[5];
                        break;
                    case 3:
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        val = ptr;
                        cycle = Cycle.Jmp[6];
                        break;
                }
                break;
        }

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        reg.pc = val;

        return cycle;
    }

    private int cbra(short n) {
        int cycle = 10;
        int cnd = (n & 0x0f00) >> 8;
        short ptr = (byte) n;
        int size = 0;
        if ((byte) n == 0) {
            ptr = fetchW(); // signed
            cycle = 8;
            size = 2;
        }

        String[] cs = new String[1];
        boolean v = getCond(cnd, /* out */ cs);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "B%s $%04x ; ptr+PC=$%08x".formatted(
                cs[0].equals("t") ? "ra" : cs[0], ptr, reg.pc + ptr - size));
//#endif
        if (v) reg.pc = reg.pc + ptr - size;

        return cycle;
    }

    private int cmoveq(short n) {
        int dr = (n & 0x0e00) >> 9;
        byte val = (byte) n;

        reg.getD()[dr] = val;

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, "MOVEQ.l #$%02x,D%d".formatted(val, dr));
//#endif

        reg.setN((reg.getD()[dr] & 0x8000_000) != 0);
        reg.setZ(reg.getD()[dr] == 0);
        reg.setV(false);
        reg.setC(false);

        return 4;
    }

    private int csub(short n) {
        int opMode = (n & 0x01c0) >> 6;

        // It looks like a command other than SUBA
        return switch (opMode) {
            case 0x00 -> csubb(n);
            case 0x01 -> csubw(n);
            case 0x02 -> csubl(n);
            case 0x03 -> csubaw(n);
            case 0x04 -> csubbDn(n);
            case 0x05 -> csubwDn(n);
            case 0x06 -> csublDn(n);
            case 0x07 -> csubal(n);
            default -> throw new UnsupportedOperationException("Not implemented!! [%04x]".formatted(n));
        };
    }

    private int csubbDn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUB.b ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = reg.getDb(dr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        int dst = 0;
        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        cycle = sm;
        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dst = (short) mem.peekB(reg.getA().get(sr));
                ans = (int) (byte) dst - (int) (byte) src;
                mem.pokeB(reg.getA().get(sr), (byte) ans);
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dst = ((short) mem.peekB(reg.getA().get(sr))) & 0xffff;
                ans = (byte) dst - (byte) src;
                mem.pokeB(reg.getA().get(sr), (byte) ans);
                reg.getA().set(sr, reg.getA().get(sr) + 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) + 1);
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) - 1);
                dst = ((short) mem.peekB(reg.getA().get(sr)) & 0xffff);
                ans = (byte) dst - (byte) src;
                mem.pokeB(reg.getA().get(sr), (byte) ans);
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dst = ((short) mem.peekB(reg.getA().get(sr) + d16)) & 0xffff;
                ans = (byte) dst - (byte) src;
                mem.pokeB(reg.getA().get(sr) + d16, (byte) ans);
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dst = ((short) mem.peekB(ptr)) & 0xffff;
                ans = (byte) dst - (byte) src;
                mem.pokeB(ptr, (byte) ans);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = ((short) mem.peekB(ptr)) & 0xffff;
                        ans = (byte) dst - (byte) src;
                        mem.pokeB(ptr, (byte) ans);
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = ((short) mem.peekB(ptr)) & 0xffff;
                        ans = (byte) dst - (byte) src;
                        mem.pokeB(ptr, (byte) ans);
                        cycle = 8;
                        break;
                }
                break;
        }

        // flag
        reg.setN((byte) ans);
        reg.setZ((byte) ans);
        reg.setVcmp((byte) src, (byte) dst, (byte) ans);
        reg.setCcmp((byte) src, (byte) dst, (byte) ans);
        reg.setX(reg.getC());

        // cycle
        cycle = Cycle.Sub_bDn[cycle];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubwDn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUB.w ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = reg.getDw(dr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        int dst = 0;
        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        cycle = sm;
        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dst = mem.peekW(reg.getA().get(sr));
                ans = (short) dst - (short) src;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dst = mem.peekW(reg.getA().get(sr));
                ans = (short) dst - (short) src;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                reg.getA().set(sr, reg.getA().get(sr) + 2);
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 2);
                dst = mem.peekW(reg.getA().get(sr));
                ans = (short) dst - (short) src;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dst = mem.peekW(reg.getA().get(sr) + d16);
                ans = (short) dst - (short) src;
                mem.pokeW(reg.getA().get(sr) + d16, (short) ans);
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dst = mem.peekW(ptr);
                ans = (short) dst - (short) src;
                mem.pokeW(ptr, (short) ans);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) dst - (short) src;
                        mem.pokeW(ptr, (short) ans);
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) dst - (short) src;
                        mem.pokeW(ptr, (short) ans);
                        cycle = 8;
                        break;
                }
                break;
        }

        // flag
        reg.setN((short) ans);
        reg.setZ((short) ans);
        reg.setVcmp((short) src, (short) dst, (short) ans);
        reg.setCcmp((short) src, (short) dst, (short) ans);
        reg.setX(reg.getC());

        // cycle
        cycle = Cycle.Sub_wDn[cycle];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csublDn(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "SUB.l ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = reg.getDl(dr);
//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        int dst = 0;
        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        cycle = sm;
        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dst = mem.peekL(reg.getA().get(sr));
                ans = dst - src;
                mem.pokeL(reg.getA().get(sr), ans);
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dst = mem.peekL(reg.getA().get(sr));
                ans = dst - src;
                mem.pokeL(reg.getA().get(sr), ans);
                reg.getA().set(sr, reg.getA().get(sr) + 4);
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 4);
                dst = mem.peekL(reg.getA().get(sr));
                ans = dst - src;
                mem.pokeL(reg.getA().get(sr), ans);
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dst = mem.peekL(reg.getA().get(sr) + d16);
                ans = dst - src;
                mem.pokeL(reg.getA().get(sr) + d16, ans);
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dst = mem.peekL(ptr);
                ans = dst - src;
                mem.pokeL(ptr, ans);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst - src;
                        mem.pokeL(ptr, ans);
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst - src;
                        mem.pokeL(ptr, ans);
                        cycle = 8;
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVcmp(src, dst, ans);
        reg.setCcmp(src, dst, ans);
        reg.setX(reg.getC());

        // cycle
        cycle = Cycle.Sub_lDn[cycle];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int csubb(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("SUB.b ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = srcAddressingByte(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        short dst = (short) (reg.getDb(dr) & 0xff);
//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        // compute
        short ans = (short) ((dst - (short) src) & 0xffff);
        reg.setDb(dr, (byte) ans);

        // flag
        reg.setN((byte) ans);
        reg.setZ((byte) ans);
        reg.setVcmp((byte) src, (byte) dst, (byte) ans);
        reg.setCcmp((byte) src, (byte) dst, (byte) ans);
        reg.setX(reg.getC());

        // cycle
        cycle[0] = Cycle.Sub_b[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int csubw(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("SUB.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dst = reg.getDw(dr) & 0xffff;
//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        // compute
        int ans = dst - src;
        reg.setDw(dr, (short) ans);

        // flag
        reg.setN((short) ans);
        reg.setZ((short) ans);
        reg.setVcmp((short) src, (short) dst, (short) ans);
        reg.setCcmp((short) src, (short) dst, (short) ans);
        reg.setX(reg.getC());

        // cycle
        cycle[0] = Cycle.Sub_w[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int csubl(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("SUB.l ");
//#endif

        int[] cycle = {0};
        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int src = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);
//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        long dst = reg.getD()[dr] & 0xffff_ffffL;
//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif


        // compute
        long ans = dst - (long) src;
        reg.setDl(dr, (int) ans);

        // flag
        reg.setN((int) ans);
        reg.setZ((int) ans);
        reg.setVcmp(src, (int) dst, (int) ans);
        reg.setCcmp(src, (int) dst, (int) ans);
        reg.setX(reg.getC());

        // cycle
        cycle[0] = Cycle.Sub_l[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int csubaw(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("SUBA.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int val = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // compute
        reg.getA().set(dr, reg.getA().get(dr) - val);
//#if DEBUG
        if (TRACE) nimo.append("A%s".formatted(dr));
//#endif

        // flag
        // none

        // cycle
        cycle[0] = Cycle.Suba_w[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int csubal(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("SUBA.l ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int val = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // compute
        reg.getA().set(dr, reg.getA().get(dr) - val);
//#if DEBUG
        if (TRACE) nimo.append("A%s".formatted(dr));
//#endif

        // flag
        // none

        // cycle
        cycle[0] = Cycle.Suba_l[cycle[0]];

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int ccmp(short n) {
        int opmode = (n & 0x1c0) >> 6;

        return switch (opmode) {
            case 0 -> ccmp_b(n);
            case 1 -> ccmp_w(n);
            case 2 -> ccmp_l(n);
            case 3 -> ccmpa_w(n);
            case 4 -> ccmpm_b(n);
            case 5 -> ccmpm_w(n);
            case 6 -> ccmpm_l(n);
            case 7 -> ccmpa_l(n);
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };

    }

    private int ccmp_b(short n) {
        int cycle = 0;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMP.b ";
//#endif

        int rn = (n & 0x0e00) >> 9;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        short after;
        short src = 0;
        byte dst = reg.getDb(rn);

        // dst
        switch (dm) {
            case 0: // Dn
                src = (short) (reg.getDb(dr) & 0xff);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Cmp_b[0];
                break;
            case 1: // An
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
            case 2: // (An)
                src = (short) (mem.peekB(reg.getA().get(dr)) & 0xff);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Cmp_b[2];
                break;
            case 3: // (An)+
                src = (short) (mem.peekB(reg.getA().get(dr)) & 0xff);
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Cmp_b[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) - 1);
                src = (short) (mem.peekB(reg.getA().get(dr)) & 0xff);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Cmp_b[4];
                break;
            case 5: // d16(An)
                int d16 = fetchW(); // signed
                src = (short) (mem.peekB(reg.getA().get(dr) + d16) & 0xff);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted((short) d16, dr);
//#endif

                cycle = Cycle.Cmp_b[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = (short) (mem.peekB(ptr) & 0xff);
                cycle = Cycle.Cmp_b[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted(ptr);
//#endif

                        src = (short) (mem.peekB(ptr) & 0xff);
                        cycle = Cycle.Cmp_b[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        src = (short) (mem.peekB(ptr) & 0xff);
                        cycle = Cycle.Cmp_b[8];
                        break;
                    case 2:
                        int ed16 = fetchW(); // signed
                        src = (short) (mem.peekB(reg.pc + ed16 - 2) & 0xff);
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted((short) ed16);
//#endif

                        cycle = Cycle.Cmp_b[9];
                        break;
                    case 3:
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;

//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        src = (short) (mem.peekB(ptr) & 0xff);
                        cycle = Cycle.Cmp_b[10];
                        break;
                    case 4:
                        src = (byte) fetchW();
//#if DEBUG
                        if (TRACE) nimo += "#$%02x".formatted((byte) src);
//#endif

                        cycle = Cycle.Cmp_b[11];
                        break;
                }
                break;
        }

        after = (short) ((dst - src) & 0xffff);

//#if DEBUG
        if (TRACE) nimo += ",D%d".formatted(rn);
//#endif

        // flag
        reg.setN((byte) after);
        reg.setZ((byte) after);
        reg.setVcmp((byte) src, dst, (byte) after);
        reg.setCcmp((byte) src, dst, (byte) after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmp_w(short n) {
        int cycle = 0;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMP.w ";
//#endif

        int rn = (n & 0x0e00) >> 9;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        short after;
        short src = 0;
        short dst = reg.getDw(rn);

        // dst
        switch (dm) {
            case 0: // Dn
                src = reg.getDw(dr);
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                cycle = Cycle.Cmp_w[0];
                break;
            case 1: // An
                src = reg.getAw(dr);
//#if DEBUG
                if (TRACE) nimo += "A%s".formatted(dr);
//#endif

                cycle = Cycle.Cmp_w[1];
                break;
            case 2: // (An)
                src = mem.peekW(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Cmp_w[2];
                break;
            case 3: // (An)+
                src = mem.peekW(reg.getA().get(dr));
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                cycle = Cycle.Cmp_w[3];
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                src = mem.peekW(reg.getA().get(dr));
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Cmp_w[4];
                break;
            case 5: // d16(An)
                int d16 = fetchW();
                src = mem.peekW(reg.getA().get(dr) + d16);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted((short) d16, dr);
//#endif

                cycle = Cycle.Cmp_w[5];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                src = mem.peekW(ptr);
                cycle = Cycle.Cmp_w[6];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "($%04x)".formatted((short) ptr);
//#endif

                        src = mem.peekW(ptr);
                        cycle = Cycle.Cmp_w[7];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "($%08x)".formatted(ptr);
//#endif

                        src = mem.peekW(ptr);
                        cycle = Cycle.Cmp_w[8];
                        break;
                    case 2:
                        int ed16 = fetchW();
                        src = mem.peekW(reg.pc + ed16 - 2);
//#if DEBUG
                        if (TRACE) nimo += "$%04x(PC)".formatted((short) ed16);
//#endif

                        cycle = Cycle.Cmp_w[9];
                        break;
                    case 3:
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo += "$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        src = mem.peekW(ptr);
                        cycle = Cycle.Cmp_w[10];
                        break;
                    case 4:
                        src = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "#$%02x".formatted((byte) src);
//#endif

                        cycle = Cycle.Cmp_w[11];
                        break;
                }
                break;
        }

        //after = (short) ((short)before - (short) val);
        after = (short) ((dst - src) & 0xffff);

//#if DEBUG
        if (TRACE) nimo += ",D%d".formatted(rn);
//#endif

        // flag
        reg.setN(after);
        reg.setZ(after);
        reg.setVcmp(src, dst, after);
        reg.setCcmp(src, dst, after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmp_l(short n) {
        int[] cycle = {0};
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("CMP.l ");
//#endif

        int rn = (n & 0x0e00) >> 9;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        int after;

        int src = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, dm, dr, 0xfff, true, 0);

        int dst = reg.getDl(rn);
        cycle[0] = Cycle.Cmp_l[cycle[0]];

        after = dst - src;

//#if DEBUG
        if (TRACE) nimo.append(",D%d".formatted(rn));
//#endif

        // flag
        reg.setN(after);
        reg.setZ(after);
        reg.setVcmp(src, dst, after);
        reg.setCcmp(src, dst, after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int ccmpa_w(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int ccmpa_l(short n) {
        int[] cycle = {0};
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("CMPA.l ");
//#endif

        int rn = (n & 0x0e00) >> 9;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        int after;
        int val = reg.getAl(rn);

        // dst
        int before = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, dm, dr, 0xfff, true, 0);

        cycle[0] = Cycle.Cmpa_l[cycle[0]];

        after = val - before;

//#if DEBUG
        if (TRACE) nimo.append(",A%s".formatted(rn));
//#endif

        // flag
        reg.setN(after);
        reg.setZ(after);
        reg.setVcmp(val, before, after);
        reg.setCcmp(val, before, after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int ccmpm_b(short n) {
        int cycle = 12;
        int dr = (n & 0x0e00) >> 9;
        int sr = (n & 0x0007);

        short after;
        short dst;
        byte vald = mem.peekB(reg.getAl(dr));
        byte src = mem.peekB(reg.getAl(sr));

        dst = vald;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "CMPM.b (A%d)+,(A%d)+".formatted(sr, dr);
//#endif

        reg.getA().set(dr, reg.getA().get(dr) + 1);
        reg.getA().set(sr, reg.getA().get(sr) + 1);

        after = (short) (dst - src); // signed

        // flag
        reg.setN((byte) after);
        reg.setZ((byte) after);
        // reg.SetVcmp((byte)before, (byte)vals, (byte)after);
        // reg.SetCcmp((byte)before, (byte)vals, (byte)after);
        reg.setVcmp(src, (byte) dst, (byte) after);
        reg.setCcmp(src, (byte) dst, (byte) after);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int ccmpm_w(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int ccmpm_l(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cmuls(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MULS.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = (short) reg.getD()[dr];

        // compute
        int ans = dval * sval;
        reg.getD()[dr] = ans;

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Muls_w[cycle[0]];

        // flag
        // reg.X -
        reg.setN((ans & 0x8000_0000) != 0);
        reg.setZ(ans == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cmulu(short n) {

        if ((n & 0xf1c0) != 0xc0c0) {
            return cand(n);
        }

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("MULU.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true) & 0xffff;

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = reg.getD()[dr] & 0xffff;

        // compute
        int ans = dval * sval;
        reg.getD()[dr] = ans;

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Mulu_w[cycle[0]];

        // flag
        // reg.X -
        reg.setN((ans & 0x8000_0000) != 0);
        reg.setZ(ans == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cdivs(short n) {

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("DIVS.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = reg.getD()[dr];

        // check TBD
        if (sval == 0) {
            // TRAP
        }
        // compute
        int ans = dval / sval;
        int mod = dval % sval;
        if (ans > 32767 || ans < -32768) {
            reg.setV(true);
            return Cycle.Divs_w[cycle[0]];
        }

        reg.getD()[dr] = (ans & 0xffff) | ((mod & 0xffff) * 0x10000);

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Divs_w[cycle[0]];

        // flag
        // reg.X -
        reg.setN((ans & 0x8000) != 0);
        reg.setZ(ans == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cdivu(short n) {

        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("DIVU.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true) & 0xffff;

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = reg.getD()[dr];

        // check TBD
        if (sval == 0) {
            // TRAP
        }
        // compute
        int ans = Integer.divideUnsigned(dval, sval);
        int mod = Integer.remainderUnsigned(dval, sval);
        if (Integer.compareUnsigned(ans, 0xffff) > 0) {
            reg.setV(true);
            return Cycle.Divs_w[cycle[0]];
        }

        reg.getD()[dr] = (ans & 0xffff) | ((mod & 0xffff) * 0x10000);

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Divu_w[cycle[0]];

        // flag
        // reg.X -
        reg.setN((ans & 0x8000) != 0);
        reg.setZ(ans == 0);
        reg.setV(false);
        reg.setC(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int caddi(short n) {
        int m = (n & 0x00c0) >> 6;
        return switch (m) {
            case 0 -> // byte
                    caddib(n);
            case 1 -> // word
                    caddiw(n);
            case 2 -> // long
                    caddil(n);
            default -> throw new UnsupportedOperationException("dummy");
        };

    }

    private int caddib(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDi.b ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        byte src = (byte) fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%02x,".formatted(src);
//#endif

        byte dst = 0;
        byte ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                dst = reg.getDb(dr);
                ans = (byte) (dst + src); // signed
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                reg.setDb(dr, ans);
                cycle = Cycle.Addi_b[0];
                break;
            case 2: // (An)
                dst = mem.peekB(reg.getA().get(dr));
                ans = (byte) (dst + src);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                mem.pokeB(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_b[1];
                break;
            case 3: // (An)+
                dst = mem.peekB(reg.getA().get(dr));
                ans = (byte) (dst +  src);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                mem.pokeB(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_b[2];
                reg.getA().set(dr, reg.getA().get(dr) + 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 1);
                if (dr == 7) reg.getA().set(dr, reg.getA().get(dr) + 1);
                dst = mem.peekB(reg.getA().get(dr));
                ans = (byte) (dst + src);
                mem.pokeB(reg.getA().get(dr), ans);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                cycle = Cycle.Addi_b[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekB(reg.getA().get(dr) + d16);
                ans = (byte) (dst + src);
                mem.pokeB(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addi_b[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                dst = mem.peekB(ptr);
                ans = (byte) (dst + src);
                mem.pokeB(ptr, ans);
                cycle = Cycle.Addi_b[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr);
                        ans = (byte) (dst + src);
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Addi_b[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekB(ptr);
                        ans = (byte) (dst + src);
                        mem.pokeB(ptr, ans);
                        cycle = Cycle.Addi_b[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVadd(src, dst, ans);
        reg.setCadd(src, dst, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddiw(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDi.w ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        short src = fetchW();
//#if DEBUG
        if (TRACE) nimo += "#$%04x,".formatted(src);
//#endif

        short dst = 0;
        short ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                dst = reg.getDw(dr);
                ans = (short) (dst + src); // signed
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                reg.setDw(dr, ans);
                cycle = Cycle.Addi_w[0];
                break;
            case 2: // (An)
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (dst + src);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                mem.pokeW(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_w[1];
                break;
            case 3: // (An)+
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (dst + src);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                mem.pokeW(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_w[2];
                reg.getA().set(dr, reg.getA().get(dr) + 2);
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                dst = mem.peekW(reg.getA().get(dr));
                ans = (short) (dst + src);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                mem.pokeW(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_w[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekW(reg.getA().get(dr) + d16);
                ans = (short) (dst + src);
                mem.pokeW(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addi_w[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                dst = mem.peekW(ptr);
                ans = (short) (dst + src);
                mem.pokeW(ptr, ans);
                cycle = Cycle.Addi_w[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) (dst + src);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Addi_w[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekW(ptr);
                        ans = (short) (dst + src);
                        mem.pokeW(ptr, ans);
                        cycle = Cycle.Addi_w[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVadd(src, dst, ans);
        reg.setCadd(src, dst, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddil(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADDi.l ";
//#endif

        int cycle = 0;

        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);

        int src = fetchL();
//#if DEBUG
        if (TRACE) nimo += "#$%08x,".formatted(src);
//#endif

        int dst = 0;
        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 0: // Dn
                dst = reg.getDl(dr);
                ans = dst + src;
//#if DEBUG
                if (TRACE) nimo += "D%d".formatted(dr);
//#endif

                reg.setDl(dr, ans);
                cycle = Cycle.Addi_l[0];
                break;
            case 2: // (An)
                dst = mem.peekL(reg.getA().get(dr));
                ans = dst + src;
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif

                mem.pokeL(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_l[1];
                break;
            case 3: // (An)+
                dst = mem.peekL(reg.getA().get(dr));
                ans = dst + src;
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif

                mem.pokeL(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_l[2];
                reg.getA().set(dr, reg.getA().get(dr) + 4);
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 4);
                dst = mem.peekL(reg.getA().get(dr));
                ans = dst + src;
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif

                mem.pokeL(reg.getA().get(dr), ans);
                cycle = Cycle.Addi_l[3];
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                dst = mem.peekL(reg.getA().get(dr) + d16);
                ans = dst + src;
                mem.pokeL(reg.getA().get(dr) + d16, ans);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif

                cycle = Cycle.Addi_l[4];
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                dst = mem.peekL(ptr);
                ans = dst + src;
                mem.pokeL(ptr, ans);
                cycle = Cycle.Addi_l[5];
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst + src;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Addi_l[6];
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dst = mem.peekL(ptr);
                        ans = dst + src;
                        mem.pokeL(ptr, ans);
                        cycle = Cycle.Addi_l[7];
                        break;
                }
                break;
        }

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVadd(src, dst, ans);
        reg.setCadd(src, dst, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cadd(short n) {
        // 0000 0110 xxxx xxxx ADDI
        // 0101 xxx0 xxxx xxxx ADDQ
        // 1101 xxx1 xx00 0xxx ADDX dr to dr
        // 1101 xxx1 xx00 1xxx ADDX mem to mem
        // 1101 xxx1 11xx xxxx ADDA
        // 1101 xxxx xxxx xxxx ADDA
        if ((n & 0xf138) == 0xd100) {
            // ADDX dr to dr
            int mm = (n & 0x01c0) >> 6;
            if (mm != 3 && mm != 7) {
                mm = (n & 0x00c0) >> 6;
                switch (mm) {
                    case 0: // byte
                        return caddxb_dd(n);
                    case 1: // word
                        return caddxw_dd(n);
                    case 2: // long
                        return caddxl_dd(n);
                }
            }
        } else if ((n & 0xf138) == 0xd108) {
            // ADDX mem to mem
            int mm = (n & 0x01c0) >> 6;
            if (mm != 3 && mm != 7) {
                mm = (n & 0x00c0) >> 6;
                switch (mm) {
                    case 0: // byte
                        return caddxb_mm(n);
                    case 1: // word
                        return caddxw_mm(n);
                    case 2: // long
                        return caddxl_mm(n);
                }
            }
        }

        int m = (n & 0x01c0) >> 6;
        return switch (m) {
            case 0 -> // byte
                    cadd0B(n);
            case 1 -> // word
                    cadd0w(n);
            case 2 -> // long
                    cadd0l(n);
            case 3 -> // word Cadda
                    caddaw(n);
            case 4 -> // byte
                    cadd1b(n);
            case 5 -> // word
                    cadd1w(n);
            case 6 -> // long
                    cadd1l(n);
            case 7 -> // long Cadda
                    caddal(n);
            default -> throw new UnsupportedOperationException("dummy");
        };

    }

    private int caddxb_dd(short n) {
        String nimo = "";
//#if DEBUG
        if (TRACE) nimo = "ADDX.b ";
//#endif

        int cycle = 4;

        int dr = (n & 0x0e00) >> 9;
        int sr = (n & 0x0007);

        // src
        int sval = reg.getDb(sr);

//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(sr);
//#endif

        // dst
        int dval = reg.getDb(dr);

        // compute
        int ans = dval + sval + (reg.getX() ? 1 : 0);
        reg.setDb(dr, (byte) ans);

//#if DEBUG
        if (TRACE) nimo += "D%d".formatted(dr);
//#endif

        // flag
        reg.setN((byte) ans);
        reg.setZ((byte) ans);
        reg.setVadd((byte) sval, (byte) dval, (byte) ans);
        reg.setCadd((byte) sval, (byte) dval, (byte) ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddxw_dd(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int caddxl_dd(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int caddxb_mm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int caddxw_mm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int caddxl_mm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cadd0B(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("ADD.b ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = srcAddressingByte(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif


        // dst
        int dval = reg.getDb(dr);

        // compute
        int ans = dval + sval;
        reg.setDb(dr, (byte) ans);

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Add0_b[cycle[0]];

        // flag
        reg.setN((byte) ans);
        reg.setZ((byte) ans);
        reg.setVadd((byte) sval, (byte) dval, (byte) ans);
        reg.setCadd((byte) sval, (byte) dval, (byte) ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cadd0w(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("ADD.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = (short) reg.getD()[dr];

        // compute
        int ans = dval + sval;
        reg.setDw(dr, (short) ans);

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Add0_w[cycle[0]];

        // flag
        reg.setN((short) ans);
        reg.setZ((short) ans);
        reg.setVadd((short) sval, (short) dval, (short) ans);
        reg.setCadd((short) sval, (short) dval, (short) ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cadd0l(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("ADD.l ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        long sval = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        long dval = reg.getD()[dr];

        // compute
        long ans = dval + sval;
        reg.getD()[dr] = (int) ans;

//#if DEBUG
        if (TRACE) nimo.append("D%d".formatted(dr));
//#endif

        cycle[0] = Cycle.Add0_l[cycle[0]];

        // flag
        reg.setN((int) ans);
        reg.setZ((int) ans);
        reg.setVadd((int) sval, (int) dval, (int) ans);
        reg.setCadd((int) sval, (int) dval, (int) ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cadd1b(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADD.b ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        // src
        byte sval = reg.getDb(dr); // signed
        // dst
        byte dval = 0; // signed

        byte ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dval = mem.peekB(reg.getA().get(sr));
                ans = (byte) (dval + sval);
                mem.pokeB(reg.getA().get(sr), ans);
                cycle = 0;
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dval = mem.peekB(reg.getA().get(sr));
                ans = (byte) (dval + sval);
                mem.pokeB(reg.getA().get(sr), ans);
                reg.getA().set(sr, reg.getA().get(sr) + 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) + 1);
                cycle = 1;
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) - 1);
                dval = mem.peekB(reg.getA().get(sr));
                ans = (byte) (dval + sval);
                mem.pokeB(reg.getA().get(sr), ans);
                cycle = 2;
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dval = mem.peekB(reg.getA().get(sr) + d16);
                ans = (byte) (dval + sval);
                mem.pokeB(reg.getA().get(sr) + d16, ans);
                cycle = 3;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dval = mem.peekB(ptr);
                ans = (byte) (dval + sval);
                mem.pokeB(ptr, ans);
                cycle = 4;
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dval = mem.peekB(ptr);
                        ans = (byte) (dval + sval);
                        mem.pokeB(ptr, ans);
                        cycle = 5;
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dval = mem.peekB(ptr);
                        ans = (byte) (dval + sval);
                        mem.pokeB(ptr, ans);
                        cycle = 6;
                        break;
                }
                break;
        }

        cycle = Cycle.Add1_b[cycle];

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVadd(sval, dval, ans);
        reg.setCadd(sval, dval, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cadd1w(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADD.w ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        // src
        int sval = (short) reg.getD()[dr];

        // dst
        int dval = 0;

        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dval = mem.peekW(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                cycle = 0;
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dval = mem.peekW(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                reg.getA().set(sr, reg.getA().get(sr) + 2);
                cycle = 1;
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 2);
                dval = mem.peekW(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeW(reg.getA().get(sr), (short) ans);
                cycle = 2;
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dval = mem.peekW(reg.getA().get(sr) + d16);
                ans = dval + sval;
                mem.pokeW(reg.getA().get(sr) + d16, (short) ans);
                cycle = 3;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dval = mem.peekW(ptr);
                ans = dval + sval;
                mem.pokeW(ptr, (short) ans);
                cycle = 4;
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dval = mem.peekW(ptr);
                        ans = dval + sval;
                        mem.pokeW(ptr, (short) ans);
                        cycle = 5;
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dval = mem.peekW(ptr);
                        ans = dval + sval;
                        mem.pokeW(ptr, (short) ans);
                        cycle = 6;
                        break;
                }
                break;
        }

        cycle = Cycle.Add1_w[cycle];

        // flag
        reg.setN((short) ans);
        reg.setZ((short) ans);
        reg.setVadd((short) sval, (short) dval, (short) ans);
        reg.setCadd((short) sval, (short) dval, (short) ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cadd1l(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ADD.l ";
//#endif

        int cycle = 0;

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

//#if DEBUG
        if (TRACE) nimo += "D%d,".formatted(dr);
//#endif

        // src
        int sval = reg.getDl(dr);

        // dst
        int dval = 0;

        int ans = 0;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (sm) {
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(sr);
//#endif

                dval = mem.peekL(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeL(reg.getA().get(sr), ans);
                cycle = 0;
                break;
            case 3: // (An)+
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(sr);
//#endif

                dval = mem.peekL(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeL(reg.getA().get(sr), ans);
                reg.getA().set(sr, reg.getA().get(sr) + 4);
                cycle = 1;
                break;
            case 4: // -(An)
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(sr);
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 4);
                dval = mem.peekL(reg.getA().get(sr));
                ans = dval + sval;
                mem.pokeL(reg.getA().get(sr), ans);
                cycle = 2;
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, sr);
//#endif

                dval = mem.peekL(reg.getA().get(sr) + d16);
                ans = dval + sval;
                mem.pokeL(reg.getA().get(sr) + d16, ans);
                cycle = 3;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                dval = mem.peekL(ptr);
                ans = dval + sval;
                mem.pokeL(ptr, ans);
                cycle = 4;
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        dval = mem.peekL(ptr);
                        ans = dval + sval;
                        mem.pokeL(ptr, ans);
                        cycle = 5;
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        dval = mem.peekL(ptr);
                        ans = dval + sval;
                        mem.pokeL(ptr, ans);
                        cycle = 6;
                        break;
                }
                break;
        }

        cycle = Cycle.Add1_l[cycle];

        // flag
        reg.setN(ans);
        reg.setZ(ans);
        reg.setVadd(sval, dval, ans);
        reg.setCadd(sval, dval, ans);
        reg.setX(reg.getC());

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int caddaw(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("ADDA.w ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        int sval = (short) srcAddressingWord(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        int dval = reg.getA().get(dr);

        // compute
        int ans = dval + sval;
        reg.getA().set(dr, ans);

//#if DEBUG
        if (TRACE) nimo.append("A%s".formatted(dr));
//#endif

        cycle[0] = Cycle.Adda_w[cycle[0]];

        // flag
        //reg.SetN((short)ans);
        //reg.SetZ((short)ans);
        //reg.SetVadd((short)sval, (short)dval, (short)ans);
        //reg.SetCadd((short)sval, (short)dval, (short)ans);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int caddal(short n) {
        StringBuilder nimo = new StringBuilder();
//#if DEBUG
        if (TRACE) nimo.append("ADDA.l ");
//#endif

        int[] cycle = {0};

        int dr = (n & 0x0e00) >> 9;
        // int dm = (n & 0x01c0) >> 6;
        int sm = (n & 0x0038) >> 3;
        int sr = (n & 0x0007);

        // src
        long sval = srcAddressingLong(/* ref */ nimo, /* ref */ cycle, sm, sr, 0xfff, true, 0);

//#if DEBUG
        if (TRACE) nimo.append(",");
//#endif

        // dst
        long dval = reg.getA().get(dr);

        // compute
        long ans = dval + sval;
        reg.getA().set(dr, (int) ans);

//#if DEBUG
        if (TRACE) nimo.append("A%s".formatted(dr));
//#endif

        cycle[0] = Cycle.Adda_l[cycle[0]];

        // flag
        //reg.SetN((int) ans);
        //reg.SetZ((int) ans);
        //reg.SetVadd((int) sval, (int) dval, (int) ans);
        //reg.SetCadd((int) sval, (int) dval, (int) ans);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int cshift(short n) {
        // 1110 ***1 **10 0*** -> ASL.b.w.l Dn,Dn
        // 1110 ***1 **00 0*** -> ASL.b.w.l #imm,Dn
        // 1110 0001 11** **** -> ASL.w     ea

        // 1110 ***0 **10 0*** -> ASR.b.w.l Dn,Dn
        // 1110 ***0 **00 0*** -> ASR.b.w.l #imm,Dn
        // 1110 0000 11** **** -> ASR.w     ea

        // 1110 ***1 **10 1*** -> LSL.b.w.l Dn,Dn
        // 1110 ***1 **00 1*** -> LSL.b.w.l #imm,Dn
        // 1110 0011 11** **** -> LSL.w     ea

        // 1110 ***0 **10 1*** -> LSR.b.w.l Dn,Dn
        // 1110 ***0 **00 1*** -> LSR.b.w.l #imm,Dn
        // 1110 0010 11** **** -> LSR.w     ea

        // 1110 ***1 **11 1*** -> ROL.b.w.l Dn,Dn
        // 1110 ***1 **01 1*** -> ROL.b.w.l #imm,Dn
        // 1110 0111 11** **** -> ROL.w     ea

        // 1110 ***0 **11 1*** -> ROR.b.w.l Dn,Dn
        // 1110 ***0 **01 1*** -> ROR.b.w.l #imm,Dn
        // 1110 0110 11** **** -> ROR.w     ea

        // 1110 ***1 **11 0*** -> ROXL.b.w.l Dn,Dn
        // 1110 ***1 **01 0*** -> ROXL.b.w.l #imm,Dn
        // 1110 0101 11** **** -> ROXL.w     ea

        // 1110 ***0 **11 0*** -> ROXR.b.w.l Dn,Dn
        // 1110 ***0 **01 0*** -> ROXR.b.w.l #imm,Dn
        // 1110 0100 11** **** -> ROXR.w     ea

        int cnt = (n & 0x0e00) >> 9;
        boolean dirL = ((n & 0x0100) >> 8) == 1;
        int siz = (n & 0x00c0) >> 6;
        boolean imm = ((n & 0x0020) >> 5) == 0;
        int typ = (n & 0x0018) >> 3;

        if (siz != 0x3) {
            switch (siz) {
                case 0:
                    switch (typ) {
                        case 0:
                            return dirL ? (imm ? casl_b_imm(n) : casl_b_DnDn(n)) : (imm ? casr_b_imm(n) : casr_b_DnDn(n));
                        case 1:
                            return dirL ? (imm ? clsl_b_imm(n) : clsl_b_DnDn(n)) : (imm ? clsr_b_imm(n) : clsr_b_DnDn(n));
                        case 2:
                            return dirL ? (imm ? croxl_b_imm(n) : croxl_b_DnDn(n)) : (imm ? croxr_b_imm(n) : croxr_b_DnDn(n));
                        case 3:
                            return dirL ? (imm ? crol_b_imm(n) : crol_b_DnDn(n)) : (imm ? cror_b_imm(n) : cror_b_DnDn(n));
                    }
                    break;
                case 1:
                    switch (typ) {
                        case 0:
                            return dirL ? (imm ? casl_w_imm(n) : casl_w_DnDn(n)) : (imm ? casr_w_imm(n) : casr_w_DnDn(n));
                        case 1:
                            return dirL ? (imm ? clsl_w_imm(n) : clsl_w_DnDn(n)) : (imm ? clsr_w_imm(n) : clsr_w_DnDn(n));
                        case 2:
                            return dirL ? (imm ? croxl_w_imm(n) : croxl_w_DnDn(n)) : (imm ? croxr_w_imm(n) : croxr_w_DnDn(n));
                        case 3:
                            return dirL ? (imm ? crol_w_imm(n) : crol_w_DnDn(n)) : (imm ? cror_w_imm(n) : cror_w_DnDn(n));
                    }
                    break;
                case 2:
                    switch (typ) {
                        case 0:
                            return dirL ? (imm ? casl_l_imm(n) : casl_l_DnDn(n)) : (imm ? casr_l_imm(n) : casr_l_DnDn(n));
                        case 1:
                            return dirL ? (imm ? clsl_l_imm(n) : clsl_l_DnDn(n)) : (imm ? clsr_l_imm(n) : clsr_l_DnDn(n));
                        case 2:
                            return dirL ? (imm ? croxl_l_imm(n) : croxl_l_DnDn(n)) : (imm ? croxr_l_imm(n) : croxr_l_DnDn(n));
                        case 3:
                            return dirL ? (imm ? crol_l_imm(n) : crol_l_DnDn(n)) : (imm ? cror_l_imm(n) : cror_l_DnDn(n));
                    }
                    break;
            }
        } else {
            switch (cnt) {
                case 0:
                    return dirL ? casl_w_ea(n) : casr_w_ea(n);
                case 1:
                    return dirL ? clsl_w_ea(n) : clsr_w_ea(n);
                case 2:
                    return dirL ? croxl_w_ea(n) : croxr_w_ea(n);
                case 3:
                    return dirL ? crol_w_ea(n) : cror_w_ea(n);
            }
        }

        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int casl_b_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "ASL.b D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        byte bv = reg.getDb(dr); // signed
        byte av = (byte) (bv << cnt); // signed
        reg.setDb(dr, av);

        reg.setC((bv & (0x80 >> (cnt - 1))) != 0);
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASL.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        byte bv = reg.getDb(d); // signed, Arithmetic shifts are done on signed types
        byte av = (byte) (bv << cnt); // signed

        reg.setDb(d, av);
        reg.setX(((bv & (0x80 >> (cnt - 1))) != 0));
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_w_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "ASL.w D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        short bv = reg.getDw(dr);
        short av = (short) (bv << cnt);
        reg.setDw(dr, av);

        reg.setC((bv & (0x8000 >> (cnt - 1))) != 0);
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xffff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASL.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        short bv = reg.getDw(d); // Arithmetic shifts are done on signed types
        short av = (short) (bv << cnt);

        reg.setDw(d, av);
        reg.setX(((bv & (0x8000 >> (cnt - 1))) != 0));
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xffff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_l_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "ASL.l D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDl(dr);
        int av = bv << cnt;
        reg.setDl(dr, av);

        reg.setC((bv & (0x8000_0000 >>> (cnt - 1))) != 0);
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xffff_ffff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASL.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d); // Arithmetic shifts are done on signed types
        int av = bv << cnt;
        reg.setDl(d, av);

        reg.setX(((bv & (0x8000_0000 >>> (cnt - 1))) != 0));
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV((bv & (0xffff_ffff << cnt)) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casl_w_ea(short n) {
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "ASL.w ";
//#endif

        short before = 0;
        short after = 0;

        int cycle = 0;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
        int sr = (n & 0x0e00) >> 9;

        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;

        switch (dm) {
            case 2: // (An)
                before = mem.peekW(reg.getA().get(dr));
                after = (short) (before << 1);
                mem.pokeW(reg.getA().get(dr), after);
//#if DEBUG
                if (TRACE) nimo += "(A%d)".formatted(dr);
//#endif
                cycle = 12;
                break;
            case 3: // (An)+
                before = mem.peekW(reg.getA().get(dr));
                after = (short) (before << 1);
                mem.pokeW(reg.getA().get(dr), after);
                reg.getA().set(dr, reg.getA().get(dr) + 2);
//#if DEBUG
                if (TRACE) nimo += "(A%d)+".formatted(dr);
//#endif
                cycle = 12;
                break;
            case 4: // -(An)
                reg.getA().set(dr, reg.getA().get(dr) - 2);
                before = mem.peekW(reg.getA().get(dr));
                after = (short) (before << 1);
                mem.pokeW(reg.getA().get(dr), after);
//#if DEBUG
                if (TRACE) nimo += "-(A%d)".formatted(dr);
//#endif
                cycle = 14;
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
                before = mem.peekW(reg.getA().get(dr) + d16);
                after = (short) (before << 1);
                mem.pokeW(reg.getA().get(dr) + d16, after);
//#if DEBUG
                if (TRACE) nimo += "$%04x(A%d)".formatted(d16, dr);
//#endif
                cycle = 16;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo += "$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, dr, isA ? "A" : "D", ni, isL ? "l" : "w");
//#endif

                if (!isL) ptr = reg.getA().get(dr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(dr) + (byte) vw + IX;
                before = mem.peekW(ptr);
                after = (short) (before << 1);
                mem.pokeW(ptr, after);
                cycle = 18;
                break;
            case 7: // etc.
                switch (dr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo += "$%04x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) (before << 1);
                        mem.pokeW(ptr, after);
                        cycle = 16;
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo += "$%08x".formatted(ptr);
//#endif

                        before = mem.peekW(ptr);
                        after = (short) (before << 1);
                        mem.pokeW(ptr, after);
                        cycle = 20;
                        break;
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        }

        reg.setX(((before & 0x8000) != 0));
        reg.setC(reg.getX());
        reg.setN(after);
        reg.setZ(after);
        reg.setV((before & 0xffff) != 0);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_b_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;
//#if DEBUG
        if (TRACE) nimo = "ASR.b D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;
        byte bv = reg.getDb(dr); // Arithmetic shifts are done on signed types
        byte av = (byte) (bv >> cnt);
        reg.setDb(dr, av);

        reg.setC((bv & (0x01 << (cnt - 1))) != 0);
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASR.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        byte bv = reg.getDb(d); // Arithmetic shifts are done on signed types
        byte av = (byte) (bv >> cnt);
        reg.setDb(d, av);

        reg.setX(((bv & (0x01 << (cnt - 1))) != 0));
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_w_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;
//#if DEBUG
        if (TRACE) nimo = "ASR.w D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;
        short bv = reg.getDw(dr); // Arithmetic shifts are done on signed types
        short av = (short) (bv >> cnt);
        reg.setDw(dr, av);

        reg.setC((bv & (0x0001 << (cnt - 1))) != 0);
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASR.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        short bv = reg.getDw(d); // Arithmetic shifts are done on signed types
        short av = (short) (bv >> cnt);
        reg.setDw(d, av);

        reg.setX((bv & (0x0001 << (cnt - 1))) != 0);
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_l_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;
//#if DEBUG
        if (TRACE) nimo = "ASR.l D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(dr); // Arithmetic shifts are done on signed types
        int av = bv >> cnt;
        reg.setDl(dr, av);

        reg.setC(((bv & (0x0000_0001 << (cnt - 1))) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ASR.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d); // Arithmetic shifts are done on signed types
        int av = bv >> cnt;
        reg.setDl(d, av);

        reg.setX(((bv & (0x0000_0001 << (cnt - 1))) != 0));
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int casr_w_ea(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int clsl_b_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSL.b D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDb(dr) & 0xff;
        int av = bv << cnt;
        reg.setDb(dr, (byte) av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x80 >> cnt)) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSL.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDb(d) & 0xff;
        int av = bv << cnt;
        reg.setDb(d, (byte) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x80 >> cnt)) != 0);
        reg.setC(reg.getX());
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_w_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSL.w D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDw(dr) & 0xffff;
        int av = bv << cnt;
        reg.setDw(dr, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x8000 >> cnt)) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSL.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDw(d) & 0xffff;
        int av = bv << cnt;
        reg.setDw(d, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x8000 >> cnt)) != 0);
        reg.setC(reg.getX());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_l_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSL.l D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDl(dr);
        int av = bv << cnt;
        reg.setDl(dr, av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x8000_0000 >>> cnt)) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSL.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d);
        int av = bv << cnt;
        reg.setDl(d, av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x8000_0000 >>> cnt)) != 0);
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsl_w_ea(short n) {
        StringBuilder nimo = new StringBuilder();

        int[] cycle = {0};

        int cnt = 1;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo.append("LSL.w ");
//#endif

        // dst
        int before = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, dm, dr, 0b0001_1111_1100, true);

        cycle[0] = Cycle.Clsrlsl_wea[cycle[0]];
        int av = before << cnt;
        reg.setDw(dr, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((before & (0x8000 >> cnt)) != 0);
        reg.setC(reg.getX());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int clsr_b_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSR.b D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDb(dr) & 0xff;
        int av = bv >> cnt;
        reg.setDb(dr, (byte) av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x01 << (cnt - 1))) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSR.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDb(d) & 0xff;
        int av = bv >>> cnt;
        reg.setDb(d, (byte) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x01 << (cnt - 1))) != 0);
        reg.setC(reg.getX());
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_w_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;
        int sr = (n & 0x0e00) >> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSR.w D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDw(dr) & 0xffff;
        int av = bv >> cnt;
        reg.setDw(dr, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x0001 << (cnt - 1))) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSR.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDw(d) & 0xffff;
        int av = bv >>> cnt;
        reg.setDw(d, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x0001 << (cnt - 1))) != 0);
        reg.setC(reg.getX());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_l_DnDn(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;
        int sr = (n & 0x0e00) >>> 9;
        int dr = (n & 0x0007);
        int cnt = reg.getDl(sr) % 64;

//#if DEBUG
        if (TRACE) nimo = "LSR.l D%d,D%d".formatted(sr, dr);
//#endif

        cycle += 2 * cnt;

        int bv = reg.getDl(dr);
        int av = bv >>> cnt;
        reg.setDl(dr, av); // >> Logical shift >>= Arithmetic shift

        reg.setC((cnt == 0) ? false : ((bv & (0x0000_0001 << (cnt - 1))) != 0));
        if (cnt != 0) reg.setX(reg.getC());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >>> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "LSR.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d);
        int av = bv >>> cnt;
        reg.setDl(d, av); // >> Logical shift >>= Arithmetic shift

        reg.setX((bv & (0x0000_0001 << (cnt - 1))) != 0);
        reg.setC(reg.getX());
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int clsr_w_ea(short n) {
        StringBuilder nimo = new StringBuilder();

        int[] cycle = {0};

        int cnt = 1;
        int dm = (n & 0x0038) >> 3;
        int dr = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo.append("LSR.w ");
//#endif

        // dst
        int before = srcAddressingWord(/* ref */ nimo, /* ref */ cycle, dm, dr, 0b0001_1111_1100, true);

        cycle[0] = Cycle.Clsrlsl_wea[cycle[0]];

        int av = before >>> cnt;
        reg.setDw(dr, (short) av); // >> Logical shift >>= Arithmetic shift

        reg.setX((before & (0x0001 << (cnt - 1))) != 0);
        reg.setC(reg.getX());
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo.toString());
//#endif

        return cycle[0];
    }

    private int crol_b_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int crol_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROL.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDb(d) & 0xff;
        // int av = (bv >> cnt) | (bv << (32 - cnt)); // RLR
        int av = (bv << cnt) | (bv >>> (8 - cnt));
        reg.setDb(d, (byte) av);

        // reg.X no change
        reg.setC(((av & 0x1) != 0));
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int crol_w_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int crol_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROL.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDw(d) & 0xffff;
        int av = (bv << cnt) | (bv >>> (16 - cnt));
        reg.setDw(d, (short) av); // >> Logical shift >>= Arithmetic shift

        // reg.X no change
        reg.setC(((av & 0x1) != 0));
        reg.setN((short) av);
        reg.setZ((short) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int crol_l_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int crol_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROL.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d);
        // int av = (bv >> cnt) | (bv << (32 - cnt)); // RLR
        int av = (bv << cnt) | (bv >>> (32 - cnt));
        reg.setDl(d, av); // >> Logical shift >>= Arithmetic shift

        // reg.X no change
        reg.setC(((av & 0x1) != 0));
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int crol_w_ea(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cror_b_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cror_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROR.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        byte bv = reg.getDb(d);
        byte av = (byte) (((bv & 0xff) >>> cnt) | ((bv & 0xff) << (8 - cnt)));
        // int av = (bv << cnt) | (bv >>> (16 - cnt));
        reg.setDb(d, av);

        // reg.X no change
        reg.setC(((av & 0x80) != 0));
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cror_w_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cror_w_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROR.w #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        short bv = reg.getDw(d);
        short av = (short) (((bv & 0xffff) >>> cnt) | ((bv & 0xffff) << (16 - cnt)));
        // int av = (bv << cnt) | (bv >>> (16 - cnt));
        reg.setDw(d, av);

        // reg.X no change
        reg.setC(((av & 0x8000) != 0));
        reg.setN(av);
        reg.setZ(av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cror_l_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cror_l_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 8;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROR.l #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDl(d);
        int av = (bv >>> cnt) | (bv << (32 - cnt)); // ROR
        reg.setDl(d, av & 0xffff); // >> Logical shift >>= Arithmetic shift

        // reg.X no change
        reg.setC(((av & 0x8000_0000) != 0));
        reg.setN(av & 0xffff);
        reg.setZ(av & 0xffff);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cror_w_ea(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_b_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_b_imm(short n) {
//#if DEBUG
        String nimo = null;
//#endif

        int cycle = 6;

        int cnt = (n & 0x0e00) >> 9;
        cnt = (cnt == 0) ? 8 : cnt;
        int d = (n & 0x0007);
//#if DEBUG
        if (TRACE) nimo = "ROXL.b #%d,D%d".formatted(cnt, d);
//#endif

        cycle += 2 * cnt;
        int bv = reg.getDb(d) & 0xff;
        //int av = (bv >>> cnt) | (bv << (32 - cnt)); // RLR
        int av = (bv << cnt) | (((reg.getX() ? 0x100 : 0x000) | bv) >>> (9 - cnt));
        reg.setDb(d, (byte) av);

        reg.setC(((bv << cnt) & 0x100) != 0);
        reg.setX(reg.getC());
        reg.setN((byte) av);
        reg.setZ((byte) av);
        reg.setV(false);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int croxl_w_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_w_imm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_l_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_l_imm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxl_w_ea(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_b_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_b_imm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_w_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_w_imm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_l_DnDn(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_l_imm(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int croxr_w_ea(short n) {
        throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
    }

    private int cfeFunc(short n) {
        hmn.feFunc(n);
        return 0;
    }

    private int cdos(short n) {
        hmn.dosCall(n);
        return 0;
    }

    private int clink(short n) {
        int dr = (n & 0x7);
        int cycle = 16;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "LINK A%s, #${1:d}";
//#endif

        short ptr = fetchW();
//#if DEBUG
        if (TRACE) nimo = nimo.formatted(dr, ptr);
//#endif

        reg.getA().set(7, reg.getA().get(7) - 4);
        mem.pokeL(reg.getA().get(7), reg.getA().get(dr));
        reg.getA().set(dr, reg.getA().get(7));
        reg.getA().set(7, reg.getA().get(7) + (int) ptr);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int cunlk(short n) {
        int dr = (n & 0x7);
        int cycle = 16;
//#if DEBUG
        String nimo = null;
        if (TRACE) nimo = "UNLK A%s";
//#endif

//#if DEBUG
        if (TRACE) nimo = nimo.formatted(dr);
//#endif

        reg.getA().set(7, reg.getA().get(dr));
        reg.getA().set(dr, mem.peekL(reg.getA().get(7)));
        reg.getA().set(7, reg.getA().get(7) + 4);

//#if DEBUG
        if (TRACE) logger.log(Level.TRACE, nimo);
//#endif

        return cycle;
    }

    private int srcAddressingByte(/* ref */ StringBuilder nimo, /* ref */ int[] cycle, int sm, int sr, int support /* = 0xfff */, boolean nimoSw /* = true */) {
        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        byte val = 0;

        cycle[0] = sm;
        switch (sm) {
            case 0: // Dn
                if ((support & (1 << 0)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("D%d".formatted(sr));
//#endif

                val = (byte) reg.getD()[sr];
                break;
            case 1: // An
                if ((support & (1 << 1)) == 1) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                throw new IndexOutOfBoundsException("Unsupported addressing");
//#if DEBUG
                // nimo += "A%s".formatted(sr);
//#endif

                // val = (byte)reg.getA().get(sr);
                // break;
            case 2: // (An)
                if ((support & (1 << 2)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)".formatted(sr));
//#endif

                val = mem.peekB(reg.getA().get(sr));
                break;
            case 3: // (An)+
                if ((support & (1 << 3)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)+".formatted(sr));
//#endif

                val = mem.peekB(reg.getA().get(sr));
                reg.getA().set(sr, reg.getA().get(sr) + 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) + 1);
                break;
            case 4: // -(An)
                if ((support & (1 << 4)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("-(A%d)".formatted(sr));
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 1);
                if (sr == 7) reg.getA().set(sr, reg.getA().get(sr) - 1);
                val = mem.peekB(reg.getA().get(sr));
                break;
            case 5: // d16(An)
                if ((support & (1 << 5)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("$%04x(A%d)".formatted(d16, sr));
//#endif

                val = mem.peekB(reg.getA().get(sr) + d16);
                break;
            case 6: // d8(An,IX)
                if ((support & (1 << 6)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) if (nimoSw)
                    nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                val = mem.peekB(ptr);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        if ((support & (1 << 7)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%04x".formatted(ptr));
//#endif

                        val = mem.peekB(ptr);
                        break;
                    case 1: // Abs.L
                        if ((support & (1 << 8)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%08x".formatted(ptr));
//#endif

                        val = mem.peekB(ptr);
                        cycle[0] = 8;
                        break;
                    case 2: // d16(PC)
                        if ((support & (1 << 9)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%04x(PC)".formatted(ptr & 0xffff));
//#endif

                        val = mem.peekB(ptr + reg.pc - 2);
                        cycle[0] = 9;
                        break;
                    case 3: // d8(PC,IX)
                        if ((support & (1 << 10)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo.append("$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        val = mem.peekB(ptr);
                        cycle[0] = 10;
                        break;
                    case 4: // #Imm
                        if ((support & (1 << 11)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        val = (byte) fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("#$%02x".formatted(val));
//#endif

                        cycle[0] = 11;
                        break;
                }
                break;
        }

        return val & 0xff;
    }

    private int srcAddressingWord(/* ref */ StringBuilder nimo, /* ref */ int[] cycle, int sm, int sr, int support /* = 0xfff */, boolean nimoSw /* = true */) {
        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        short val = 0;

        cycle[0] = sm;
        switch (sm) {
            case 0: // Dn
                if ((support & (1 << 0)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("D%d".formatted(sr));
//#endif

                val = (short) reg.getD()[sr];
                break;
            case 1: // An
                if ((support & (1 << 1)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("A%s".formatted(sr));
//#endif

                val = (short) reg.getA().get(sr);
                break;
            case 2: // (An)
                if ((support & (1 << 2)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)".formatted(sr));
//#endif

                val = mem.peekW(reg.getA().get(sr));
                break;
            case 3: // (An)+
                if ((support & (1 << 3)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)+".formatted(sr));
//#endif

                val = mem.peekW(reg.getA().get(sr));
                reg.getA().set(sr, reg.getA().get(sr) + 2);
                break;
            case 4: // -(An)
                if ((support & (1 << 4)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("-(A%d)".formatted(sr));
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 2);
                val = mem.peekW(reg.getA().get(sr));
                break;
            case 5: // d16(An)
                if ((support & (1 << 5)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("$%04x(A%d)".formatted(d16, sr));
//#endif

                val = mem.peekW(reg.getA().get(sr) + d16);
                break;
            case 6: // d8(An,IX)
                if ((support & (1 << 6)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) if (nimoSw)
                    nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                val = mem.peekW(ptr);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        if ((support & (1 << 7)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%04x".formatted(ptr & 0xffff));
//#endif

                        val = mem.peekW(ptr);
                        break;
                    case 1: // Abs.L
                        if ((support & (1 << 8)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%08x".formatted(ptr));
//#endif

                        val = mem.peekW(ptr);
                        cycle[0] = 8;
                        break;
                    case 2: // d16(PC)
                        if ((support & (1 << 9)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%04x(PC)".formatted(ptr & 0xffff));
//#endif

                        val = mem.peekW(ptr + reg.pc - 2);
                        cycle[0] = 9;
                        break;
                    case 3: // d8(PC,IX)
                        if ((support & (1 << 10)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) nimo.append("$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        val = mem.peekW(ptr);
                        cycle[0] = 10;
                        break;
                    case 4: // #Imm
                        if ((support & (1 << 11)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        val = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("#$%04x".formatted(val));
//#endif

                        cycle[0] = 11;
                        break;
                }
                break;
        }

        return val & 0xffff;
    }

    private int srcAddressingLong(/* ref */ StringBuilder nimo, /* ref */ int[] cycle, int sm, int sr, int support /* = 0xfff */, boolean nimoSw /* = true */, int shift /* = 0 */) {
        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        cycle[0] = sm;
        switch (sm) {
            case 0: // Dn
                if ((support & (1 << 0)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("D%d".formatted(sr));
//#endif

                val = reg.getD()[sr];
                break;
            case 1: // An
                if ((support & (1 << 1)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("A%s".formatted(sr));
//#endif

                val = reg.getA().get(sr);
                break;
            case 2: // (An)
                if ((support & (1 << 2)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)".formatted(sr));
//#endif

                val = mem.peekL(reg.getA().get(sr) + shift);
                break;
            case 3: // (An)+
                if ((support & (1 << 3)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("(A%d)+".formatted(sr));
//#endif

                val = mem.peekL(reg.getA().get(sr));
                reg.getA().set(sr, reg.getA().get(sr) + 4);
                break;
            case 4: // -(An)
                if ((support & (1 << 4)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("-(A%d)".formatted(sr));
//#endif

                reg.getA().set(sr, reg.getA().get(sr) - 4);
                val = mem.peekL(reg.getA().get(sr));
                break;
            case 5: // d16(An)
                if ((support & (1 << 5)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE && nimoSw) nimo.append("$%04x(A%d)".formatted(d16, sr));
//#endif

                val = mem.peekL(reg.getA().get(sr) + d16 + shift);
                break;
            case 6: // d8(An,IX)
                if ((support & (1 << 6)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) if (nimoSw)
                    nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                val = mem.peekL(ptr + shift);
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        if ((support & (1 << 7)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("($%04x)".formatted(ptr & 0xffff));
//#endif

                        val = mem.peekL(ptr + shift);
                        break;
                    case 1: // Abs.L
                        if ((support & (1 << 8)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("($%08x)".formatted(ptr));
//#endif

                        val = mem.peekL(ptr + shift);
                        cycle[0] = 8;
                        break;
                    case 2: // d16(PC)
                        if ((support & (1 << 9)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("$%04x(PC)".formatted(ptr & 0xffff));
//#endif

                        val = mem.peekL(ptr + reg.pc - 2 + shift);
                        cycle[0] = 9;
                        break;
                    case 3: // d8(PC,IX)
                        if ((support & (1 << 10)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;
//#if DEBUG
                        if (TRACE) if (nimoSw)
                            nimo.append("$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }
                        val = mem.peekL(ptr + shift);
                        cycle[0] = 10;
                        break;
                    case 4: // #Imm
                        if ((support & (1 << 11)) == 0) throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
                        val = fetchL();
//#if DEBUG
                        if (TRACE && nimoSw) nimo.append("#$%08x".formatted(val));
//#endif

                        cycle[0] = 11;
                        break;
                }
                break;
        }

        return val;
    }

    private int srcAddressingLongLea(/* ref */ StringBuilder nimo, /* ref */ int[] cycle, int sm, int sr) {
        short vw;
        boolean isA;
        int ni;
        boolean isL;
        int IX;
        int ptr;
        int val = 0;

        cycle[0] = sm;
        switch (sm) {
            case 0:
            case 1:
            case 3:
            case 4:
                throw new UnsupportedOperationException("LEA Invalid addressing mode %04x".formatted(sm));
            case 2: // (An)
//#if DEBUG
                if (TRACE) nimo.append("(A%d)".formatted(sr));
//#endif

                val = reg.getA().get(sr);
                break;
            case 5: // d16(An)
                short d16 = fetchW(); // signed
//#if DEBUG
                if (TRACE) nimo.append("$%04x(A%d)".formatted(d16, sr));
//#endif

                val = reg.getA().get(sr) + d16;
                break;
            case 6: // d8(An,IX)
                vw = fetchW();
                isA = (vw & 0x8000) != 0;
                ni = (vw & 0x7000) >> 12;
                isL = (vw & 0x0800) != 0;
                IX = (isA ? reg.getA().get(ni) : reg.getD()[ni]);
//#if DEBUG
                if (TRACE) nimo.append("$%02x(A%d,%s%d.%s)".formatted(vw & 0xff, sr, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                if (!isL) ptr = reg.getA().get(sr) + (byte) vw + (short) (IX & 0xffff);
                else ptr = reg.getA().get(sr) + (byte) vw + IX;
                val = ptr;
                break;
            case 7: // etc.
                switch (sr) {
                    case 0: // Abs.W
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("$%04x".formatted(ptr & 0xffff));
//#endif

                        val = ptr;
                        break;
                    case 1: // Abs.L
                        ptr = fetchL();
//#if DEBUG
                        if (TRACE) nimo.append("$%08x".formatted(ptr));
//#endif

                        val = ptr;
                        cycle[0] = 8;
                        break;
                    case 2: // d16(PC)
                        ptr = fetchW();
//#if DEBUG
                        if (TRACE) nimo.append("$%04x(PC)".formatted(ptr & 0xffff));
//#endif

                        val = ptr + reg.pc - 2;
                        cycle[0] = 9;
                        break;
                    case 3: // d8(PC,IX)
                        vw = fetchW();
                        isA = (vw & 0x8000) != 0;
                        ni = (vw & 0x7000) >> 12;
                        isL = (vw & 0x0800) != 0;

//#if DEBUG
                        if (TRACE) nimo.append("$%02x(PC,%s%s.%s)".formatted(vw & 0xff, isA ? "A" : "D", ni, isL ? "l" : "w"));
//#endif

                        if (isL) {
                            IX = (isA ? reg.getAl(ni) : reg.getDl(ni));
                            ptr = reg.pc + (byte) vw + IX - 2;
                        } else {
                            IX = (isA ? reg.getAw(ni) : reg.getDw(ni));
                            ptr = reg.pc + (byte) vw + (short) (IX & 0xffff) - 2;
                        }

                        val = ptr;
                        cycle[0] = 10;
                        break;
                    default:
                        throw new UnsupportedOperationException("LEA Unknown mode %04x".formatted(sm));
                }
                break;
        }

        return val;
    }

    private void pushSSP(int val) {
        reg.setSSP(reg.getSSP() - 4);
        mem.pokeL(reg.getSSP(), val);
    }

    private void pushSSPw(short val) {
        reg.setSSP(reg.getSSP() - 2);
        mem.pokeW(reg.getSSP(), val);
    }

    private void pushUSP(int val) {
        reg.setUSP(reg.getUSP() - 4);
        mem.pokeL(reg.getUSP(), val);
    }

    private void push(int val) {
        reg.getA().set(7, reg.getA().get(7) - 4);
        mem.pokeL(reg.getA().get(7), val);
    }

    private int pop() {
        int val = mem.peekL(reg.getA().get(7));
        reg.getA().set(7, reg.getA().get(7) + 4);
        return val;
    }

    private short popw() {
        short val = mem.peekW(reg.getA().get(7));
        reg.getA().set(7, reg.getA().get(7) + 2);
        return val;
    }

    private boolean getCond(int cnd, /* out */ String[] cs) {
        return switch (cnd) {
            case 0 -> {
                cs[0] = "t";
                yield true;
            }
            case 1 -> {
                cs[0] = "f";
                yield false;
            }
            case 2 -> {
                cs[0] = "hi";
                yield (!reg.getC() && !reg.getZ());
            }
            case 3 -> {
                cs[0] = "ls";
                yield (reg.getC() || reg.getZ());
            }
            case 4 -> {
                cs[0] = "cc";
                yield !reg.getC();
            }
            case 5 -> {
                cs[0] = "cs";
                yield reg.getC();
            }
            case 6 -> {
                cs[0] = "ne";
                yield !reg.getZ();
            }
            case 7 -> {
                cs[0] = "eq";
                yield reg.getZ();
            }
            case 8 -> {
                cs[0] = "vc";
                yield !reg.getV();
            }
            case 9 -> {
                cs[0] = "vs";
                yield reg.getV();
            }
            case 0xa -> {
                cs[0] = "pl";
                yield !reg.getN();
            }
            case 0xb -> {
                cs[0] = "mi";
                yield reg.getN();
            }
            case 0xc -> {
                cs[0] = "ge";
                yield (reg.getN() && reg.getV()) || (!reg.getN() && !reg.getV());
            }
            case 0xd -> {
                cs[0] = "lt";
                yield (reg.getN() && !reg.getV()) || (!reg.getN() && reg.getV());
            }
            case 0xe -> {
                cs[0] = "gt";
                yield !reg.getZ() && ((reg.getN() && reg.getV()) || (!reg.getN() && !reg.getV()));
            }
            case 0xf -> {
                cs[0] = "le";
                yield reg.getZ() || (reg.getN() && !reg.getV()) || (!reg.getN() && reg.getV());
            }
            default ->
                    throw new UnsupportedOperationException("Not implemented at PC: %08x, opcode: %04x".formatted(reg.pc - 2, mem.peekW(reg.pc - 2) & 0xffff));
        };
    }
}
