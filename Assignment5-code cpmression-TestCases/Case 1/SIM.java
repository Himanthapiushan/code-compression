

import java.io.*;
import java.util.*;

public class SIM {

    public static void main(String[] args) throws FileNotFoundException {
        String originalPath="original.txt";
        String compressedPath="compressed.txt";
        //compression(originalPath);
        //decompression(compressedPath);

        for(String input: args){
            if("1".equals(input)){
                compression(originalPath);
            }
            if("2".equals(input)){
                decompression(compressedPath);
            }
        }
    }

    private static void compression (String originalPath) throws  FileNotFoundException{
        List<String> dictionary32bitEntries=dictionaryCreator(originalPath);
        List<String> originalCode=originalCodeToList(originalPath);
        List<String> compressedInstructions=new ArrayList<>();
        boolean isPreviousInstructionRLE= false;
        for(int i=0;i<originalCode.size();i++){
            String instructionLine=originalCode.get(i);
            List<String> allCompressions= new ArrayList<>();
            allCompressions.add(originalBinaryComp(instructionLine));
            if (i!=0 && !isPreviousInstructionRLE){
                allCompressions.add(rleComp(i,originalCode,isPreviousInstructionRLE));
            }
            else{
                allCompressions.add("cannot compress with this method");
            }
            allCompressions.add(bitMaskedBasedComp(instructionLine,dictionary32bitEntries));
            allCompressions.add(oneBitMismatchComp(instructionLine,dictionary32bitEntries));
            allCompressions.add(twoBitMismatchComp(instructionLine,dictionary32bitEntries));
            allCompressions.add(twoBitMismatchAnywhereComp(instructionLine,dictionary32bitEntries));
            allCompressions.add(fourBitMismatchComp(instructionLine,dictionary32bitEntries));
            allCompressions.add(directComp(instructionLine,dictionary32bitEntries));

            String optimalCompression= findOptimalCompression(allCompressions);
            compressedInstructions.add(optimalCompression);
            if (optimalCompression.equals(allCompressions.get(1))){
                isPreviousInstructionRLE=true;
                i+=Integer.parseInt(allCompressions.get(1).substring(3),2);
            }else {
            isPreviousInstructionRLE=false;     }
        }
        //System.out.println(compressedInstructions);
        writeCompressedToTxt(compressedInstructions);
    }

    private static void writeCompressedToTxt(List<String> compressedInstructions){

        StringBuilder outputSequence = new StringBuilder();

        for (String compressedInstruction: compressedInstructions) {
            outputSequence.append(compressedInstruction);
        }

        if (outputSequence.length()%32 != 0){
            outputSequence.append(String.join("", Collections.nCopies(32 - outputSequence.length() % 32, "0")));
        }

        try {
            FileWriter fw = new FileWriter("cout.txt");
            BufferedWriter bw = new BufferedWriter(fw);

            for (int i = 0; i < outputSequence.length(); i+=32){
                bw.write(outputSequence.substring(i,i+32));
                bw.newLine();
            }
            bw.close();
            fw.close();
        }catch (IOException ex){
            System.out.println(ex);
        }
    }

    private static String originalBinaryComp(String instructionLine){
        return "000" + instructionLine;
    }


    private static String rleComp(int i,List<String> originalCode, boolean isPreviousInstructionRLE){
        String instructionLine=originalCode.get(i);
        String compressedIns = null;
        if (instructionLine.equals(originalCode.get(i - 1))){
            int runCount=1;
            int j=i+1;
            while (true){
                if (instructionLine.equals(originalCode.get(j)) && runCount<8 && !isPreviousInstructionRLE){
                    runCount+=1;
                    j++;
                }
                else {
                    break;
                }
            }
            String binaryRunCount = Integer.toBinaryString(runCount-1);

            if (binaryRunCount.length() != 3){
                binaryRunCount = String.join("", Collections.nCopies(3 - binaryRunCount.length(),"0")) + binaryRunCount;
            }
            compressedIns = "001" + binaryRunCount;
        }else {
            compressedIns = "cannot compress with this method";
        }
        return compressedIns;
        }




    private static String bitMaskedBasedComp(String instructionLine,List<String>dictionary32bitEntries){
        //entry-32,code-4
        String compressedIns="cannot compress with this method";
       for (String dictionaryEntry:dictionary32bitEntries){
           String xorResult=xorCalculation(instructionLine,dictionaryEntry);
           if (xorResult.contains("1")) {
               int startingLocationIndex=xorResult.indexOf('1');
               if (startingLocationIndex<=xorResult.length()-4 && xorResult.substring(startingLocationIndex + 4).chars().filter(ch -> ch == '1').count() == 0) {
                   String startingLocation = Integer.toBinaryString(startingLocationIndex);
                   String bitmask = xorResult.substring(startingLocationIndex, startingLocationIndex + 4);
                   String dictionaryEntryIndex = Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                   if (startingLocation.length() != 5) {
                       startingLocation = String.join("", Collections.nCopies(5 - startingLocation.length(), "0")) + startingLocation;
                   }
                   if (dictionaryEntryIndex.length() != 4) {
                       dictionaryEntryIndex = String.join("", Collections.nCopies(4 - dictionaryEntryIndex.length(), "0")) + dictionaryEntryIndex;
                   }
                   compressedIns = "010" + startingLocation + bitmask + dictionaryEntryIndex;
                   break;
               }
           }
           else {
               compressedIns="cannot compress with this method";
           }
       }
        return compressedIns;
    }

    private static String oneBitMismatchComp(String instructionLine,List<String>dictionary32bitEntries){
        //entry-32,code-4
        String compressedIns="cannot compress with this method";
        for (String dictionaryEntry: dictionary32bitEntries){
            String xorResult = xorCalculation(instructionLine,dictionaryEntry);
            if (xorResult.chars().filter(ch-> ch=='1').count()==1){
                String startingLocation= Integer.toBinaryString(xorResult.indexOf('1'));
                String dictionaryEntryIndex = Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                if(startingLocation.length()!=5){
                    startingLocation=String.join("", Collections.nCopies(5-startingLocation.length(),"0"))+startingLocation;
                }
                if(dictionaryEntryIndex.length()!=4){
                    dictionaryEntryIndex=String.join("", Collections.nCopies(4-dictionaryEntryIndex.length(),"0"))+dictionaryEntryIndex;
                }
                compressedIns = "011" + startingLocation + dictionaryEntryIndex;
                break;
            }
        }
        return compressedIns;
    }

    private static String twoBitMismatchComp(String instructionLine,List<String>dictionary32bitEntries){
        //entry-32,code-4
        String compressedIns="cannot compress with this method";
        for (String dictionaryEntry: dictionary32bitEntries){
            String xorResult = xorCalculation(instructionLine,dictionaryEntry);
            if (xorResult.chars().filter(ch-> ch=='1').count()==2 && xorResult.contains("11")){
                String startingLocation= Integer.toBinaryString(xorResult.indexOf('1'));
                String dictionaryEntryIndex = Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                if(startingLocation.length()!=5){
                    startingLocation=String.join("", Collections.nCopies(5-startingLocation.length(),"0"))+startingLocation;
                }
                if(dictionaryEntryIndex.length()!=4){
                    dictionaryEntryIndex=String.join("", Collections.nCopies(4-dictionaryEntryIndex.length(),"0"))+dictionaryEntryIndex;
                }
                compressedIns = "100" + startingLocation + dictionaryEntryIndex;
                break;
            }
        }
        return compressedIns;
    }

    private static String fourBitMismatchComp(String instructionLine,List<String>dictionary32bitEntries){
        //entry-32,code-4
        String compressedIns="cannot compress with this method";
        for (String dictionaryEntry: dictionary32bitEntries){
            String xorResult = xorCalculation(instructionLine,dictionaryEntry);
            if (xorResult.chars().filter(ch-> ch=='1').count()==4 && xorResult.contains("1111")){
                String startingLocation= Integer.toBinaryString(xorResult.indexOf('1'));
                String dictionaryEntryIndex = Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                if(startingLocation.length()!=5){
                    startingLocation=String.join("", Collections.nCopies(5-startingLocation.length(),"0"))+startingLocation;
                }
                if(dictionaryEntryIndex.length()!=4){
                    dictionaryEntryIndex=String.join("", Collections.nCopies(4-dictionaryEntryIndex.length(),"0"))+dictionaryEntryIndex;
                }
                compressedIns = "101" + startingLocation + dictionaryEntryIndex;
                break;
            }
        }
        return compressedIns;
    }

    private static String twoBitMismatchAnywhereComp(String instructionLine,List<String>dictionary32bitEntries){
        String compressedIns="cannot compress with this method";
        for (String dictionaryEntry: dictionary32bitEntries){
            String xorResult= xorCalculation(instructionLine,dictionaryEntry);
            int startingLocationOneIndex=xorResult.indexOf('1');
            String startingLocationOne=Integer.toBinaryString(startingLocationOneIndex);
            if (startingLocationOneIndex+2<xorResult.length()) {
                if (xorResult.contains("1") && xorResult.chars().filter(ch -> ch == '1').count() == 2 &&
                        xorResult.substring(startingLocationOneIndex + 2).chars().filter(ch -> ch == '1').count() == 1) {
                    String dictionaryEntryIndex = Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                    String a = xorResult.substring(startingLocationOneIndex + 2);
                    String b = String.join("", Collections.nCopies(xorResult.length() - a.length(), "0")) + a;
                    String startingLocationTwo = Integer.toBinaryString(b.indexOf('1'));
                    if (startingLocationOne.length() != 5) {
                        startingLocationOne = String.join("", Collections.nCopies(5 - startingLocationOne.length(), "0")) + startingLocationOne;
                    }
                    if (startingLocationTwo.length() != 5) {
                        startingLocationTwo = String.join("", Collections.nCopies(5 - startingLocationTwo.length(), "0")) + startingLocationTwo;
                    }
                    if (dictionaryEntryIndex.length() != 4) {
                        dictionaryEntryIndex = String.join("", Collections.nCopies(4 - dictionaryEntryIndex.length(), "0")) + dictionaryEntryIndex;
                    }
                    compressedIns = "110" + startingLocationOne + startingLocationTwo + dictionaryEntryIndex;
                    break;
                }
            }
            else{
                compressedIns="cannot compress with this method";
            }
        }
        return compressedIns;
    }

    private static String directComp(String instructionLine,List<String>dictionary32bitEntries){
        String compressedIns="cannot compress with this method";
        for(String dictionaryEntry: dictionary32bitEntries){
            String xorResult=xorCalculation(dictionaryEntry,instructionLine);
            if (xorResult.chars().filter(ch-> ch=='0').count()==32){
                String dictionaryEntryIndex=Integer.toBinaryString(dictionary32bitEntries.indexOf(dictionaryEntry));
                if(dictionaryEntryIndex.length()!=4){
                    dictionaryEntryIndex=String.join("", Collections.nCopies(4-dictionaryEntryIndex.length(),"0"))+dictionaryEntryIndex;
                }
                compressedIns="111"+dictionaryEntryIndex;
                break;
            }
        }
        return compressedIns;
    }


    private static String xorCalculation(String string1, String string2){

        StringBuilder s= new StringBuilder();

        for (int i = 0; i < string1.length(); i++){
            if (string1.charAt(i) == string2.charAt(i)){
                s.append("0");
            }else {
                s.append("1");
            }
        }
        return s.toString();
    }

    private static String findOptimalCompression(List<String> compressedInstructions){

        String optimalCompression = compressedInstructions.get(0);

        for (String compressedInstructionLine: compressedInstructions) {
            if (!compressedInstructionLine.equals("cannot compress with this method") && (compressedInstructionLine.length() < optimalCompression.length()) ){
                optimalCompression = compressedInstructionLine;
            }
        }
        return optimalCompression;
    }


    private static List<String> originalCodeToList (String originalPath) throws FileNotFoundException {
        List originalList= new ArrayList();
        Scanner scr=new Scanner(new File(originalPath));
        while (scr.hasNext()){
            String instructionLine= scr.next();
            originalList.add(instructionLine);
        }
        scr.close();
        return originalList;
    }


    private static List<String> dictionaryCreator(String originalPath) throws FileNotFoundException {

        ArrayList<String> instructions= new ArrayList<>(); //instructions without repetitions
        Map<String,Integer> freqMap= new HashMap<>();//number of times one instruction occurred
        List<String> originalCode= originalCodeToList(originalPath);  //original code into a list

        for(String instructionLine:originalCode){
            if (!instructions.contains(instructionLine)){
                instructions.add(instructionLine);
            }
            if (!freqMap.containsKey(instructionLine)){
                freqMap.put(instructionLine,1);
            }
            else{
                Integer freqMapCount= freqMap.get(instructionLine);
                freqMap.replace(instructionLine,freqMapCount+1);
            }
        }

        //System.out.println(dictionaryEntries);

        return dictionarySorting(freqMap,instructions);


    }
    private static List<String> dictionarySorting(Map<String,Integer>freqMap,ArrayList<String> instructions) {
        //creating maps in decreasing orders
        LinkedHashMap<String,Integer> sortedfreqMapReverse=new LinkedHashMap<>();
        freqMap.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).forEachOrdered(x ->
                sortedfreqMapReverse.put(x.getKey(),x.getValue()));


        ArrayList<String> binaryValues=new ArrayList<>();//list contains binary instructions only
        ArrayList<Integer> freqList=new ArrayList<>();//list contains frequency values only

        for (String instructionLine: sortedfreqMapReverse.keySet()){
            binaryValues.add(instructionLine);
            freqList.add(freqMap.get(instructionLine));
        }

        ArrayList<String> dictionary = new ArrayList<>();

        for (int i=0;i<binaryValues.size();i++){
            int lastIndex= freqList.lastIndexOf(freqList.get(i));
            int ties=lastIndex-i+1;
            if (ties>1){

                List<String>tiedInstructions=binaryValues.subList(i,i+ties);
                Map<String,Integer> tiedMapWithInstructionSet= new HashMap<>();

                for(String tiedInstruction: tiedInstructions){
                    tiedMapWithInstructionSet.put(tiedInstruction,instructions.indexOf(tiedInstruction));
                }

                LinkedHashMap<String,Integer> sortedTiedInstructionSet=new LinkedHashMap<>();
                tiedMapWithInstructionSet.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x ->
                        sortedTiedInstructionSet.put(x.getKey(),x.getValue()));
                dictionary.addAll(sortedTiedInstructionSet.keySet());

            }else {
                dictionary.add(binaryValues.get(i));
            }
            i+=lastIndex-i;
            if (lastIndex==freqList.size()-1){
                break;
            }
        }
        //System.out.println(dictionary.subList(0,16));
        //System.out.println(sortedfreqMapReverse);
        return dictionary.subList(0,16);
    }


    //Decompression

    private static String originalBinaryDeComp(String CompressedIns){
        return CompressedIns.substring(3);
    }

    private static List<String> rleDeComp (String previousDeCompressedIns, String currentCompressedIns){
        List<String> decompressedInstructions = new ArrayList<>();

        for (int i=0; i < Integer.parseInt(currentCompressedIns.substring(3),2); i++){
            decompressedInstructions.add(previousDeCompressedIns);
        }

        return decompressedInstructions;
    }

    private static String bitMaskBasedDeComp(String CompressedIns, List<String> dictionary){

        String startingLocation = CompressedIns.substring(3,8);
        String bitMask = CompressedIns.substring(8,12);
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(12),2);
        String dictionaryEntry=dictionary.get(dictionaryEntryIndex);
        //int from= Integer.parseInt(startingLocation,2);
        //int to=from+4;
        String xorResult=String.join("",Collections.nCopies(Integer.parseInt(startingLocation,2),"0"))+bitMask;
        xorResult=xorResult+String.join("",Collections.nCopies(32-xorResult.length(),"0"));

        return xorCalculation(xorResult,dictionaryEntry);
    }

    private static String oneBitMismatchDeComp(String CompressedIns, List<String> dictionary){
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(9),2);
        String dictionaryEntry= dictionary.get(dictionaryEntryIndex);
        String mismatchLocation = CompressedIns.substring(3,8);
        int from = Integer.parseInt(mismatchLocation,2);
        int to = from+1;

        return dictionaryEntry.substring(0,from) + flipBits(dictionaryEntry.substring(from,to)) + dictionaryEntry.substring(to);
    }

    private static String twoBitMismatchDeComp(String CompressedIns, List<String> dictionary){
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(9),2);
        String dictionaryEntry= dictionary.get(dictionaryEntryIndex);
        String mismatchLocation =CompressedIns.substring(3,8);
        int from = Integer.parseInt(mismatchLocation,2);
        int to = from + 2;
        return dictionaryEntry.substring(0,from) + flipBits(dictionaryEntry.substring(from,to)) + dictionaryEntry.substring(to);
    }

    private static String fourBitMismatchDeComp(String CompressedIns, List<String> dictionary){
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(9),2);
        String dictionaryEntry= dictionary.get(dictionaryEntryIndex);
        String mismatchLocation = CompressedIns.substring(3,8);
        int from = Integer.parseInt(mismatchLocation,2);
        int to = from + 4;

        return dictionaryEntry.substring(0,from) + flipBits(dictionaryEntry.substring(from,to)) + dictionaryEntry.substring(to);
    }

    private static String twoBitMismatchesAnywhereDeComp(String CompressedIns, List<String> dictionary){
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(13),2);
        String dictionaryEntry= dictionary.get(dictionaryEntryIndex);
        String mismatchLocation1 = CompressedIns.substring(3,8);
        String mismatchLocation2 = CompressedIns.substring(8,13);
        int from1 = Integer.parseInt(mismatchLocation1,2);
        int to1 = from1 + 1;
        int from2 = Integer.parseInt(mismatchLocation2,2);
        int to2 = from2 + 1;
        //System.out.println(from1);
        //System.out.println(from2);

        return dictionaryEntry.substring(0,from1) + flipBits(dictionaryEntry.substring(from1,to1)) +
                dictionaryEntry.substring(to1,from2) + flipBits(dictionaryEntry.substring(from2,to2)) + dictionaryEntry.substring(to2);
    }

    private static String directDeComp(String CompressedIns, List<String> dictionary){
        int dictionaryEntryIndex = Integer.parseInt(CompressedIns.substring(3),2);
        return dictionary.get(dictionaryEntryIndex);
    }



    private static String flipBits(String stringToFlip){

        StringBuilder s= new StringBuilder();

        for (int i = 0; i < stringToFlip.length(); i++){
            if (stringToFlip.charAt(i) == '1'){
                s.append("0");
            }else {
                s.append("1");
            }
        }
        return s.toString();
    }
    private static StringBuilder compressedCodeToString (String compressedPath) throws FileNotFoundException {
        StringBuilder compressedCodeAndDictionary= new StringBuilder();
        Scanner scr=new Scanner(new File(compressedPath));
        while (scr.hasNext()){
            compressedCodeAndDictionary.append(scr.next());
        }
        scr.close();
        int a=compressedCodeAndDictionary.indexOf("x");
        String compressedCodeTxtOnly=compressedCodeAndDictionary.substring(0,a);
        String dictionaryTxtOnly=compressedCodeAndDictionary.substring(a+4,compressedCodeAndDictionary.length());
        return compressedCodeAndDictionary;
    }

    private static List<String> dictionaryTxtToList(StringBuilder compressedCodeAndDictionary){
        int a=compressedCodeAndDictionary.indexOf("x");
        String dictionaryTxtOnly=compressedCodeAndDictionary.substring(a+4);
        List<String> dictionaryEntriesList=new ArrayList<>();
        //System.out.println(dictionaryTxtOnly.length()/32);
        int count=0;
        for (int i=0;i<(dictionaryTxtOnly.length())/32;i++){
            if (count<=32*i){
                dictionaryEntriesList.add(dictionaryTxtOnly.substring(count,count+32));
            }
            count+=32;
        }
        //System.out.println(dictionaryEntriesList.size());
        return dictionaryEntriesList;
    }


    private static List<String> compressedTxtCodeToList(StringBuilder compressedCodeAndDictionary){
        int a=compressedCodeAndDictionary.indexOf("x");
        String compressedCodeTxtOnly=compressedCodeAndDictionary.substring(0,a);
        List<String> compressedCodesList=new ArrayList<>();

        for (int i =0; i < compressedCodeTxtOnly.length(); i++){
            String header = compressedCodeTxtOnly.substring(i,i+3);

            if (header.equals("000")){
                try {
                    compressedCodesList.add(compressedCodeTxtOnly.substring(i, i + 35));
                    i += 34;
                }catch (StringIndexOutOfBoundsException ex){
                    break;
                }
            }else if (header.equals("001")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+6));
                i+=5;
            }else if (header.equals("010")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+16));
                i+=15;
            }else if (header.equals("011")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+12));
                i+=11;
            }else if (header.equals("100")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+12));
                i+=11;
            }else if (header.equals("101")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+12));
                i+=11;
            }else if (header.equals("110")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+17));
                i+=16;
            }else if (header.equals("111")){
                compressedCodesList.add(compressedCodeTxtOnly.substring(i,i+7));
                i+=6;
            }
        }
        //System.out.println(compressedCodesList);
        return compressedCodesList;
    }

    private static void decompression(String compressedPath) throws FileNotFoundException {
        StringBuilder compressedCodeAndDictionary= compressedCodeToString(compressedPath);
        List<String> dictionaryEntries=dictionaryTxtToList(compressedCodeAndDictionary);
        List<String> compressedCodesList=compressedTxtCodeToList(compressedCodeAndDictionary);
        List<String> decompressedCodesList= new ArrayList<>();
        for (String compressedCode:compressedCodesList ){
            switch (compressedCode.substring(0, 3)) {
                case "000":
                    decompressedCodesList.add(originalBinaryDeComp(compressedCode));
                    break;
                case "001":
                    String previousDeCompressedCode = decompressedCodesList.get(decompressedCodesList.size() - 1);
                    List<String> rle = rleDeComp(previousDeCompressedCode, compressedCode);
                    decompressedCodesList.add(previousDeCompressedCode);
                    decompressedCodesList.addAll(rle);
                    break;
                case "010":
                    decompressedCodesList.add(bitMaskBasedDeComp(compressedCode, dictionaryEntries));
                    break;
                case "011":
                    decompressedCodesList.add(oneBitMismatchDeComp(compressedCode, dictionaryEntries));

                    break;
                case "100":
                    decompressedCodesList.add(twoBitMismatchDeComp(compressedCode, dictionaryEntries));
                    break;
                case "101":
                    decompressedCodesList.add(fourBitMismatchDeComp(compressedCode, dictionaryEntries));
                    break;
                case "110":
                    decompressedCodesList.add(twoBitMismatchesAnywhereDeComp(compressedCode, dictionaryEntries));
                    break;
                case "111":
                    decompressedCodesList.add(directDeComp(compressedCode, dictionaryEntries));
                    break;
            }
        }
        //System.out.println(decompressedCodesList);
        writeDeCompressedToTxt(decompressedCodesList);
    }
    private static void writeDeCompressedToTxt(List<String> decompressedCodesList){

        try {
            FileWriter fw = new FileWriter("dout.txt");
            for(String str: decompressedCodesList){
                fw.write(str+System.lineSeparator());
            }
            fw.close();
        }catch (IOException ex){
            System.out.println(ex);
        }
    }

}
