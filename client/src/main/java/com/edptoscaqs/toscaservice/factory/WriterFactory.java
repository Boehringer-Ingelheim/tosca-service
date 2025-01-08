package com.edptoscaqs.toscaservice.factory;

import java.io.*;

public interface WriterFactory {
    FileWriter createFileWriter(String fileName) throws IOException;
    BufferedWriter createBufferedWriter(Writer writer) throws IOException;
    FileOutputStream createFileOutputStream(String fileName) throws IOException;
}