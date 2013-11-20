package kianxali.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import kianxali.decoder.Context;

public abstract class ImageFile {
    protected final ByteSequence imageFile;
    protected final long fileSize;

    public ImageFile(Path path) throws IOException {
        this.imageFile = ByteSequence.fromFile(path);
        this.fileSize = imageFile.getRemaining();
    }

    public abstract List<Section> getSections();
    public abstract Context createContext();
    public abstract long getCodeEntryPointMem();
    public abstract long toFileAddress(long memAddress);
    public abstract long toMemAddress(long fileOffset);

    public ByteSequence getByteSequence(long memAddress, boolean locked) {
        if(locked) {
            imageFile.lock();
        }
        imageFile.seek(toFileAddress(memAddress));
        return imageFile;
    }

    public Section getSectionForMemAddress(long memAddress) {
        for(Section sec : getSections()) {
            if(memAddress >= sec.getStartAddress() && memAddress < sec.getEndAddress()) {
                return sec;
            }
        }
        return null;
    }

    public boolean isValidAddress(long memAddress) {
        return getSectionForMemAddress(memAddress) != null;
    }

    public long getFileSize() {
        return fileSize;
    }
}
