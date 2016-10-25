package club.kazza.kazzacraft.utils;

public final class NibbleArray {
    private final byte[] backing;

    public NibbleArray(int size) {
        backing = new byte[size / 2];
    }

    public byte[] getBacking() {
        return backing;
    }

    public byte get(int index) {
        byte val = backing[index / 2];
        if (index % 2 == 0) {
            return (byte) (val & 0x0f);
        } else {
            return (byte) ((val & 0xf0) >> 4);
        }
    }

    public void set(int index, int value) {
        value &= 0xf;
        int half = index / 2;
        byte previous = backing[half];
        if (index % 2 == 0) {
            backing[half] = (byte) (previous & 0xf0 | value);
        } else {
            backing[half] = (byte) (previous & 0x0f | value << 4);
        }
    }
}
