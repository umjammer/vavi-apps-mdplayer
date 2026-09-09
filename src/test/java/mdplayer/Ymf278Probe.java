/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.chips.YmF278BChip;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/** Headless probe of the raw YMF278B register cache. Not a regression test. */
class Ymf278Probe {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void dump() throws Exception {
        String file = System.getProperty("diag.file");
        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        @SuppressWarnings("unchecked")
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file));
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;

        YmF278BChip chip = plugin.chipRegister.chip(YmF278BChip.class);
        System.err.println("chip=" + chip + " register[0]=" + (chip != null ? chip.register[0] : null));

        short[] buffer = new short[1024];
        for (int block = 0; block < 4; block++) {
            for (int i = 0; i < 44100 * 2 / buffer.length; i++) {
                plugin.getDriver().render(buffer, 0, buffer.length);
            }
            int[][] regs = chip.register[0];
            System.err.println("--- block " + block);
            for (int port = 0; port < regs.length; port++) {
                int nonZero = 0;
                for (int v : regs[port]) if (v != 0) nonZero++;
                System.err.println("  port " + port + " nonzero=" + nonZero);
            }
            // the wave slot registers, snum = (reg - 8) % 24: base 4 at 0x68 carries key on and pan
            for (int base = 0; base < 6; base++) {
                StringBuilder sb = new StringBuilder("  base " + base + " (0x"
                        + Integer.toHexString(8 + base * 24) + "): ");
                for (int ch = 0; ch < 24; ch++) {
                    sb.append("%02x ".formatted(regs[2][8 + base * 24 + ch]));
                }
                System.err.println(sb);
            }
            StringBuilder sb = new StringBuilder("  keyon: ");
            for (int ch = 0; ch < 24; ch++) {
                if ((regs[2][0x68 + ch] & 0x80) != 0) sb.append(ch).append(' ');
            }
            System.err.println(sb);
            // the FM part: 0xb0..0xb8 of bank 0
            sb = new StringBuilder("  fm 0xb0..0xb8: ");
            for (int ch = 0; ch < 9; ch++) sb.append(Integer.toHexString(regs[0][0xb0 + ch])).append(' ');
            System.err.println(sb);
        }
    }
}
