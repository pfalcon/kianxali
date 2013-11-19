package kianxali.cpu.x86;

import kianxali.cpu.x86.xml.OperandDesc;
import kianxali.cpu.x86.xml.OperandDesc.OperandType;

public final class X86CPU {
    public enum Model {
        I8086, I80186, I80286, I80386, I80486,
        PENTIUM, PENTIUM_MMX, PENTIUM_PRO, PENTIUM_II, PENTIUM_III, PENTIUM_IV,
        CORE_1, CORE_2, CORE_I7,
        ITANIUM,
        ANY
    }

    // Utility class, no constructor
    private X86CPU() {

    }

    public enum OperandSize {
        O8, O16, O32, O64, O80, O128
    }

    public enum AddressSize {
        A16, A32, A64
    }

    public enum Register {
        // generic 8 bit
        AL, AH, BL, BH, CL, CH, DL, DH,

        // generic 16 bit
        AX, BX, CX, DX, BP, SP, SI, DI,

        // generic 32 bit
        EAX, EBX, ECX, EDX, EBP, ESP, ESI, EDI,

        // generic 64 bit
        RAX, RBX, RCX, RDX, RSP, RBP, RSI, RDI,
        R8, R9, R10, R11, R12, R13, R14, R15,
        // lower 8 bit
        SIL, DIL, BPL, SPL,
        R8B, R9B, R10B, R11B, R12B, R13B, R14B, R15B,
        // lower 16 bit
        R8W, R9W, R10W, R11W, R12W, R13W, R14W, R15W,
        // lower 32 bit
        R8D, R9D, R10D, R11D, R12D, R13D, R14D, R15D,

        // segment registers
        CS, DS, ES, FS, GS, SS,

        // Control registers
        CR0, CR2, CR3, CR4,

        // Debug registers
        DR0, DR1, DR2, DR3, DR4, DR5, DR6, DR7,

        // Test registers
        TR0, TR1, TR2, TR3, TR4, TR5, TR6, TR7,

        // FPU registers
        ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7,

        // MMX registers (are actually aliases for FPU registers)
        MM0, MM1, MM2, MM3, MM4, MM5, MM6, MM7,

        // SSE registers
        XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7, XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15
    }

    public enum ExecutionMode {
        REAL, PROTECTED, LONG, SMM
    }

    public enum Segment {
        CS, DS, SS, ES, FS, GS
    }

    public enum InstructionSetExtension {
        MMX, SMX, VMX,
        SSE_1, SSE_2, SSE_3, SSE_4_1, SSE_4_2, SSSE_3
    }

    public static AddressSize getAddressSize(X86Context ctx) {
        switch(ctx.getExecMode()) {
        case SMM:
            // TODO: not sure if fall-through to long mode is correct here
        case LONG:
            if(ctx.getPrefix().adrSizePrefix) {
                return AddressSize.A32;
            } else {
                return AddressSize.A64;
            }
        case PROTECTED:
            if(ctx.getPrefix().adrSizePrefix) {
                return AddressSize.A16;
            } else {
                return AddressSize.A32;
            }
        case REAL:
            return AddressSize.A16;
        default:
            throw new RuntimeException("invalid cpu mode: " + ctx.getExecMode());
        }
    }

    private static OperandSize getDefaultOperandSize(X86Context ctx) {
        switch(ctx.getExecMode()) {
        case SMM:
            // TODO: not sure if fall-through to long mode is correct here
        case LONG:
            if(ctx.getPrefix().rexWPrefix) {
                return OperandSize.O64;
            } else if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O16;
            } else {
                return OperandSize.O32;
            }
        case PROTECTED:
            if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O16;
            } else {
                return OperandSize.O32;
            }
        case REAL:
            return OperandSize.O16;
        default:
            throw new RuntimeException("invalid cpu mode: " + ctx.getExecMode());
        }
    }

    public static OperandSize getOperandSize(X86Context ctx, OperandType opType) {
        switch(opType) {
        case BYTE:
            return OperandSize.O8;
        case WORD_OPS:      // TODO: check
        case WORD_FPU:
        case WORD:
            return OperandSize.O16;
        case WORD_DWORD_64: // TODO: check
            return getDefaultOperandSize(ctx);
        case WORD_DWORD_S64:
        case WORD_DWORD:
            if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O16;
            } else {
                return OperandSize.O32;
            }
        case DWORD_QWORD:
            if(ctx.getPrefix().rexWPrefix) {
                return OperandSize.O64;
            } else {
                return OperandSize.O32;
            }
        case POINTER_REX:
            if(ctx.getPrefix().rexWPrefix) {
                return OperandSize.O80;
            } else if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O16;
            } else {
                return OperandSize.O32;
            }
        case POINTER:
            if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O16;
            } else {
                return OperandSize.O32;
            }
        case DWORD_INT_FPU:
        case REAL_SINGLE_FPU:
            return OperandSize.O32;
        case DOUBLE_FPU:
        case QWORD_FPU:
            return OperandSize.O64;
        case DWORD:
            return OperandSize.O32;
        case QWORD:
            return OperandSize.O64;
        case QWORD_WORD:
            if(ctx.getPrefix().opSizePrefix) {
                return OperandSize.O64;
            } else {
                return OperandSize.O16;
            }
        case SCALAR_DOUBLE:
            return OperandSize.O64;
        case SCALAR_SINGLE:
            return OperandSize.O32;
        case DQWORD:
        case DOUBLE_128:
        case SINGLE_128:
            return OperandSize.O128;
        case REAL_EXT_FPU:
            return OperandSize.O80;
        default:
            throw new UnsupportedOperationException("invalid operand type: " + opType);
        }
    }

    private static Register getGenericRegister8(short id) {
        switch(id) {
        case 0: return Register.AL;
        case 1: return Register.CL;
        case 2: return Register.DL;
        case 3: return Register.BL;
        case 4: return Register.AH;
        case 5: return Register.CH;
        case 6: return Register.DH;
        case 7: return Register.BH;
        case 8: return Register.R8B;
        case 9: return Register.R9B;
        case 10:return Register.R10B;
        case 11:return Register.R11B;
        case 12:return Register.R12B;
        case 13:return Register.R13B;
        case 14:return Register.R14B;
        case 15:return Register.R15B;
        default:
            throw new UnsupportedOperationException("invalid generic 8 bit register: " + id);
        }
    }

    private static Register getGenericRegister16(short id) {
        switch(id) {
        case 0: return Register.AX;
        case 1: return Register.CX;
        case 2: return Register.DX;
        case 3: return Register.BX;
        case 4: return Register.SP;
        case 5: return Register.BP;
        case 6: return Register.SI;
        case 7: return Register.DI;
        case 8: return Register.R8W;
        case 9: return Register.R9W;
        case 10:return Register.R10W;
        case 11:return Register.R11W;
        case 12:return Register.R12W;
        case 13:return Register.R13W;
        case 14:return Register.R14W;
        case 15:return Register.R15W;
        default:
            throw new UnsupportedOperationException("invalid generic 16 bit register: " + id);
        }
    }

    private static Register getGenericRegister32(short id) {
        switch(id) {
        case 0: return Register.EAX;
        case 1: return Register.ECX;
        case 2: return Register.EDX;
        case 3: return Register.EBX;
        case 4: return Register.ESP;
        case 5: return Register.EBP;
        case 6: return Register.ESI;
        case 7: return Register.EDI;
        case 8: return Register.R8D;
        case 9: return Register.R9D;
        case 10:return Register.R10D;
        case 11:return Register.R11D;
        case 12:return Register.R12D;
        case 13:return Register.R13D;
        case 14:return Register.R14D;
        case 15:return Register.R15D;
        default:
            throw new UnsupportedOperationException("invalid generic 32 bit register: " + id);
        }
    }

    private static Register getGenericRegister64(short id) {
        switch(id) {
        case 0: return Register.RAX;
        case 1: return Register.RCX;
        case 2: return Register.RDX;
        case 3: return Register.RBX;
        case 4: return Register.RSP;
        case 5: return Register.RBP;
        case 6: return Register.RSI;
        case 7: return Register.RDI;
        case 8: return Register.R8;
        case 9: return Register.R9;
        case 10:return Register.R10;
        case 11:return Register.R11;
        case 12:return Register.R12;
        case 13:return Register.R13;
        case 14:return Register.R14;
        case 15:return Register.R15;
        default:
            throw new UnsupportedOperationException("invalid generic 64 bit register: " + id);
        }
    }

    private static Register getSegmentRegister(short id) {
        switch(id & 0x7) {
        case 0: return Register.ES;
        case 1: return Register.CS;
        case 2: return Register.SS;
        case 3: return Register.DS;
        case 4: return Register.FS;
        case 5: return Register.GS;
        default:
            throw new UnsupportedOperationException("invalid segment register: " + id);
        }
    }

    private static Register getFPURegister(short id) {
        switch(id & 0x07) {
        case 0: return Register.ST0;
        case 1: return Register.ST1;
        case 2: return Register.ST2;
        case 3: return Register.ST3;
        case 4: return Register.ST4;
        case 5: return Register.ST5;
        case 6: return Register.ST6;
        case 7: return Register.ST7;
        default:
            throw new UnsupportedOperationException("invalid FPU register: " + id);
        }
    }

    private static Register getMMXRegister(short id) {
        switch(id & 0x07) {
        case 0: return Register.MM0;
        case 1: return Register.MM1;
        case 2: return Register.MM2;
        case 3: return Register.MM3;
        case 4: return Register.MM4;
        case 5: return Register.MM5;
        case 6: return Register.MM6;
        case 7: return Register.MM7;
        default:
            throw new UnsupportedOperationException("invalid MMX register: " + id);
        }
    }

    private static Register getXMMRegister(short id) {
        switch(id) {
        case 0: return Register.XMM0;
        case 1: return Register.XMM1;
        case 2: return Register.XMM2;
        case 3: return Register.XMM3;
        case 4: return Register.XMM4;
        case 5: return Register.XMM5;
        case 6: return Register.XMM6;
        case 7: return Register.XMM7;
        case 8: return Register.XMM8;
        case 9: return Register.XMM9;
        case 10: return Register.XMM10;
        case 11: return Register.XMM11;
        case 12: return Register.XMM12;
        case 13: return Register.XMM13;
        case 14: return Register.XMM14;
        case 15: return Register.XMM15;
        default:
            throw new UnsupportedOperationException("invalid XMM register: " + id);
        }
    }

    private static Register getControlRegister(short id) {
        switch(id) {
        case 0: return Register.CR0;
        case 2: return Register.CR2;
        case 3: return Register.CR3;
        case 4: return Register.CR4;
        default:
            throw new UnsupportedOperationException("invalid control register: " + id);
        }
    }

    private static Register getDebugRegister(short id) {
        switch(id) {
        case 0: return Register.DR0;
        case 1: return Register.DR1;
        case 2: return Register.DR2;
        case 3: return Register.DR3;
        case 4: return Register.DR4;
        case 5: return Register.DR5;
        case 6: return Register.DR6;
        case 7: return Register.DR7;
        default:
            throw new UnsupportedOperationException("invalid debug register: " + id);
        }
    }

    private static Register getTestRegister(short id) {
        switch(id) {
        case 0: return Register.TR0;
        case 1: return Register.TR1;
        case 2: return Register.TR2;
        case 3: return Register.TR3;
        case 4: return Register.TR4;
        case 5: return Register.TR5;
        case 6: return Register.TR6;
        case 7: return Register.TR7;
        default:
            throw new UnsupportedOperationException("invalid test register: " + id);
        }
    }

    public static Register getGenericAddressRegister(X86Context ctx, short id) {
        if(ctx.getExecMode() != ExecutionMode.LONG && id > 7) {
            throw new UnsupportedOperationException("used 64 bit register id in 32 bit mode");
        }
        AddressSize adrSize = getAddressSize(ctx);
        switch(adrSize) {
        case A16:   return getGenericRegister16(id);
        case A32:   return getGenericRegister32(id);
        case A64:   return getGenericRegister64(id);
        default:    throw new UnsupportedOperationException("invalid adrSize: " + adrSize);
        }
    }

    private static Register getOperandRegisterGeneral(OperandDesc op, X86Context ctx, short id) {
        if(ctx.getExecMode() != ExecutionMode.LONG && id > 7) {
            throw new UnsupportedOperationException("used 64 bit register id in 32 bit mode");
        }
        OperandSize opSize = getOperandSize(ctx, op.operType);
        switch(opSize) {
        case O8:    return getGenericRegister8(id);
        case O16:   return getGenericRegister16(id);
        case O32:   return getGenericRegister32(id);
        case O64:   return getGenericRegister64(id);
        default:    throw new UnsupportedOperationException("invalid opSize: " + opSize);
        }
    }

    public static Register getOperandRegister(OperandDesc op, X86Context ctx, short id) {
        switch(op.adrType) {
        case MOD_RM_R:
        case MOD_RM_M:
        case LEAST_REG:
            return getOperandRegisterGeneral(op, ctx, id);
        case MOD_RM_R_SEG:
            return getSegmentRegister(id);
        case MOD_RM_M_FPU:
        case MOD_RM_M_FPU_REG:
            return getFPURegister(id);
        case MOD_RM_MMX:
        case MOD_RM_R_MMX:
        case MOD_RM_M_MMX:
            return getMMXRegister(id);
        case MOD_RM_M_XMM_REG:
        case MOD_RM_XMM:
        case MOD_RM_R_XMM:
            return getXMMRegister(id);
        case SEGMENT2:
            return getSegmentRegister((short) ((id >> 3) & 0x3));
        case SEGMENT33:
            return getSegmentRegister((short) ((id >> 3) & 0x7));
        case MOD_RM_R_FORCE_GEN:
            return getGenericAddressRegister(ctx, id);
        case MOD_RM_R_DEBUG:
            return getDebugRegister(id);
        case MOD_RM_R_CTRL:
            return getControlRegister(id);
        case MOD_RM_R_TEST:
            return getTestRegister(id);
        case GROUP:
            switch(op.directGroup) {
            case GENERIC:   return getOperandRegisterGeneral(op, ctx, id);
            case X87FPU:    return getFPURegister(id);
            default:        throw new UnsupportedOperationException("invalid directGroup: " + op.directGroup);
            }
        default:
            throw new UnsupportedOperationException("invalid adrType: " + op.adrType);
        }
    }
}