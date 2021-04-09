import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;


/**
 * @author Yizhen Yuan
 */
public class GetContents {
    static PDDocument pDDocument;
    static String ADDRESS;
    static String OUTPUT;
    static File file;
    static PDDocumentOutline pdDocumentOutline;
    static int indexDeviation;

    static void initialize() {
        try {
            file = new File(ADDRESS);
            pDDocument = PDDocument.load(file);
            PDFTreeNode.setRoot(StreamSupport.stream(pDDocument.getDocumentCatalog().getDocumentOutline().children().spliterator(), false).collect(Collectors.toList()));
            indexDeviation = PDFTreeNode.all.stream().filter(e -> !e.children.isEmpty()).findFirst().get().startPage - 1;
        } catch (IOException e) {
            System.out.println("Input file not found or not available");
            System.exit(1);
        }
        assert pDDocument != null;
    }


    /**
     * print the catalog of the book to the print stream
     *
     * @author Yizhen Yuan
     */
    public static void printCatalog(boolean content) {
        System.out.println("Processing Index");
        if(processIndex()) {
            System.out.println("Index initialized");
        }else {
            System.out.println("This book does not contains an index");
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT), StandardCharsets.UTF_8))) {
            bufferedWriter.write("{\n\"bookTitle\": \"" + pDDocument.getDocumentInformation().getTitle() + "\",\n");
            bufferedWriter.write("\"TOC\": [\n");
            System.out.println("Basic check done");
            String a = PDFTreeNode.showTree(content);
            bufferedWriter.write(a + "\n");
            bufferedWriter.write("]\n}\n");
            System.out.println("Writing done");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Transfer the whole pdf file into a txt file output in printStream
     *
     * @param printStream the position
     */
    public static void pdfToText(@NotNull PrintStream printStream) {
        try {
            printStream.println(new Tika().parseToString(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * a private function for inserting index into each node
     */

    //Please delete this when public, this is only for testing
    public static ArrayList<String> TESTindex = new ArrayList<>();

    //TODO: handle the case if there is no index.
    private static boolean processIndex() {
        /*
            Find the Node of Index
            From back the front, the first one contains "index" should be the index
         */
        PDFTreeNode index = null;
        for (int i = PDFTreeNode.all.size() - 1; i >= 0; i--) {
            if (PDFTreeNode.all.get(i).title.toLowerCase().contains("index")) {
                index = PDFTreeNode.all.get(i);
                break;
            }
        }
        if (index == null) return false;
        try {
            /*
                getting content of the index part
             */
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(index.startPage);
            stripper.setEndPage(index.endPage);
            String raw = stripper.getText(pDDocument);
            /*
                Split them by line and comma into tokens
             */
            String[] indexTokens = raw.split(",|\r|\n");
            List<String> tokens = Arrays.stream(indexTokens).filter(e -> GeneralTools.stringClean(e).length() > 0).filter(e -> !GeneralTools.superCompare(e, "index")).filter(e -> !e.toLowerCase().contains("see also")).collect(Collectors.toList());
            /*
                Start building the word we are currently locating
             */
            StringBuilder currentWord = new StringBuilder();
            boolean wordEnded = false;

            for (String token : tokens) {
                try {
                    if (GeneralTools.characterCount(token) > 0) {
                    /*
                        This token contains english characters, it should be
                        a key word that we are going to add to the node.index
                        or at least part of it (due to word wrapping around)
                     */
                        if (wordEnded) {
                        /*
                            The previous index is added, now we shall start a new one
                         */
                            currentWord = new StringBuilder(token);
                            wordEnded = false;
                        } else currentWord.append(token);
                    } else {
                        TESTindex.add(currentWord.toString());
                    /*
                        When there starting to be pure numbers, it should be the pages
                     */
                        wordEnded = true;
                        token = GeneralTools.stringClean(token);
                        if (token.contains("–")) {
                        /*
                            If it is a page range, we check the start page and the end page
                            and find the accurate start node and the end node
                            and we add the index to all node between (inclusively) start node and the end node
                         */
                            String[] anon = token.split("–");
                            assert anon.length == 2;
                            int startPage = Integer.parseInt(anon[0]);
                            int endPage = Integer.parseInt(anon[1]);
                            List<PDFTreeNode> startNodes = PDFTreeNode.findIndex(startPage);
                            List<PDFTreeNode> endNodes = PDFTreeNode.findIndex(endPage);
                            PDFTreeNode start = null;
                            PDFTreeNode end = null;
                            if (startNodes.size() == 1) {
                            /*
                                If there is only one node that contains that page
                             */
                                start = startNodes.get(0);
                            } else {
                                PDFTextStripper anonStripper = new PDFTextStripper();
                                anonStripper.setStartPage(startPage);
                                anonStripper.setEndPage(startPage);
                                String tempPage = anonStripper.getText(pDDocument);
                            /*
                                Find out where the first occurrence of the key word
                                finding the last Node that start before the first position of the key word
                             */
                                int firstPosition = tempPage.indexOf(currentWord.toString());
                                if (firstPosition != -1) {
                                    if (startNodes.get(0).startPageSeparateIndex > firstPosition)
                                        start = startNodes.get(0);
                                    else {
                                    /*
                                        If all section in that page start before the first
                                        position of the keyword. That means the last one would be
                                        the section the keyword belongs too.
                                     */
                                        start = startNodes.get(startNodes.size() - 1);
                                        for (int i = 0; i < startNodes.size(); i++) {
                                            if (startNodes.get(i).startPageSeparateIndex > firstPosition) {
                                                start = startNodes.get(i - 1);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (endNodes.size() == 1) {
                                end = endNodes.get(0);
                            } else {
                                PDFTextStripper anonStripper = new PDFTextStripper();
                                anonStripper.setStartPage(endPage);
                                anonStripper.setEndPage(endPage);
                                String tempPage = anonStripper.getText(pDDocument);
                                int lastPosition = tempPage.lastIndexOf(currentWord.toString());
                                if (lastPosition != -1) {
                                    for (int i = endNodes.size() - 1; i >= 0; i--) {
                                        if (endNodes.get(i).startPageSeparateIndex <= lastPosition) {
                                            end = endNodes.get(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        /*
                            Now, we should get start node and the end node
                            if some of them is null, then we default in following way:
                            start is null: do not add the index to anywhere
                            end is null: add the index to all nodes that contains that end page
                            Although the default setting here might not be best,
                            but since there usually will not be a lot of nodes in one page,
                            so this is not important
                         */
                            boolean started = false;
                            for (int i = 0; i < PDFTreeNode.all.size(); i++) {
                                PDFTreeNode curr = PDFTreeNode.all.get(i);
                                if (started) curr.index.add(currentWord.toString());
                                if (curr == start) {
                                    curr.index.add(currentWord.toString());
                                    started = true;
                                }
                                if (curr == end) {
                                    if (start != end) curr.index.add(currentWord.toString());
                                    break;
                                }
                            }
                        } else {
                        /*
                            single page
                         */
                            //token = token.replace(".", "");
                            int page = Integer.parseInt(token);
                            List<PDFTreeNode> nodes = PDFTreeNode.findIndex(page);
                            if (nodes.size() == 1) nodes.get(0).index.add(currentWord.toString());
                            else {
                                PDFTextStripper anonStripper = new PDFTextStripper();
                                anonStripper.setStartPage(page);
                                anonStripper.setEndPage(page);
                                String tempPage = anonStripper.getText(pDDocument);
                                int firstPosition = tempPage.indexOf(currentWord.toString());
                                int lastPosition = tempPage.lastIndexOf(currentWord.toString());
                                boolean start = false;
                                for (int i = 1; i < nodes.size(); i++) {
                                    PDFTreeNode curr = nodes.get(i);
                                    if (curr.startPageSeparateIndex > firstPosition) {
                                        nodes.get(i - 1).index.add(currentWord.toString());
                                        start = true;
                                    }
                                    if (curr.startPageSeparateIndex > lastPosition) {
                                        break;
                                    }
                                    if (start) curr.index.add(currentWord.toString());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    currentWord = new StringBuilder();
                    System.err.println("there is an error at ");
                    System.err.println(e.getMessage());
                    System.err.println("but it is ok");
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * a private function for displaying help information
     */
    private static void help() {
        final String name = "p2j";
        System.out.println("Usage: " + name + " INPUT [OUTPUT] [-s]");
        System.out.println("Extract the file in INPUT to the OUTPUT as a json file");
        System.out.println("  or : " + name + " INPUT [OUTPUT] -c");
        System.out.println("Extract all the content in the INPUT file to out put");
        System.out.println("-s, shorter version, which omit the popular words and the content");
        System.exit(0);
    }

    //if the decoding is not working, try gb18030
    public static void main(final String[] args) {
        String[] keyboardIn = args;
        keyboardIn = new String[]{"D:\\intellijProjects\\TreeOfKnowledges\\Used_Books\\ElementaryCalculus.pdf"};
        if (keyboardIn.length == 0 || keyboardIn.length > 3) help();
        if (keyboardIn.length == 1) {
            ADDRESS = keyboardIn[0];
            initialize();
            PrintStream printStream = null;
            OUTPUT = ADDRESS.replace(".pdf", "").concat("_output.json");
            printCatalog(true);
        }
        if (keyboardIn.length == 2) {
            if (Arrays.asList(keyboardIn).contains("-c")) {
                if (keyboardIn[0].equals("-c")) {
                    ADDRESS = keyboardIn[1];
                } else {
                    ADDRESS = keyboardIn[0];
                }
                String name = ADDRESS.replace(".pdf", "").concat("_output.txt");
                initialize();
                PrintStream printStream = null;
                try {
                    printStream = new PrintStream(new File(name));
                } catch (FileNotFoundException e) {
                    System.out.println("Can not create a output file with name " + name);
                    System.exit(1);
                }
                pdfToText(printStream);
            } else {
                if (Arrays.asList(keyboardIn).contains("-s")) {
                    if (keyboardIn[0].equals("-s")) {
                        ADDRESS = keyboardIn[1];
                    } else {
                        ADDRESS = keyboardIn[0];
                    }
                    OUTPUT = ADDRESS.replace(".pdf", "").concat("_output.json");
                    initialize();
                    PrintStream printStream = null;
                    printCatalog(false);
                    System.out.println("?");
                } else {
                    ADDRESS = keyboardIn[0];
                    initialize();
                    OUTPUT = keyboardIn[1];
                    printCatalog(true);
                }
            }
        }
        if (keyboardIn.length == 3) {
            List<String> list = Arrays.asList(keyboardIn);
            if (list.contains("-c")) {
                list = list.stream().filter(e -> !e.equals("-c")).collect(Collectors.toList());
                ADDRESS = list.get(0);
                initialize();
                PrintStream printStream = null;
                try {
                    printStream = new PrintStream(new File(list.get(1)));
                } catch (FileNotFoundException e) {
                    System.out.println("No such file " + list.get(1));
                    System.exit(1);
                }
                pdfToText(printStream);
            } else {
                if (list.contains("-s")) {
                    list = list.stream().filter(e -> !e.equals("-s")).collect(Collectors.toList());
                    ADDRESS = list.get(0);
                    initialize();
                    OUTPUT = list.get(1);
                    printCatalog(false);
                } else {
                    help();
                }
            }
        }
    }
}