package org.solhost.folko.dasm.images.pe;

public interface AddressConverter {
    public long rvaToMemory(long rva);
    public long rvaToFile(long rva);
    public long fileToRVA(long offset);
    public long fileToMemory(long offset);
    public long memoryToRVA(long mem);
    public long memoryToFile(long mem);
}