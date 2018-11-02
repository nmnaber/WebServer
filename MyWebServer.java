/*--------------------------------------------------------

1. Name / Date: Nick Naber

2. Java version used, if not the official version for the class:

build 1.8.0_152

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

In shell window:

> java MyWebServer

5. List of files needed for running the program.

 a. MyWebServer.java

5. Notes:

First line of readLine from client browser is read which sets the HTTP header based on the file type.
Default html file in the directory which is activily generated html file everytime a thread starts.
The filepath of the fake-cgi is parsed into its elements and another html file is generated to send
the results back to the client. Program was completed in IntelliJ on Windows 10.

/*--------------------------------------------------------

 */
import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.Date;


class ReadFiles {

    public void delete() {
        File newWebPage = new File("directory.html");

        if (newWebPage.exists()) {
            newWebPage.delete();
        }

    }

    public void start () {
        File newWebPage = new File("directory.html"); //make directory the default file served

        try {
            FileWriter createHTML = new FileWriter(newWebPage,false);

            File f1 = new File ( "." ) ;

            // Modified the given list files code to write to an html file
            File[] strFilesDirs = f1.listFiles ( );

            for ( int i = 0 ; i < strFilesDirs.length ; i ++ ) {
                if ( strFilesDirs[i].isDirectory ( ) ) {
                    createHTML.write("Directory: " + strFilesDirs[i] + "<br />" ); // list, but no link to other directories
                }

                else if ( strFilesDirs[i].isFile ( ) ) //had to use char becuase multiple ""
                createHTML.write("<a href= " +(char)34 + strFilesDirs[i] + (char) 34 + " >" + strFilesDirs[i]
                        + " (" + strFilesDirs[i].length ( ) + ")" + "</a><br>");
            }

            createHTML.close();
        }


        catch (IOException x){
            System.out.println ("Could not create directory");

        }


    }
}

class ListenWorker extends Thread {
    Socket sock;
    ListenWorker (Socket s) {sock = s;}

    private volatile boolean flag = true;

    public void stopRunning()
    {
        flag = false;
    }

    public void addsum(String filepath) {

        //start with /cgi/addnums.fake-cgi?person=YourName&num1=4&num2=5 HTTP/1.1

        //split on space

        String[] query = filepath.split(" ");

        String noHTTP = query[0];

        String[] nextSP = noHTTP.split("-");

        String queryCloser = nextSP[1];

        //get person=YourName&num1=4&num2=5

        String[] nameNumbers = queryCloser.split("&");

        //get person=YourName num1=4 num2=5

        String nameEdit = nameNumbers[0];
        String numbersEdit1 = nameNumbers[1];
        String numbersEdit2 = nameNumbers[2];

        String nameFinal = nameEdit.split("=")[1];
        String num1Final =  numbersEdit1.split("=")[1];
        String num2Final =  numbersEdit2.split("=")[1];

        Integer number1 = Integer.parseInt(num1Final);
        Integer number2 = Integer.parseInt(num2Final);

        Integer result = number1+number2;

        File newWebPage = new File("sumadded.html"); //changed served file to results html

        try {

            //create results html file
            FileWriter createHTML = new FileWriter(newWebPage,false);

            createHTML.write("Name: " + nameFinal + "<br />" );
            createHTML.write("Problem: " + number1 + " + " + number2 + " = " + result + "<br />" );
            createHTML.write("<a href= " + (char)34 + "/directory.html" + (char)34 + ">Back to the Directory</a><br />");

            createHTML.close();
        }


        catch (IOException x){
            System.out.println ("Could not create results page");
        }


    }

    public void run(){
        PrintStream out = null;
        BufferedReader in = null;
        BufferedOutputStream write = null;
        ReadFiles newDir = new ReadFiles(); //start a new directory
        newDir.delete(); //delete the old file
        newDir.start(); //create a new directory html file

        String file = "directory.html";
        try {
            out = new PrintStream(sock.getOutputStream());
            write = new BufferedOutputStream(sock.getOutputStream()); //create an output stream for a file
            in = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));

            //read the first line of the browser req

            String lineone = in.readLine();

            if (lineone == null) {
                sock.close();
            }

            System.out.println ("Request from a client: " + lineone);
            //split that line into command and MIME type

                String modes[] = lineone.split(" ");
                String command = modes[0];
                String filepath = modes[1];


            String contentType = "text/html"; //set def. contentType for directory

            if (filepath.length() > 1) {

                if(filepath.contains("?")) { //catch a filepath that has a ? to parse

                    System.out.println (filepath);
                        addsum(filepath);
                        file = "sumadded.html";

                }
                else {
                    //parse filepath request coming from client
                    String[] paths = filepath.split("/");

                    file = paths[1];

                    System.out.println("Now serving filename: " + file );

                    //check txt or html

                    String[] filetypeCheck = file.split("\\.");

                    String filetype = filetypeCheck[1];

                    System.out.println("File Type: " + filetype );


                    if (filetype.equals("txt") || filetype.equals("java")) {
                        contentType = "text/plain";
                    } else if (filetype.equals("html")) {
                        contentType = "text/html";
                    } else {
                        System.out.println ("Filetype not supported");
                    }
                }
            }
            while (flag) {

                File currentFile = new File(file);

                int length = (int) currentFile.length();
                //create the header based on the MIME
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Length: "+ length);
                out.println("Content-Type: " + contentType);
                out.println();
                out.flush();

                //find file data

                if (filepath.equals("/")) { //default to directory
                    currentFile = new File("directory.html");
                }

                byte[] data = new byte[length];

                FileInputStream fileInput = new FileInputStream(currentFile);

                fileInput.read(data);

                //send the file to the client
                write.write(data, 0, length);

                write.flush();


                sock.close(); // close this connection, but not the server;

            }

        } catch (IOException x) {
            System.out.println("Connection reset. Listening again...");
        }

    }

}

public class MyWebServer {

    public static boolean controlSwitch = true;

    public static void main(String a[]) throws IOException {
        int q_len = 6;
        int port = 2540;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println("Nick Naber's Web Server running at 2540.\n");
        while (controlSwitch) {

            sock = servsock.accept();
            ListenWorker listenWorker = new ListenWorker(sock);

            listenWorker.start();

            try{Thread.sleep(1000);} catch(InterruptedException ex) {}
            listenWorker.stopRunning();
        }


    }
}



