package kianxali.image.pe;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kianxali.cpu.x86.X86Context;
import kianxali.cpu.x86.X86CPU.ExecutionMode;
import kianxali.cpu.x86.X86CPU.Model;
import kianxali.image.ImageFile;
import kianxali.image.Section;

public class PEFile extends ImageFile implements AddressConverter {
    private DOSStub dosStub;
    private PEHeader peHeader;
    private OptionalHeader optionalHeader;
    private List<PESection> sections;
    private Imports imports;

    public PEFile(File file) throws IOException {
        super(file);
        loadHeaders();
        loadImports();
    }

    @Override
    public X86Context createContext() {
        if(optionalHeader.isPE64()) {
            return new X86Context(Model.ANY, ExecutionMode.LONG);
        } else {
            return new X86Context(Model.ANY, ExecutionMode.PROTECTED);
        }
    }

    @Override
    public long rvaToMemory(long rva) {
        return rva + optionalHeader.getImageBase();
    }

    @Override
    public long rvaToFile(long rva) {
        for(PESection header : sections) {
            long memOffset = header.getVirtualAddressRVA();
            if(rva >= memOffset && rva < memOffset + header.getRawSize()) {
                return rva - memOffset + header.getFilePosition();
            }
        }
        throw new IllegalArgumentException("invalid rva: " + rva);
    }

    @Override
    public long fileToRVA(long offset) {
        for(PESection header : sections) {
            long fileOffset = header.getFilePosition();
            if(offset >= fileOffset && offset < fileOffset + header.getRawSize()) {
                return offset - header.getFilePosition() + header.getVirtualAddressRVA();
            }
        }
        throw new IllegalArgumentException("invalid offset: " + offset);
    }

    @Override
    public long fileToMemory(long offset) {
        return rvaToMemory(fileToRVA(offset));
    }

    @Override
    public long memoryToRVA(long mem) {
        return mem - optionalHeader.getImageBase();
    }

    @Override
    public long memoryToFile(long mem) {
        return rvaToFile(memoryToRVA(mem));
    }

    private void loadHeaders() {
        imageFile.lock();
        imageFile.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        imageFile.seek(0);
        dosStub = new DOSStub(imageFile);

        imageFile.seek(dosStub.getPEPointer());
        peHeader = new PEHeader(imageFile);

        optionalHeader = new OptionalHeader(imageFile);

        sections = new ArrayList<>(peHeader.getNumSections());
        for(int i = 0; i < peHeader.getNumSections(); i++) {
            sections.add(i, new PESection(imageFile, this));
        }
        imageFile.unlock();
    }

    private void loadImports() {
        long importsRVA = optionalHeader.getDataDirectoryOffsetRVA(OptionalHeader.DATA_DIRECTORY_IMPORT);
        if(importsRVA != 0) {
            imageFile.lock();
            imageFile.seek(rvaToFile(importsRVA));
            imports = new Imports(imageFile, this);
            imageFile.unlock();
        } else {
            // no imports
            imports = new Imports();
            imports.getDLLName(0); // XXX: remove
        }
    }

    @Override
    public List<Section> getSections() {
        List<Section> res = new ArrayList<>(sections.size());
        for(Section section : sections) {
            res.add(section);
        }
        return Collections.unmodifiableList(res);
    }

    @Override
    public long toFileAddress(long memAddress) {
        return memoryToFile(memAddress);
    }

    @Override
    public long toMemAddress(long fileOffset) {
        return fileToMemory(fileOffset);
    }

    @Override
    public long getCodeEntryPointMem() {
        return rvaToMemory(optionalHeader.getEntryPointRVA());
    }
}