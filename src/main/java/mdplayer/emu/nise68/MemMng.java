package mdplayer.emu.nise68;

import java.util.Map;
import java.util.TreeMap;


/**
 * Human68k memory blocks: first fit from the heap start up to {@link #LIMIT}, freed blocks are reused.
 * <p>
 * X68000 tools free and re-malloc large work buffers per item (ZPCNV3 per tone); a bump allocator
 * runs off the end of main memory, and past 16MB addresses wrap onto the I/O hooks.
 */
public class MemMng {

    /** block start -> size */
    private final TreeMap<Integer, Integer> blocks = new TreeMap<>();
    /** where the heap starts */
    private final int base;
    int address = 0x2000;

    public int getAddress() {
        return address;
    }

    public int getAllocCount() {
        return blocks.size();
    }

    public MemMng(int startAdr) {
        base = align(startAdr);
        address = 0x2000;
    }

    private static final int bl = 4;

    /** end of main memory on a fully expanded X68000 (12MB); GVRAM and I/O start here */
    public static final int LIMIT = 0x00c0_0000;

    private static int align(int v) {
        return v % bl != 0 ? v + bl - (v % bl) : v;
    }

    public boolean set(int memPtr, int size) {
        if (blocks.containsKey(memPtr)) return false;

        blocks.put(memPtr, size);
        return true;
    }

    public boolean Change(int newPtr, int newlen) {
        if (!blocks.containsKey(newPtr)) return false;
        if (newlen > room(newPtr)) return false;
        blocks.put(newPtr, newlen);
        return true;
    }

    /** @return how large the block at {@code ptr} may grow before it hits the next block or {@link #LIMIT} */
    public int room(int ptr) {
        Integer next = blocks.higherKey(ptr);
        return (next != null ? next : Math.max(LIMIT, ptr)) - ptr;
    }

    public boolean contains(int ptr) {
        return blocks.containsKey(ptr);
    }

    /** @return the first address at or above {@link #base} where {@code size} bytes fit, or -1 */
    private int findFit(int size) {
        int cur = base;
        for (Map.Entry<Integer, Integer> e : blocks.entrySet()) {
            int start = e.getKey();
            int end = align(start + e.getValue());
            if (end <= cur) continue;
            if (start >= cur && start - cur >= size) return cur;
            cur = Math.max(cur, end);
        }
        return LIMIT - cur >= size ? cur : -1;
    }

    /** @return the largest block {@link #malloc} can still hand out */
    public int available() {
        int cur = base, max = 0;
        for (Map.Entry<Integer, Integer> e : blocks.entrySet()) {
            int start = e.getKey();
            int end = align(start + e.getValue());
            if (end <= cur) continue;
            if (start > cur) max = Math.max(max, start - cur);
            cur = Math.max(cur, end);
        }
        return Math.max(max, LIMIT - cur);
    }

    /** @return the block address, or -1 when there is no room below {@link #LIMIT} */
    public int malloc(int byteSize) {
        if (byteSize < 0) return -1;
        int size = align(byteSize);
        int ret = findFit(size);
        if (ret < 0) return -1;
        blocks.put(ret, size);
        return ret;
    }

    public int mfree(int ptr) {
        return blocks.remove(ptr) != null ? 0 : -1;
    }
}
