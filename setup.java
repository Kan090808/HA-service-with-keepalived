import java.util.*;
import java.io.*;
import java.util.Scanner;

public class setup {
  // vip
  // ip : from ip.txt
  // master : 1 or 0
  public static void main(String[] args)throws IOException {
    Runtime.getRuntime().exec("sh networkCardName.sh");
    String cardName = "";
    String getIp = "";
    Scanner reader = new Scanner(System.in);
    Scanner sc = new Scanner(System.in);
    int selectCard, master;
    String line = null;
    String vip = "";
    String conf = "";
    Process p;
    ArrayList<String> card = new ArrayList<String>();
    String file = "networkCardName.txt";
    FileReader fileReader = new FileReader(file);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    PrintWriter writer = new PrintWriter("ip.sh", "UTF-8");
    PrintWriter outputConf = new PrintWriter("keepalived.conf", "UTF-8");
    while ((line = bufferedReader.readLine()) != null) {
      card.add(line);
    }
    System.out.println("please select which network card");
    for (int i = 0 ; i < card.size(); i++) {
      int linenumber = i + 1;
      System.out.println(linenumber + " " + card.get(i));
    }
    selectCard = reader.nextInt();
    cardName = card.get(selectCard - 1);
    char[] c = cardName.toCharArray();
    cardName = "";
    for (int i = 0; i < c.length - 1; i++) {
      cardName += c[i];
    }
    getIp = "/sbin/ip -o -4 addr list " + cardName + " | awk '{print $4}' | cut -d/ -f1 > ip.txt";
    writer.println(getIp);
    System.out.println("getIp " + getIp);
    System.out.println("Master or Backup : 1 true || 0 false");
    master = reader.nextInt();
    String inMaster="";
    int prio=0;
    if(master == 1){
      inMaster = "MASTER";
      prio = 150;
    }else if(master == 0){
      inMaster = "BACKUP";
      prio = 100;
    }
    writer.close();
    Runtime.getRuntime().exec("sh ip.sh");
    System.out.println("Input virtual ip address");
    vip = sc.nextLine();
    System.out.println(vip);
    outputConf.println("vrrp_instance VI_1 {\nstate "+inMaster+"\ninterface "+cardName);
    outputConf.println("virtual_router_id 51\npriority "+prio+"\nadvert_int 1");
    outputConf.println("authentication {\nauth_type PASS\nauth_pass 00000000\n}\nvirtual_ipaddress {\n"+vip+"\n}\n}");
    Runtime.getRuntime().exec("cp keepalived.conf /etc/keepalived/");
    outputConf.close();
    reader.close();
    sc.close();
  }
}