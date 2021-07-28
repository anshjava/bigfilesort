package ru.kamuzta.bigfilesort;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

public class Util {
    public static void generateFile(String fileName, long totalSize, long totalLines, long memory) {
        //вычисляем длину строки в символах, примерно 1 байт на символ
        long start = System.nanoTime();
        int stringLength = (int) (totalSize / totalLines);
        int iterations = (int) (totalSize / memory);
        System.out.printf("НАРЕЗКА ФАЙЛА НА ЧАСТИ\nФайл из %d строк размером %d Mb будет записан в %d подходов, по %d строк в каждом с использованием %d памяти\n",
                totalLines,
                totalSize / 1024 / 1024,
                iterations,
                totalLines / iterations,
                memory);

        for (int i = 0; i < iterations; i++) {
            List<String> stringList = new ArrayList<>();
            for (int j = 0; j < totalLines / iterations; j++) {
                stringList.add(getRandomString(stringLength));
            }
            writeListToFile(stringList, fileName);
            System.out.println("Итерация # " + i + " завершена, в файл добавлено " + stringList.size() + " строк длиною " + stringLength + " символов");
        }

        long time = System.nanoTime() - start;
        System.out.printf("Файл размером %d Mb сгенерирован за %.3f секунд\n", totalSize / 1024 / 1024, time / 1e9);
    }

    //получаем рандомную строку
    private static String getRandomString(int length) {
        String symbols = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(symbols.charAt(rnd.nextInt(symbols.length())));
        return sb.toString();
    }

    //дописыват список строк в конец файла
    private static void writeListToFile(List<String> list, String fileName) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName, true))) {
            for (String line : list)
                pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //нарезает файл на части, влезающие в память по отдельности
    public static List<String> ripFileToParts(String fileName, long memory) {
        System.out.println("НАРЕЗКА ФАЙЛА НА ЧАСТИ");
        long start = System.nanoTime();
        List<String> partsNames = new ArrayList<>();
        long totalSize = 0;
        try {
            totalSize = Files.size(Paths.get(fileName));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        int iterations = (int) (totalSize / memory);
        long partSize = totalSize / iterations;


        try (BufferedReader sourceReader = new BufferedReader(new FileReader(fileName))) {
            int partNumber = 1;
            long bytesReaded = 0;
            List<String> stringList = new ArrayList<>();
            String line = "";
            //читаем построчно файл, после вычитки предельного значения байтов отправляем данные на запись в очередной кусок
            while ((line = sourceReader.readLine()) != null) {
                stringList.add(line);
                bytesReaded = bytesReaded + line.length();

                if (bytesReaded >= partSize) {
                    writeListToFile(stringList, fileName + ".part" + partNumber);
                    partsNames.add(fileName + ".part" + partNumber);
                    System.out.printf("Итерация # %d завершена, в очередной кусок перенесено %d строк\n",partsNames.size(),stringList.size());
                    partNumber++;
                    stringList.clear();
                    bytesReaded = 0;
                }
            }
            //записываем последний кусок, после выхода из цикла
            writeListToFile(stringList, fileName + ".part" + partNumber);
            partsNames.add(fileName + ".part" + partNumber);
            System.out.printf("Итерация # %d завершена, в очередной кусок перенесено %d строк\n",partsNames.size(),stringList.size());


        } catch (IOException e) {
            e.printStackTrace();
        }
        long time = System.nanoTime() - start;
        System.out.printf("Файл %s размером %d Mb  был нарезан на %d кусков за %.3f секунд\n",fileName, totalSize / 1024 / 1024, partsNames.size(), time / 1e9);
        return partsNames;
    }

    //пробегается по списку имен кусков, сортирует содержимое каждого
    public static Queue<String> sortEachPart(List<String> partsNames) {
        System.out.println("ПЕРВИЧНАЯ СОРТИРОВКА СОДЕРЖИМОГО КУСКОВ");
        Queue<String> sortedPartsNames = new LinkedList<>();

        for (String partName : partsNames) {
            List<String> stringList = null;
            try {
                stringList = Files.readAllLines(Paths.get(partName));
                Files.delete(Paths.get(partName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Collections.sort(stringList);
            writeListToFile(stringList, partName + ".sorted");

            sortedPartsNames.add(partName + ".sorted");
        }
        return sortedPartsNames;
    }

    //последовательно мержит и сортирует по 2 куска из полученного списка до тех пор, пока в списке не останется 1 файл
    public static void mergeSortParts(Queue<String> sortedPartsNames,String outputName) {
        System.out.println("СОРТИРОВКА СЛИЯНИЕМ");
        long start = System.nanoTime();
        int stepNumber = 0;
        String writeFileName;
        String fileName1;
        String fileName2;

        while (sortedPartsNames.size() >= 2) {
            fileName1 = sortedPartsNames.poll();
            fileName2 = sortedPartsNames.poll();
            stepNumber++;
            writeFileName = outputName +".step" + stepNumber;
            try (BufferedReader sourceReader1 = new BufferedReader(new FileReader(fileName1));
                 BufferedReader sourceReader2 = new BufferedReader(new FileReader(fileName2));
                 PrintWriter pw = new PrintWriter(new FileWriter(writeFileName, true))) {

                String line1 = null;
                String line2 = null;

                boolean getNext1 = true;
                boolean getNext2 = true;
                while (true) {
                    if(getNext1)
                        line1 = sourceReader1.readLine();
                    if(getNext2)
                        line2 = sourceReader2.readLine();
                    if (line1 == null && line2 == null) { //условие завершения работы с парой кусков
                        getNext1 = true;
                        getNext2 = true;
                        break;
                    } else if (line1 == null && line2 != null) {
                        pw.println(line2);
                        getNext1 = false;
                        getNext2 = true;
                    } else if (line2 == null && line1 != null) {
                        pw.println(line1);
                        getNext1 = true;
                        getNext2 = false;
                    } else {
                        if (line1.compareTo(line2) >= 0) {
                            pw.println(line2);
                            getNext1 = false;
                            getNext2 = true;
                        } else {
                            pw.println(line1);
                            getNext1 = true;
                            getNext2 = false;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Files.delete(Paths.get(fileName1));
                Files.delete(Paths.get(fileName2));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            sortedPartsNames.add(writeFileName);
        }
        File oldFileName = new File(sortedPartsNames.poll());
        File newFileName = new File(outputName);
        oldFileName.renameTo(newFileName);
        long time = System.nanoTime() - start;
        System.out.printf("Получен отсортированный итоговый файл %s. Сортировка слиянием заняла %.3f секунд\n",
                newFileName.toString(),
                time / 1e9);
    }

}