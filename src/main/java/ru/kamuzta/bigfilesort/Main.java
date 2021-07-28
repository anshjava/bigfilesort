package ru.kamuzta.bigfilesort;

import java.io.*;
import java.util.Queue;

public class Main {

    public static void main(String[] args) {
        String inputName = "bigfile.txt";
        String outputName = "bigfile.sorted.txt";
        long totalSize = (1024L * 1024L) * 10000L; //10Gb
        long totalLines = 10000000L; //10млн строк
        long memory = (1024L * 1024L) * 500L; //500Mb памяти

        Util.generateFile(inputName,totalSize,totalLines, memory);
        Queue<String> queue = Util.sortEachPart(Util.ripFileToParts(inputName,memory));
        Util.mergeSortParts(queue,outputName);

    }

}
