package kianxali.cpu.x86;

import kianxali.cpu.x86.X86CPU.X86Register;
import kianxali.decoder.Operand;
import kianxali.decoder.UsageType;
import kianxali.util.OutputFormatter;

public class RegisterOp implements Operand {
    private final UsageType usage;
    private final X86Register register;

    public RegisterOp(UsageType usage, X86Register register) {
        this.usage = usage;
        this.register = register;
    }

    @Override
    public UsageType getUsage() {
        return usage;
    }

    @Override
    public String asString(OutputFormatter formatter) {
        return formatter.formatRegister(register.toString());
    }
}
