package org.wso2.carbon.uuf.handlebars;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaceholderWriter extends Writer {

    private List<StringBuilder> buffers;
    private StringBuilder currentBuffer;
    private List<String> placeholders;

    public PlaceholderWriter() {
        buffers = new ArrayList<>();
        currentBuffer = new StringBuilder();
        buffers.add(currentBuffer);
        placeholders = new ArrayList<>();
    }

    @Override
    public Writer append(final char c) throws IOException {
        currentBuffer.append(c);
        return this;
    }

    @Override
    public Writer append(final CharSequence csq) throws IOException {
        currentBuffer.append(csq);
        return this;
    }

    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        currentBuffer.append(csq, start, end);
        return this;
    }

    @Override
    public void write(final char[] buffer) throws IOException {
        currentBuffer.append(buffer);
    }

    @Override
    public void write(final int c) throws IOException {
        currentBuffer.append((char) c);
    }

    @Override
    public void write(final String str) throws IOException {
        currentBuffer.append(str);
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        currentBuffer.append(str, off, len);
    }

    @Override
    public void write(final char[] buffer, final int off, final int len) throws IOException {
        currentBuffer.append(buffer, off, len);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        currentBuffer = null;
        buffers = null;
        placeholders = null;
    }

    public void addPlaceholder(final String placeholderName) {
        currentBuffer = new StringBuilder();
        buffers.add(currentBuffer);
        placeholders.add(placeholderName);
    }

    public String toString(Map<String, String> placeholderValues) {
        StringBuilder out = new StringBuilder();
        out.append(buffers.get(0));
        for (int i = 0; i < placeholders.size(); i++) {
            String key = placeholders.get(i);
            String placeholderValue;
            if (key != null) {
                out.append("<!-- start ");
                out.append(key);
                out.append(" -->");
                if ((placeholderValue = placeholderValues.get(key)) != null) {
                    out.append(placeholderValue);
                }
                out.append("<!-- end ");
                out.append(key);
                out.append(" -->");
            }
            out.append(buffers.get(i + 1));
        }
        return out.toString();
    }

    @Override
    public String toString() {
        StringBuilder tmpBuffer = new StringBuilder();
        buffers.forEach(tmpBuffer::append);
        return tmpBuffer.toString();
    }
}
