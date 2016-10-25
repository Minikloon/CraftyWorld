package club.kazza.kazzacraft.utils;

public final class LongPackedArray {
    private final long[] words;
    private final byte bitsPerValue;
    private final long valueMask;

    public static final byte bitsPerWord = Long.SIZE;

    public LongPackedArray(int bitsPerValue, int capacity) {
        if (bitsPerValue > bitsPerWord)
            throw new IllegalArgumentException("bitsPerValue (" + bitsPerValue + ") can't be greater than " + bitsPerWord);

        int longs = (int) Math.ceil((bitsPerValue * capacity) / bitsPerWord);
        this.words = new long[longs];
        this.bitsPerValue = (byte) bitsPerValue;
        this.valueMask = (1L << bitsPerValue) - 1L;
    }

    public long[] getBacking() {
        return words;
    }

    public int get(int index) {
        index *= bitsPerValue;
        int wordIndex = index / bitsPerWord;
        int shifts = index & (bitsPerWord - 1);

        long value = words[wordIndex] >>> shifts;
        if (shifts + bitsPerValue > bitsPerWord) {
            value |= words[++wordIndex] << bitsPerWord - shifts;
        }

        return (int) (value & valueMask);
    }

    public void set(int index, int value) {
        if (value > valueMask) {
            throw new IllegalArgumentException("value (" + value + ") must not be greater than " + valueMask);
        }

        index *= bitsPerValue;
        int wordIndex = index / bitsPerWord;
        int shifts = index & (bitsPerWord - 1);

        words[wordIndex] = this.words[wordIndex] & ~(this.valueMask << shifts) | (value & valueMask) << shifts;

        int i2 = shifts + bitsPerValue;
        // The value is divided over two long values
        if (i2 > bitsPerWord) {
            wordIndex++;
            words[wordIndex] = words[wordIndex] & ~((1L << i2 - bitsPerWord) - 1L) | value >> bitsPerWord - shifts;
        }
    }
}

