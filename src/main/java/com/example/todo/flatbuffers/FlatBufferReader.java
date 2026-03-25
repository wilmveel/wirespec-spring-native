package com.example.todo.flatbuffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Low-level reader for FlatBuffers tables without flatc-generated classes.
 * Wraps a ByteBuffer positioned at a table root.
 */
public final class FlatBufferReader {

    private final ByteBuffer bb;
    private final int tablePos;
    private final int vtablePos;
    private final int vtableSize;

    private FlatBufferReader(ByteBuffer bb, int tablePos) {
        this.bb = bb;
        this.tablePos = tablePos;
        this.vtablePos = tablePos - bb.getInt(tablePos);
        this.vtableSize = bb.getShort(vtablePos) & 0xFFFF;
    }

    public static FlatBufferReader fromBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int rootOffset = bb.getInt(bb.position()) + bb.position();
        return new FlatBufferReader(bb, rootOffset);
    }

    public static FlatBufferReader fromTable(ByteBuffer bb, int tablePos) {
        return new FlatBufferReader(bb, tablePos);
    }

    /**
     * Returns the field offset within the table, or 0 if absent.
     * @param fieldIndex 0-based field index from the schema
     */
    private int fieldOffset(int fieldIndex) {
        int vtableOffset = 4 + fieldIndex * 2;
        if (vtableOffset >= vtableSize) return 0;
        return bb.getShort(vtablePos + vtableOffset) & 0xFFFF;
    }

    public long readLong(int fieldIndex, long defaultValue) {
        int offset = fieldOffset(fieldIndex);
        return offset != 0 ? bb.getLong(tablePos + offset) : defaultValue;
    }

    public int readInt(int fieldIndex, int defaultValue) {
        int offset = fieldOffset(fieldIndex);
        return offset != 0 ? bb.getInt(tablePos + offset) : defaultValue;
    }

    public boolean readBoolean(int fieldIndex, boolean defaultValue) {
        int offset = fieldOffset(fieldIndex);
        return offset != 0 ? bb.get(tablePos + offset) != 0 : defaultValue;
    }

    public String readString(int fieldIndex) {
        int offset = fieldOffset(fieldIndex);
        if (offset == 0) return null;
        int stringFieldPos = tablePos + offset;
        int stringPos = stringFieldPos + bb.getInt(stringFieldPos);
        int len = bb.getInt(stringPos);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = bb.get(stringPos + 4 + i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int vectorLength(int fieldIndex) {
        int offset = fieldOffset(fieldIndex);
        if (offset == 0) return 0;
        int vectorFieldPos = tablePos + offset;
        int vectorPos = vectorFieldPos + bb.getInt(vectorFieldPos);
        return bb.getInt(vectorPos);
    }

    public FlatBufferReader vectorElement(int fieldIndex, int elementIndex) {
        int offset = fieldOffset(fieldIndex);
        if (offset == 0) return null;
        int vectorFieldPos = tablePos + offset;
        int vectorPos = vectorFieldPos + bb.getInt(vectorFieldPos);
        int dataStart = vectorPos + 4;
        int elementOffsetPos = dataStart + elementIndex * 4;
        int elementPos = elementOffsetPos + bb.getInt(elementOffsetPos);
        return new FlatBufferReader(bb, elementPos);
    }

    public ByteBuffer getByteBuffer() {
        return bb;
    }

    public int getTablePos() {
        return tablePos;
    }
}
