package campus.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CaptureResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private PrintWriter writer;
    private ServletOutputStream stream;

    public CaptureResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new java.io.OutputStreamWriter(buffer, StandardCharsets.UTF_8), true);
        }
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (stream == null) {
            stream = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    buffer.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    buffer.write(b, off, len);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }
            };
        }
        return stream;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        if (stream != null) {
            stream.flush();
        }
    }

    public byte[] getCapturedBytes() throws IOException {
        flushBuffer();
        return buffer.toByteArray();
    }
}
