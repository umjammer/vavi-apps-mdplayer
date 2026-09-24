package mdplayer.lib.vgm;

import mdplayer.lib.vgm.Vgm.IVgm;


class DacControl implements Vgm.IDac {

    private static final int MAX_CHIPS = 0xff;
    private final DacControl_[] DACData = new DacControl_[MAX_CHIPS];
    private final IVgm ivgm;

    DacControl(IVgm ivgm) {
        this.ivgm = ivgm;
    }

    @Override
    public void update(int chipId, int samples) {
//#if DEBUG
        if (!ivgm.isVirtual()) return;
//#endif
        DacControl_ chip = DACData[chipId];
        chip.update(samples);
    }

    @Override
    public int deviceStart(int chipId) {
        if ((chipId & 0xff) >= MAX_CHIPS)
            return 0;

        DacControl_ chip = DACData[chipId];
        chip.startDacControl();

        return 1;
    }

    public void deviceStop(int chipId) {
        DacControl_ chip = DACData[chipId];
        chip.stopDacControl();
    }

    @Override
    public void deviceReset(int chipId) {
        DacControl_ chip = DACData[chipId];
        chip.reset();
    }

    @Override
    public void setupChip(int chipId, int chType, int chNum, int command) {
        DacControl_ chip = DACData[chipId];
        chip.setup(chType, chNum, command);
    }

    @Override
    public void setData(int chipId, byte[] data, int dataLen, int stepSize, int stepBase) {
        DacControl_ chip = DACData[chipId];
        chip.setData(data, dataLen, stepSize, stepBase);
    }

    @Override
    public void refreshData(int chipId, byte[] data, int dataLen) {
        // Should be called to fix the data pointer. (e.g. after a realloc)
        DacControl_ chip = DACData[chipId];
        chip.refreshData(data, dataLen);
    }

    @Override
    public void setFrequency(int chipId, int frequency) {
        //logger.log(Level.TRACE, "chipId%d frequency%d".formatted(chipId, frequency));
        DacControl_ chip = DACData[chipId];
        chip.setFrequency(chipId, frequency);
    }

    @Override
    public void start(int chipId, int dataPos, int lenMode, int length) {
        DacControl_ chip = DACData[chipId];
        chip.start(dataPos, lenMode, length);
    }

    @Override
    public void stop(int chipId) {
        DacControl_ chip = DACData[chipId];
        chip.stop();
    }

    @Override
    public void refresh() {
        for (int i = 0; i < MAX_CHIPS; i++) DACData[i] = new DacControl_();
    }

    private void writeChipReg(int chipType2, int chipId, int port, int offset, int data) {
        switch (chipType2) {
        case 0x00: // SN76489
            ivgm.writeSn76489(chipId, data);
            break;
        case 0x01: // YM2413+
            ivgm.writeYm2413(chipId, offset, data);
            break;
        case 0x02: // Ym2612
            ivgm.writeYm2612(chipId, port, offset, data, -1);
            break;
        case 0x03: // YM2151+
            ivgm.writeYm2151(chipId, port, offset, data, 0, 0);
            break;
        case 0x06: // YM2203+
            ivgm.writeYm2203(chipId, offset, data);
            break;
        case 0x07: // YM2608+
            ivgm.writeYm2608(chipId, port, offset, data);
            break;
        case 0x08: // YM2610+
            ivgm.writeYm2610(chipId, port, offset, data);
            break;
        case 0x09: // YM3812+
            ivgm.writeYm3812(chipId, offset, data);
            break;
        case 0x0A: // YM3526+
            ivgm.writeYm3526(chipId, offset, data);
            break;
        case 0x0B: // Y8950+
            ivgm.writeY8950(chipId, offset, data);
            break;
        case 0x0C: // YMF262+
            ivgm.writeYmF262(chipId, port, offset, data);
            break;
        case 0x0D: // YMF278B+
            ivgm.writeYmF278B(chipId, port, offset, data);
            break;
        case 0x0E: // YMF271+
            ivgm.writeYmF271(chipId, port, offset, data);
            break;
        case 0x0F: // YMZ280B+
            ivgm.writeYmZ280B(chipId, offset, data);
            break;
        case 0x10:
            ivgm.writeRf5C164(chipId, offset, data);
            break;
        case 0x11: // PWM
            ivgm.writePwm(chipId, port, (offset << 8) | (data << 0));
            break;
        case 0x12: // AY8910+
            ivgm.writeAy8910(chipId, offset, data);
            break;
        case 0x13: // DMG+
            ivgm.writeDmg(chipId, offset, data);
            break;
        case 0x14: // NES+
            ivgm.writeNes(chipId, offset, data);
            break;
        case 0x16: // UPD7759
            ivgm.writeUpd7759(chipId, offset, data);
            break;
        case 0x17: // OKIM6258
            if (ivgm.isVirtual()) // logger.log(Level.TRACE, "[DAC]");
                ivgm.writeOkiM6258(chipId, offset, data);
            break;
        case 0x1b: // OotakeHuC6280
            ivgm.writeHuC6280(chipId, offset, data);
            break;
        case 0x29: // MSM5205, as the draft libvgm numbered it (now the Mikey's)
        case 0x2c: // MSM5205
            ivgm.writeMsm5205(chipId, offset, data & 0xff);
            break;
        case 0x2d: // MSM5232
            ivgm.writeMsm5232(chipId, offset, data & 0xff);
            break;
        }
    }

    class DacControl_ {

        private static final int DAC_SMPL_RATE = 44100; // DAC control's own sample rate (Fixed)

        private static int mulDiv64Round(int multiplicand, int multiplier, int divisor) {
            // Yes, I'm correctly rounding the values.
            return (multiplicand * multiplier + divisor / 2) / divisor;
        }

        // Commands sent to dest-chips
        int dstChipType2;
        int dstChipID;
        int dstCommand;
        int cmdSize;

        // Frequency (Hz) at which the commands are sent
        int frequency;
        // to protect from reading beyond End Of data
        int dataLen;
        byte[] data;
        // Position where to start
        int dataStart;
        // usually 1, set to 2 for L/R interleaved data
        int stepSize;
        // usually 0, set to 0/1 for L/R interleaved data
        int stepBase;
        int cmdsToSend;

        // Running Bits:	0 (01) - instanceof playing
        //					2 (04) - loop sample (simple loop from start to end)
        //					4 (10) - already sent this command
        //					7 (80) - disabled
        int running;
        int reverse;
        // Position : Player SampleRate
        int step;
        // Position : data SampleRate
        int pos;
        int remainCmds;
        // true Position : data (== Pos, if Reverse instanceof off)
        int realPos;
        // always stepSize * cmdSize
        int dataStep;

        void sendCommand() {
            int port;
            int command;
            int data;

            if ((running & 0x10) != 0)   // command already sent
                return;
            if (dataStart + realPos >= dataLen)
                return;

//            if (!chips -> Reverse) {
//                ChipData00 = chips.data[(chips.DataStart + chips.RealPos)];
//                ChipData01 = chips.data[(chips.DataStart + chips.RealPos + 1)];
//            } else
//                ChipData = chips -> data + (chips -> dataStart + chips -> CmdsToSend - 1 - chips -> Pos);
            switch (dstChipType2) {
            // Support for the important chips
            case 0x02: // Ym2612 (16-bit Register (actually 9 Bit), 8-bit data)
                port = (dstCommand & 0xff00) >> 8;
                command = dstCommand & 0x00ff;
                data = this.data[(dataStart + realPos)];
                //if (model == enmModel.RealModel) logger.log(Level.DEBUG, "%x %x".formatted(data, chips.RealPos)));

                writeChipReg(dstChipType2, dstChipID, port, command, data);
                break;
            case 0x11: // PWM (4-bit Register, 12-bit data)
                port = dstCommand & 0x000f;
                command = this.data[dataStart + realPos + 1] & 0x0F;
                data = this.data[dataStart + realPos];
                writeChipReg(dstChipType2, dstChipID, port, command, data);
                break;
            // Support for other chips (mainly for completeness)
            case 0x00: // SN76496 (4-bit Register, 4-bit/10-bit data)
                command = dstCommand & 0x00f0;
                data = this.data[dataStart + realPos] & 0x0F;
                if ((command & 0x10) != 0) {
                    // Volume Change (4-Bit value)
                    writeChipReg(dstChipType2, dstChipID, 0x00, 0x00, command | data);
                } else {
                    // Frequency Write (10-Bit value)
                    port = ((this.data[dataStart + realPos + 1] & 0x03) << 4) | ((this.data[dataStart + realPos] & 0xf0) >> 4);
                    writeChipReg(dstChipType2, dstChipID, 0x00, 0x00, command | data);
                    writeChipReg(dstChipType2, dstChipID, 0x00, 0x00, port);
                }
                break;
            case 0x18: // OKIM6295 - TODO: verify
                command = dstCommand & 0x00ff;
                data = this.data[dataStart + realPos];

                if (command == 0) {
                    port = (byte) ((dstCommand & 0x0F00) >> 8);
                    if ((data & 0x80) > 0) {
                        // Sample Start
                        // write sample ID
                        writeChipReg(dstChipType2, dstChipID, 0x00, command, data);
                        // write channel(s) that should play the sample
                        writeChipReg(dstChipType2, dstChipID, 0x00, command, port << 4);
                    } else {
                        // Sample Stop
                        writeChipReg(dstChipType2, dstChipID, 0x00, command, port << 3);
                    }
                } else {
                    writeChipReg(dstChipType2, dstChipID, 0x00, command, data);
                }
                break;
            // Generic support: 8-bit Register, 8-bit data
            case 0x01: // YM2413
            case 0x03: // YM2151
            case 0x06: // YM2203
            case 0x09: // YM3812
            case 0x0A: // YM3526
            case 0x0B: // Y8950
            case 0x0F: // YMZ280B
            case 0x12: // AY8910
            case 0x13: // GameBoy DMG
            case 0x14: // NES APU
    //    	case 0x15: // MultiPCM
            case 0x16: // UPD7759
            case 0x17: // OKIM6258
            case 0x1D: // K053260 - TODO: Verify
            case 0x1E: // Pokey - TODO: Verify
            case 0x29: // MSM5205 (draft)
            case 0x2C: // MSM5205
            case 0x2D: // MSM5232
                command = dstCommand & 0x00FF;
                data = this.data[dataStart + realPos];
                writeChipReg(dstChipType2, dstChipID, 0x00, command, data);
                break;
            // Generic support: 16-bit Register, 8-bit data
            case 0x07: // YM2608
            case 0x08: // YM2610/B
            case 0x0C: // YMF262
            case 0x0D: // YMF278B
            case 0x0E: // YMF271
            case 0x19: // K051649 - TODO: Verify
            case 0x1A: // K054539 - TODO: Verify
            case 0x1C: // C140 - TODO: Verify
                port = (dstCommand & 0xff00) >> 8;
                command = dstCommand & 0x00FF;
                data = this.data[dataStart + realPos];
                writeChipReg(dstChipType2, dstChipID, port, command, data);
                break;
            // Generic support: 8-bit Register with Channel Select, 8-bit data
            case 0x05: // RF5C68
            case 0x10: // RF5C164
            case 0x1B: // OotakeHuC6280
                port = (dstCommand & 0xff00) >> 8;
                command = dstCommand & 0x00FF;
                data = this.data[dataStart + realPos];

                if (port == 0xff) // Send Channel Select
                    writeChipReg(dstChipType2, dstChipID, 0x00, command & 0x0f, data);
                else {
                    int prevChn;

                    prevChn = port; // by default don't restore channel
                    // get current channel for supported chips
                    if (dstChipType2 == 0x05) {
                    } // TODO
                    else if (dstChipType2 == 0x05) {
                    } // TODO
                    else if (dstChipType2 == 0x1B)
                        prevChn = ivgm.readHuC6280(dstChipID, 0x00);

                    // Send Channel Select
                    writeChipReg(dstChipType2, dstChipID, 0x00, command >> 4, port);
                    // Send data
                    writeChipReg(dstChipType2, dstChipID, 0x00, command & 0x0F, data);
                    // restore old channel
                    if (prevChn != port)
                        writeChipReg(dstChipType2, dstChipID, 0x00, command >> 4, prevChn);

                    // Send data
                    writeChipReg(dstChipType2, dstChipID, 0x00, command & 0x0F, data);
                }
                break;
            // Generic support: 8-bit Register, 16-bit data
            case 0x1F: // QSound
                command = dstCommand & 0x00FF;
                writeChipReg(dstChipType2, dstChipID, this.data[dataStart + realPos] & 0xff, this.data[dataStart + realPos + 1] & 0xff, command);
                break;
            }
            running |= 0x10;
        }

        void update(int samples) {
            int newPos;
            int realDataStp;

//logger.log(Level.TRACE, "DAC update chipId%d samples%d this.Running%d".formatted(chipId, samples, this.Running));
            if ((this.running & 0x80) != 0) // disabled
                return;
            if ((this.running & 0x01) == 0) // stopped
                return;

            if (this.reverse == 0)
                realDataStp = this.dataStep;
            else
                realDataStp = -this.dataStep;

            if (samples > 0x20) {
                // very effective Speed Hack for fast seeking
                newPos = this.step + (samples - 0x10);
                newPos = mulDiv64Round(newPos * this.dataStep, this.frequency, DAC_SMPL_RATE);
                while (this.remainCmds != 0 && this.pos < newPos) {
                    this.pos += this.dataStep;
                    this.realPos = this.realPos + realDataStp;
                    this.remainCmds--;
                }
            }

            this.step += samples;
            // Formula: Step * Freq / SampleRate
            newPos = mulDiv64Round(this.step * this.dataStep, this.frequency, DAC_SMPL_RATE);
//logger.log(Level.TRACE, "newPos%d this.Step%d this.DataStep%d this.Frequency%d DAC_SMPL_RATE%d".formatted(newPos, this.Step, this.DataStep, this.Frequency, (int)setting.getoutputDevice().SampleRate));
            this.sendCommand();

            while (this.remainCmds != 0 && this.pos < newPos) {
                this.sendCommand();
                this.pos += this.dataStep;
//if (model == enmModel.RealModel) logger.log(Level.DEBUG, "datastep:%d".formatted(this.DataStep));
                this.realPos = this.realPos + realDataStp;
                this.running &= 0xef;// ~0x10;
                this.remainCmds--;
            }

            if (this.remainCmds == 0 && ((this.running & 0x04) != 0)) {
                // loop back to start
                this.remainCmds = this.cmdsToSend;
                this.step = 0x00;
                this.pos = 0x00;
                if (this.reverse == 0)
                    this.realPos = 0x00;
                else
                    this.realPos = (this.cmdsToSend - 0x01) * this.dataStep;
            }

            if (this.remainCmds == 0)
                this.running &= 0xfe; // stop
        }

        void startDacControl() {
            this.dstChipType2 = 0xff;
            this.dstChipID = 0x00;
            this.dstCommand = 0x0000;

            this.running = 0xff; // disable all actions (except setup_chip)
        }

        void stopDacControl() {
            this.running = 0xff;
        }

        void reset() {
            this.dstChipType2 = 0x00;
            this.dstChipID = 0x00;
            this.dstCommand = 0x00;
            this.cmdSize = 0x00;

            this.frequency = 0;
            this.dataLen = 0x00;
            this.data = null;
            this.dataStart = 0x00;
            this.stepSize = 0x00;
            this.stepBase = 0x00;

            this.running = 0x00;
            this.reverse = 0x00;
            this.step = 0x00;
            this.pos = 0x00;
            this.realPos = 0x00;
            this.remainCmds = 0x00;
            this.dataStep = 0x00;
        }

        void setup(int chType, int chNum, int command) {
            this.dstChipType2 = chType; // TypeID (e.g. 0x02 for Ym2612)
            this.dstChipID = chNum; // chips number (to send commands to 1st or 2nd chips)
            this.dstCommand = command; // Port and command (would be 0x02A for Ym2612)

            switch (this.dstChipType2) {
            case 0x00: // SN76496
                if ((this.dstCommand & 0x0010) > 0)
                    this.cmdSize = 0x01; // Volume Write
                else
                    this.cmdSize = 0x02; // Frequency Write
                break;
            case 0x02: // Ym2612
                this.cmdSize = 0x01;
                break;
            case 0x11: // PWM
            case 0x1F: // QSound
                this.cmdSize = 0x02;
                break;
            default:
                this.cmdSize = 0x01;
                break;
            }
            this.dataStep = this.cmdSize * this.stepSize;
        }

        void setData(byte[] data, int dataLen, int stepSize, int stepBase) {
            if ((this.running & 0x80) > 0)
                return;

            if (dataLen > 0 && data != null) {
                this.dataLen = dataLen;
                this.data = data;
            } else {
                this.dataLen = 0x00;
                this.data = null;
            }
            this.stepSize = stepSize > 0 ? stepSize : 1;
            this.stepBase = stepBase;
            this.dataStep = this.cmdSize * this.stepSize;
        }

        void refreshData(byte[] data, int dataLen) {
            if ((this.running & 0x80) != 0)
                return;

            if (dataLen > 0 && data != null) {
                this.dataLen = dataLen;
                this.data = data;
            } else {
                this.dataLen = 0x00;
                this.data = null;
            }
        }

        void setFrequency(int chipId, int frequency) {
            if ((this.running & 0x80) != 0)
                return;

            if (frequency != 0)
                this.step = this.step * this.frequency / frequency;
            this.frequency = frequency;
        }

        void start(int dataPos, int lenMode, int length) {
            int cmdStepBase;

            if ((this.running & 0x80) != 0)
                return;

            cmdStepBase = this.cmdSize * this.stepBase;
            if (dataPos != 0xffff_ffff) { // skip setting dataStart, if Pos == -1
                this.dataStart = dataPos + cmdStepBase;
                if (this.dataStart > this.dataLen) // catch bad value and force silence
                    this.dataStart = this.dataLen;
            }

            switch (lenMode & 0x0F) {
            case DCTRL_LMODE_IGNORE: // Length instanceof already set - ignore
                break;
            case DCTRL_LMODE_CMDS: // Length = number of commands
                this.cmdsToSend = length;
                break;
            case DCTRL_LMODE_MSEC: // Length = time : msec
                this.cmdsToSend = 1000 * length / this.frequency;
                break;
            case DCTRL_LMODE_TOEND: // play unti stop-command instanceof received (or data-end instanceof reached)
                this.cmdsToSend = (this.dataLen - (this.dataStart - cmdStepBase)) / this.dataStep;
                break;
            case DCTRL_LMODE_BYTES: // raw byte count
                this.cmdsToSend = length / this.dataStep;
                break;
            default:
                this.cmdsToSend = 0x00;
                break;
            }
            this.reverse = (lenMode & 0x10) >> 4;

            this.remainCmds = this.cmdsToSend;
            this.step = 0x00;
            this.pos = 0x00;
            if (this.reverse == 0)
                this.realPos = 0x00;
            else
                this.realPos = (this.cmdsToSend - 0x01) * this.dataStep;

            this.running &= 0xfb;
            this.running |= (byte) ((lenMode & 0x80) != 0 ? 0x04 : 0x00); // set loop mode

            this.running |= 0x01; // start
            this.running &= 0xef; // command isn't yet sent
        }

        void stop() {
            if ((this.running & 0x80) != 0)
                return;

            this.running &= 0xfe; // stop
        }
    }
}
