package com.edptoscaqs.toscaservice.factory;

import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class DefaultWriterFactory implements WriterFactory {
    @Override
    public FileWriter createFileWriter(String fileName) throws IOException {
        return new FileWriter(fileName);
    }

    @Override
    public BufferedWriter createBufferedWriter(Writer writer) {
        return new BufferedWriter(writer);
    }

    @Override
    public FileOutputStream createFileOutputStream(String fileName) throws IOException {
        return new FileOutputStream(fileName);
    }
}
