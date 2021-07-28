package ru.kamuzta.bigfilesort;

import java.util.Queue;

public class Main {

    public static void main(String[] args) {
        String inputName = "bigfile.txt";
        String outputName = "bigfile.sorted.txt";
        long totalSize = (1024L * 1024L) * 1000L; //1Gb
        long totalLines = 1000000L; //1млн строк
        long memory = (1024L * 1024L) * 100L; //100Mb памяти

        BigFileSorter.generateFile(inputName,totalSize,totalLines, memory);
        BigFileSorter.sortFile(inputName,outputName,memory);

    }

}
