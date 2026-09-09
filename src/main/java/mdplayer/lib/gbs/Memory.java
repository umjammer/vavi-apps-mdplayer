package mdplayer.lib.gbs;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;


public class Memory {

    private static final Logger logger = System.getLogger(Memory.class.getName());

    private final byte[][] cartROM;
    private final List<byte[]> vRAM;
    private final List<byte[]> exRAM;
    private final List<byte[]> wkRAM;
    private final byte[] spriteAtrTbl;
    private final byte[] hiRAM;
    private int cartROMBank;
    private final int vRAMBank;
    private final int exRAMBank;
    private final int wkRAMBank;
    private byte ier;

    private final IO io;

    public static Memory Empty = new Memory(new byte[1][], null);

    public Memory(byte[][] memoryArray, IO io) {
        cartROM = memoryArray;
        this.io = io;

        cartROMBank = 1;
        vRAMBank = 0;
        exRAMBank = 0;
        wkRAMBank = 1;

        vRAM = new ArrayList<>();
        vRAM.add(new byte[0x2000]);

        exRAM = new ArrayList<>();
        exRAM.add(new byte[0x2000]);

        wkRAM = new ArrayList<>();
        wkRAM.add(new byte[0x1000]);
        wkRAM.add(new byte[0x1000]);

        spriteAtrTbl = new byte[0xa0];
        hiRAM = new byte[0x80];

        ier = 0;
    }

    public byte peekB(int pc) {
        pc = (pc & 0xFFFF);

        if (pc >= 0x0000 && pc < 0x4000) {
            return cartROM[0][(pc - 0x0000) & 0xFFFF];
        } else if (pc >= 0x4000 && pc < 0x8000) {
            return cartROM[cartROMBank][(pc - 0x4000) & 0xFFFF];
        } else if (pc >= 0x8000 && pc < 0xa000) {
            return vRAM.get(vRAMBank)[(pc - 0x8000) & 0xFFFF];
        } else if (pc >= 0xa000 && pc < 0xc000) {
            return exRAM.get(exRAMBank)[(pc - 0xa000) & 0xFFFF];
        } else if (pc >= 0xc000 && pc < 0xd000) {
            return wkRAM.getFirst()[(pc - 0xc000) & 0xFFFF];
        } else if (pc >= 0xd000 && pc < 0xe000) {
            return wkRAM.get(wkRAMBank)[(pc - 0xd000) & 0xFFFF];
        } else if (pc >= 0xe000 && pc < 0xf000) {
            return wkRAM.getFirst()[(pc - 0xe000) & 0xFFFF];
        } else if (pc >= 0xf000 && pc < 0xfe00) {
            return wkRAM.get(wkRAMBank)[(pc - 0xf000) & 0xFFFF];
        } else if (pc >= 0xfe00 && pc < 0xfea0) {
            return spriteAtrTbl[(pc - 0xfe00) & 0xFFFF];
        } else if (pc >= 0xfea0 && pc < 0xff00) {
            return (byte) 0xff; // Not use area
        } else if (pc >= 0xff00 && pc < 0xff80) {
            return (byte) io.read(pc);
        } else if (pc >= 0xff80 && pc < 0xffff) {
            return hiRAM[(pc - 0xff80) & 0xFFFF];
        } else if (pc == 0xffff) {
            return ier; // Interrupt Enable Register
        } else {
            throw new IllegalArgumentException("Not written to RAM area.");
        }
    }

    public void pokeB(int pc, byte dat) {
        if (pc >= 0x2000 && pc < 0x4000) {
            cartROMBank = dat & 0xFF;
            if (cartROMBank >= cartROM.length) throw new IllegalArgumentException("Switching to a non-existent Bank.");
        } else if ((pc >= 0x4000 && pc < 0x6000) || pc == 0xff70) {
            // Ignore
        } else if (pc >= 0x8000 && pc < 0xa000) {
            vRAM.get(vRAMBank)[(pc - 0x8000) & 0xFFFF] = dat;
        } else if (pc >= 0xa000 && pc < 0xc000) {
            exRAM.get(exRAMBank)[(pc - 0xa000) & 0xFFFF] = dat;
        } else if (pc >= 0xc000 && pc < 0xd000) {
            wkRAM.getFirst()[(pc - 0xc000) & 0xFFFF] = dat;
        } else if (pc >= 0xd000 && pc < 0xe000) {
            wkRAM.get(wkRAMBank)[(pc - 0xd000) & 0xFFFF] = dat;
        } else if (pc >= 0xe000 && pc < 0xf000) {
            wkRAM.getFirst()[(pc - 0xe000) & 0xFFFF] = dat;
        } else if (pc >= 0xf000 && pc < 0xfe00) {
            wkRAM.get(wkRAMBank)[(pc - 0xf000) & 0xFFFF] = dat;
        } else if (pc >= 0xfe00 && pc < 0xfea0) {
            spriteAtrTbl[(pc - 0xfe00) & 0xFFFF] = dat;
        } else if (pc >= 0xfea0 && pc < 0xff00) {
            //Not use
        } else if (pc >= 0xff00 && pc < 0xff80) {
            io.write(pc, dat & 0xff);
        } else if (pc >= 0xff80 && pc < 0xffff) {
            hiRAM[(pc - 0xff80) & 0xFFFF] = dat;
        } else if (pc == 0xffff) {
            ier = dat; //Interrupt Enable Register
        } else {
            logger.log(Level.WARNING, "Written to ROM area $%04X.".formatted(pc));
        }
    }

    public int peekW(int pc) { // ushort in C# -> int in Java
        return (peekB(pc) & 0xFF) + ((peekB(pc + 1) & 0xFF) << 8);
    }

    public void pokeW(int pc, int dat) {
        pokeB(pc, (byte) dat);
        pokeB(pc + 1, (byte) ((dat >> 8) & 0xFF));
    }

    public String getBank() {
        return "bnk %d:%d:%d:%d".formatted(cartROMBank, vRAMBank, exRAMBank, wkRAMBank);
    }
}
