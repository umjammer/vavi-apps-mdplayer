/*
 * MIT License
 *
 * Copyright (c) 2024 SingleStepTests
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mdplayer.driver.zms.nise68;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * M68kHarteTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-07-21 nsano initial version <br>
 * @see "https://github.com/SingleStepTests/m68000"
 * @see "https://claude.ai/chat/8a7435fe-00b0-4a5e-89f2-d2aae2c5e4e2"
 * @see "https://github.com/cdunku/m65xx/blob/main/src/main.c"
 * @see "https://github.com/MicroCoreLabs/Projects/tree/master/MCL68/MC68000_Test_Code"
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
class M68kHarteTest {

    private static int PRINTED_YET = 0;
    private static final String M68K_JSON_PATH = "tmp/harte/m68k/v1";

    private static final String[] REG_ORDER = {
            "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "usp",
            "ssp", "sr", "pc"
    };

    static class NameResult {
        final int ptr;
        final String name;

        NameResult(int ptr, String name) {
            this.ptr = ptr;
            this.name = name;
        }
    }

    static class TransactionResult {
        final int ptr;
        final List<Object> transactions;
        final int numCycles;

        TransactionResult(int ptr, List<Object> transactions, int numCycles) {
            this.ptr = ptr;
            this.transactions = transactions;
            this.numCycles = numCycles;
        }
    }

    static class StateResult {
        final int ptr;
        final Map<String, Object> state;

        StateResult(int ptr, Map<String, Object> state) {
            this.ptr = ptr;
            this.state = state;
        }
    }

    static class TestResult {
        final int ptr;
        final Map<String, Object> test;

        TestResult(int ptr, Map<String, Object> test) {
            this.ptr = ptr;
            this.test = test;
        }
    }

    private static int readIntLE(byte[] content, int offset) {
        return ByteBuffer.wrap(content, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static short readShortLE(byte[] content, int offset) {
        return ByteBuffer.wrap(content, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private static byte readByte(byte[] content, int offset) {
        return content[offset];
    }

    private static NameResult readName(byte[] content, int ptr) {
        int numbytes = readIntLE(content, ptr);
        int magicNum = readIntLE(content, ptr + 4);
        ptr += 8;

        if (magicNum != 0x89ab_cdef) {
            throw new RuntimeException("Magic number assertion failed");
        }

        int strlen = readIntLE(content, ptr);
        ptr += 4;

        String nstr = new String(content, ptr, strlen, java.nio.charset.StandardCharsets.UTF_8);
        ptr += strlen;

        return new NameResult(ptr, nstr);
    }

    private static TransactionResult readTransactions(byte[] content, int ptr) {
        int numbytes = readIntLE(content, ptr);
        int magicNum = readIntLE(content, ptr + 4);

        if (magicNum != 0x4567_89ab) {
            throw new RuntimeException("Magic number assertion failed");
        }

        ptr += 8;
        List<Object> transactions = new ArrayList<>();

        int numCycles = readIntLE(content, ptr);
        int numTransactions = readIntLE(content, ptr + 4);

        if (numCycles == 0) {
            PRINTED_YET++;
        }

        ptr += 8;

        for (int i = 0; i < numTransactions; i++) {
            byte tw = readByte(content, ptr);
            int cycles = readIntLE(content, ptr + 1);
            ptr += 5;

            if (tw == 0) {
                transactions.add(Arrays.asList("n", cycles));
                continue;
            }

            int fc = readIntLE(content, ptr);
            int addrBus = readIntLE(content, ptr + 4);
            int dataBus = readIntLE(content, ptr + 8);
            int UDS = readIntLE(content, ptr + 12);
            int LDS = readIntLE(content, ptr + 16);
            int bw = UDS + LDS;
            ptr += 20;

            String tws = switch (tw) {
                case 1 -> // write
                        "w";
                case 2 -> // read
                        "r";
                case 3 -> // TAS cycle
                        "t";
                case 4 -> // read address error (no AS assert)
                        "re";
                case 5 -> // write address error (no AS assert)
                        "we";
                default -> throw new RuntimeException("BAD KIND");
            };

            transactions.add(Arrays.asList(
                    tws, cycles, fc, addrBus,
                    (bw == 2) ? ".w" : ".b",
                    dataBus, UDS, LDS
            ));
        }

        return new TransactionResult(ptr, transactions, numCycles);
    }

    private static StateResult readState(byte[] content, int ptr) {
        Map<String, Object> st = new HashMap<>();

        int numbytes = readIntLE(content, ptr);
        int magicNum = readIntLE(content, ptr + 4);
        ptr += 8;

        if (magicNum != 0x0123_4567) {
            throw new RuntimeException("Magic number assertion failed");
        }

        for (String reg : REG_ORDER) {
            st.put(reg, readIntLE(content, ptr));
            ptr += 4;
        }

        int pf0 = readIntLE(content, ptr);
        int pf1 = readIntLE(content, ptr + 4);
        ptr += 8;
        st.put("prefetch", Arrays.asList(pf0, pf1));

        // RAM 6-byte values
        int numRams = readIntLE(content, ptr);
        ptr += 4;

        List<List<Integer>> ram = new ArrayList<>();

        for (int i = 0; i < numRams; i++) {
            int addr = readIntLE(content, ptr);
            short data = readShortLE(content, ptr + 4);
            ptr += 6;

            if (addr >= 0x100_0000) {
                throw new RuntimeException("Address assertion failed");
            }

            ram.add(Arrays.asList(addr, (data >> 8) & 0xFF));
            ram.add(Arrays.asList(addr | 1, data & 0xFF));
        }

        st.put("ram", ram);

        return new StateResult(ptr, st);
    }

    private static TestResult decodeTest(byte[] content, int ptr) {
        Map<String, Object> test = new HashMap<>();

        int numbytes = readIntLE(content, ptr);
        int magicNum = readIntLE(content, ptr + 4);

        if (magicNum != 0xabc1_2367) {
            throw new RuntimeException("Magic number assertion failed");
        }

        ptr += 8;

        NameResult nameResult = readName(content, ptr);
        ptr = nameResult.ptr;
        test.put("name", nameResult.name);

        StateResult initialResult = readState(content, ptr);
        ptr = initialResult.ptr;
        test.put("initial", initialResult.state);

        StateResult finalResult = readState(content, ptr);
        ptr = finalResult.ptr;
        test.put("final", finalResult.state);

        TransactionResult transResult = readTransactions(content, ptr);
        ptr = transResult.ptr;
        test.put("transactions", transResult.transactions);
        test.put("length", transResult.numCycles);

        return new TestResult(ptr, test);
    }

    private static void decodeFile(String infilename, String outfilename) throws IOException {
        System.out.println("DECODE " + infilename);

        byte[] content = Files.readAllBytes(Paths.get(infilename));
        int ptr = 0;

        int magicNum = readIntLE(content, ptr);
        int numTests = readIntLE(content, ptr + 4);

        if (magicNum != 0x1a3f_5d71) {
            throw new RuntimeException("Magic number assertion failed");
        }

        ptr += 8;
        List<Map<String, Object>> tests = new ArrayList<>();

        PRINTED_YET = 0;
        for (int i = 0; i < numTests; i++) {
            TestResult testResult = decodeTest(content, ptr);
            ptr = testResult.ptr;
            tests.add(testResult.test);
        }

        if (PRINTED_YET > 0) {
            System.out.println("NUM WITH NO CYCLES: " + PRINTED_YET);
        }

        // Delete output file if it exists
        Path out = Paths.get(outfilename);
        if (!Files.exists(out.getParent())) Files.createDirectory(out.getParent());
        Files.deleteIfExists(out);

        // Write JSON output
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(tests);

        try (FileWriter writer = new FileWriter(outfilename)) {
            writer.write(json);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void test1() throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(M68K_JSON_PATH), "*.json.bin")) {

            for (Path entry : stream) {
                String fname = entry.getFileName().toString();
                String outname = fname.substring(0, fname.length() - 4); // Remove .bin
                decodeFile(entry.toString(), "tmp/harte/m68k/json/" + outname);
            }
        }
    }
}