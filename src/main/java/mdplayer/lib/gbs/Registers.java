package mdplayer.lib.gbs;


public class Registers {

    public byte a, b, c, d, e, f, h, l;
    public int sp, pc;

    public int getAf() {
        return ((a & 0xFF) << 8) | (f & 0xFF);
    }

    public void setAf(int value) {
        a = (byte) ((value >> 8) & 0xFF);
        f = (byte) (value & 0xFF);
    }

    public int getBc() {
        return ((b & 0xFF) << 8) | (c & 0xFF);
    }

    public void setBc(int value) {
        b = (byte) ((value >> 8) & 0xFF);
        c = (byte) (value & 0xFF);
    }

    public int getDe() {
        return ((d & 0xFF) << 8) | (e & 0xFF);
    }

    public void setDe(int value) {
        d = (byte) ((value >> 8) & 0xFF);
        e = (byte) (value & 0xFF);
    }

    public int getHl() {
        return ((h & 0xFF) << 8) | (l & 0xFF);
    }

    public void setHl(int value) {
        h = (byte) ((value >> 8) & 0xFF);
        l = (byte) (value & 0xFF);
    }

    public boolean isZ() {
        return (f & 0x80) != 0;
    }

    public void setZ(boolean value) {
        f &= 0x7f;
        f |= (byte) (value ? 0x80 : 0);
    }

    public boolean isS() {
        return (f & 0x40) != 0;
    }

    public void setS(boolean value) {
        f &= 0xbf;
        f |= (byte) (value ? 0x40 : 0);
    }

    public boolean isH() {
        return (f & 0x20) != 0;
    }

    public void setH(boolean value) {
        f &= 0xdf;
        f |= (byte) (value ? 0x20 : 0);
    }

    public boolean isC() {
        return (f & 0x10) != 0;
    }

    public void setC(boolean value) {
        f &= 0xef;
        f |= (byte) (value ? 0x10 : 0);
    }

    @Override
    public String toString() {
        String n = "A%02x B%02x C%02x D%02x E%02x F%02x H%02x L%02x SP%04x PC%04x Flg%s%s%s%s".formatted(
                a & 0xFF, b & 0xFF, c & 0xFF, d & 0xFF, e & 0xFF, f & 0xFF, h & 0xFF, l & 0xFF,
                sp & 0xFFFF, pc & 0xFFFF,
                isZ() ? "Z" : "-", isS() ? "S" : "-", isH() ? "H" : "-", isC() ? "C" : "-");
        return n;
    }
}
