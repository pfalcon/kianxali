package org.solhost.folko.dasm.cpu.x86;

import java.io.IOException;
import org.solhost.folko.dasm.cpu.x86.X86CPU.ExecutionMode;
import org.solhost.folko.dasm.cpu.x86.X86CPU.Model;
import org.solhost.folko.dasm.cpu.x86.X86CPU.Segment;
import org.solhost.folko.dasm.decoder.Context;
import org.solhost.folko.dasm.decoder.Decoder;
import org.solhost.folko.dasm.xml.OpcodeEntry;
import org.solhost.folko.dasm.xml.OpcodeGroup;
import org.solhost.folko.dasm.xml.OpcodeSyntax;
import org.xml.sax.SAXException;

public class X86Context implements Context {
    private final Model model;
    private final ExecutionMode execMode;
    private long instructionPointer;
    private Prefix prefix;

    public X86Context(Model model, ExecutionMode execMode) {
        this.model = model;
        this.execMode = execMode;
        reset();
    }

    @Override
    public void setInstructionPointer(long instructionPointer) {
        this.instructionPointer = instructionPointer;
    }

    @Override
    public long getInstructionPointer() {
        return instructionPointer;
    }

    public Prefix getPrefix() {
        return prefix;
    }

    public boolean acceptsOpcode(OpcodeSyntax syntax) {
        if(model != Model.ANY) {
            if(!syntax.getOpcodeEntry().supportedProcessors.contains(model)) {
                return false;
            }
        }

        switch(syntax.getOpcodeEntry().mode) {
        case LONG:
            if(execMode != ExecutionMode.LONG) {
                return false;
            }
            break;
        case PROTECTED:
            if(execMode == ExecutionMode.REAL) {
                return false;
            }
            break;
        case REAL:
        case SMM:
            break;
        default:        throw new UnsupportedOperationException("invalid execution mode: " + syntax.getOpcodeEntry().mode);
        }

        return true;
    }

    public void applyPrefix(X86Instruction inst) {
        OpcodeEntry opcode = inst.getOpcode();
        if(!opcode.belongsTo(OpcodeGroup.PREFIX)) {
            throw new UnsupportedOperationException("not a prefix");
        }

        switch(opcode.opcode) {
        case 0xF0: prefix.lockPrefix = true; break;
        case 0xF2: prefix.repNZPrefix = true; break;
        case 0xF3: prefix.repZPrefix = true; break;
        case 0x2E: prefix.overrideSegment = Segment.CS; break;
        case 0x36: prefix.overrideSegment = Segment.SS; break;
        case 0x3E: prefix.overrideSegment = Segment.DS; break;
        case 0x40:
        case 0x41:
        case 0x42:
        case 0x43:
        case 0x44:
        case 0x45:
        case 0x46:
        case 0x48:
        case 0x49:
        case 0x4A:
        case 0x4B:
        case 0x4C:
        case 0x4D:
        case 0x4E:
        case 0x4F:
                prefix.rexWPrefix = (opcode.opcode & 8) != 0;
                prefix.rexRPrefix = (opcode.opcode & 4) != 0;
                prefix.rexXPrefix = (opcode.opcode & 2) != 0;
                prefix.rexBPrefix = (opcode.opcode & 1) != 0;
                break;
        case 0x26: prefix.overrideSegment = Segment.ES; break;
        case 0x64: prefix.overrideSegment = Segment.FS; break;
        case 0x65: prefix.overrideSegment = Segment.GS; break;
        case 0x66: prefix.opSizePrefix = true; break;
        case 0x67: prefix.adrSizePrefix = true; break;
        case 0x9B: prefix.waitPrefix = true; break;
        default:
            throw new UnsupportedOperationException("unknown prefix: " + opcode);
        }
    }

    public Model getModel() {
        return model;
    }

    public ExecutionMode getExecMode() {
        return execMode;
    }

    public void reset() {
        prefix = new Prefix();
    }

    @Override
    public Decoder createInstructionDecoder() {
        try {
            return X86Decoder.fromXML("x86reference.xml", "x86reference.dtd");
        } catch (SAXException | IOException e) {
            System.err.println("Couldn't create X86 decoder: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
