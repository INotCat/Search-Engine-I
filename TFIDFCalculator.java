import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TFIDFCalculator{
    private Operator op;
    private List<String> bookList;
    private List<Pair> taskList;
    private List<Trie> rootList;
    private Trie totalTrie;

    public TFIDFCalculator(){
        this.bookList = new ArrayList<>();//store string 
        this.rootList = new ArrayList<>();
        this.taskList = new ArrayList<>();
        this.totalTrie = new Trie();
        this.op = new Operator();
    }
    public static void main(String[] args){
        Formulator fomulator = new Formulator();
        TFIDFCalculator tf = new TFIDFCalculator();
        //read in and parse the txt file
        tf.fileReader(args[0]);
        tf.taskReader(args[1]);
        //each string is a book, and then we make a trie for each book, and a big trie for a complete doc
        tf.op.makeTrie(tf.bookList, tf.rootList, tf.totalTrie);   
        //complete the task by iterating tasklist
        StringBuilder sb = new StringBuilder();
        for(Pair p : tf.taskList){
            double tfIdf = fomulator.tfIdf(tf.rootList, tf.totalTrie, p);
            sb.append(String.format("%.5f", tfIdf) + " ");
        }
        tf.fileWriter(sb.toString());
    }

    public void fileReader(String filename){
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
        
            while((line = br.readLine()) != null){
                //count++
                //if(op.docParser(tf.bookList, line, sb, count)) {count=0;}
                op.docParser(line, sb);
                count++;
                if(count %5 == 0){
                    bookList.add(sb.toString());
                    //reset the value
                    sb.setLength(0);
                    count=0;
                }
            }
            // If there are remaining lines less than 5
            if (sb.length() > 0) {
                bookList.add(sb.toString());
            }
        } catch (Exception e) {
            System.out.println("Error in fileReader");
            e.printStackTrace();
        }
    }

    public void taskReader(String filename){
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            Boolean Second_Line = false;
            while((line = br.readLine()) != null){
                op.taskParser(taskList, line, Second_Line);
                Second_Line = true;
            }    
        } catch (Exception e){
            System.out.println("Error in IO in taskReader");
            e.printStackTrace();
        }
}

    public void fileWriter(String content){
        File file = new File("output.txt");
        try{
            if(!file.exists()){
                if(!file.createNewFile()){
                    throw new IOException("Failed to create file: output.txt");
                }
            }
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
                bw.write(content);
            } 
        } catch(IOException e){
            System.out.println("Error when outputing file");
            e.printStackTrace();
        }
    }
}

class Operator{

    public void taskParser(List<Pair> taskList, String line, Boolean Second_Line){
        int index = 0;
        try {
            if(Second_Line){
                for(String number : line.split(" ")){
                    taskList.get(index).setIndex(number);
                    index++;
                }
            }
            else{
                for(String word : line.split(" ")){
                    Pair pr = new Pair(word);
                    taskList.add(pr);
                }
            }
        } catch (Exception e) {
            System.out.println("Error in taskParser method");
            e.printStackTrace();;
        }
    }

    public void docParser(String line, StringBuilder sb){
        line = line.toLowerCase();
        line = line.replaceAll("[^a-z]+", " ");
        line = line.trim();//need trim otherwise may exsit many space in the head
        line = line + " ";//If we do not add a space here, next line's word would stick to the previous line
        sb.append(line);
        
    }

    public void makeTrie(List<String> bookList, List<Trie> rootList, Trie totalTrie){
        try{
            for(String book : bookList){
                Trie tr = new Trie();
                for(String word : book.split("\\s+")){
                    if(!tr.search(word)){//if the word is not in the small book then add the word to the totalTrie
                        totalTrie.insert(word);//create total word for a tire
                    }
                    tr.insert(word);//we check the small book "first" to handle the frequency for big book, and then we go back to calculate the frequecy for small book
                    
                }
                tr.setTotalCount(book.split("\\s+").length);////WRONGGG HERE
                rootList.add(tr);//store the each book's root reference into the booklist
            }
        }catch (NullPointerException e) {
            System.out.println("Error in spliting the word in each book");
            e.printStackTrace();
        }
    }
}

class Formulator{
    private double tf(List<Trie> docs, Pair query) {
        try {
            int i = Integer.parseInt(query.getIndex());
            double numberDocContainTerm = docs.get(i).getFrequency(query.getSearch());
            double doc_size = docs.get(i).getTotalCount();
        
            return numberDocContainTerm / doc_size;  
        } catch (ArithmeticException e) {
            System.out.println("Divided by zero when counting TF");
            e.printStackTrace();
            return 0;
        }
        
    }
    private double idf(List<Trie> docs, Trie totalTrie, Pair query) {
        Double numberDocContainTerm = totalTrie.getFrequency(query.getSearch());
        try {
            return Math.log(docs.size() / numberDocContainTerm);
        } catch (ArithmeticException e) {
            System.out.println("Divided by zero in IDF");
            e.printStackTrace();
            return 0;
        }
    }
    
    public double tfIdf(List<Trie> eachDocList, Trie totalDoc, Pair p) {
        return tf(eachDocList, p) * idf(eachDocList, totalDoc, p);
    }
}

class Pair{
    private String search;
    private String index;
    public Pair(String search){
        this.search = search;
    }
    public void setIndex(String index){
        this.index = index;
    }
    public void showInfo(){
        System.out.println(search+"::"+index);
    }
    public String getIndex() {
        return index;
    }
    public String getSearch() {
        return search;
    }
}

class TrieNode {
    TrieNode[] children;
    boolean isEndOfWord;
    double frequency;
    double totalCount;
    public TrieNode(){
        this.children = new TrieNode[26];
        this.isEndOfWord = false;
        this.frequency = 0;
        this.totalCount = 0;
    }
}

class Trie {
    private TrieNode root;

    public Trie(){
        root = new TrieNode();
    }
    // insert a word
    public void insert(String word) {
        TrieNode node = root;
        node.totalCount++;//save the total word frequecies in root node
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) {
                node.children[c - 'a'] = new TrieNode();
            }
            node = node.children[c - 'a'];
        }
        node.isEndOfWord = true;
        node.frequency++;
    }
    //search the existence in tire
    public boolean search(String word) {
        TrieNode node = root;
        ////////////node.totalCount++;
        for (char c : word.toCharArray()) {
            node = node.children[c - 'a'];
            if (node == null) {
                return false;
            }
        }
        return node.isEndOfWord;
    }
    
    public double getFrequency(String word){
        if(search(word)){
            TrieNode node = root;
            for(char c : word.toCharArray()){
                node = node.children[c - 'a'];
            }
            return node.frequency;
        }
        else{
            return 0;
        }   
    }

    public double getTotalCount(){
        return root.totalCount;
    }

    public void setTotalCount(int times){
        root.totalCount = (Double.valueOf(times));
    }
}

