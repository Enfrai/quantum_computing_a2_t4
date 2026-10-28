package org.mse802.sdk;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Quantum Quokka gateway and circuit-free classical baseline SDK. Author: Shuohui Liu. Date: 28 Oct 2026. */
public final class Main {
  static final String URL = System.getenv().getOrDefault("QUOKKA_URL", "http://quokka1.quokkacomputing.com/qsim/qasm");
  static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
  static String arg(String[] a, int i) { if (a.length <= i) throw new IllegalArgumentException("Missing argument " + i); return a[i]; }
  static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
  static String block(double a, double b, int i, int j) { return "ry("+a+") q["+i+"];\nry("+b+") q["+j+"];\ncx q["+i+"],q["+j+"];\n"; }
  static void pairs(List<int[]> out,int start,int end) { if(start>=end)return;int mid=(start+end-1)/2;pairs(out,start,mid);pairs(out,mid+1,end);out.add(new int[]{mid,end}); }
  public static String circuit(String bits, double[] angles) {
    int n=bits.length();if(n<2)throw new IllegalArgumentException("At least 2 pixels required");
    if(angles.length!=2*(n-1))throw new IllegalArgumentException("Expected "+(2*(n-1))+" angles");
    StringBuilder s=new StringBuilder("OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q["+n+"];\ncreg c[1];\n");
    for(int i=0;i<n;i++){char b=bits.charAt(i);if(b=='1')s.append("x q[").append(i).append("];\n");else if(b!='0')throw new IllegalArgumentException("Binary pixels only");}
    List<int[]> p=new ArrayList<>();pairs(p,0,n-1);int k=0;for(int[] pair:p)s.append(block(angles[k++],angles[k++],pair[0],pair[1]));
    return s.append("measure q[").append(n-1).append("] -> c[0];\n").toString();
  }
  static double[] angles(String raw) {if(raw.isEmpty())return new double[0];String[] ss=raw.split(",");double[] a=new double[ss.length];for(int i=0;i<a.length;i++)a[i]=Double.parseDouble(ss[i]);return a;}
  // Classical comparator: nearest Hamming distance to ideal horizontal/vertical bar patterns; no quantum gates or simulation.
  static double classical(String bits, int side) {
    if(bits.length()!=side*side)throw new IllegalArgumentException("Image size mismatch");
    int bestH=Integer.MAX_VALUE,bestV=Integer.MAX_VALUE;
    for(int mask=1;mask<(1<<side)-1;mask++) {
      int dh=0,dv=0;for(int r=0;r<side;r++)for(int c=0;c<side;c++){
        int observed=bits.charAt(r*side+c)-'0';dh+=observed!=((mask>>r)&1)?1:0;dv+=observed!=((mask>>c)&1)?1:0;
      }bestH=Math.min(bestH,dh);bestV=Math.min(bestV,dv);
    }
    return bestV<bestH?1.0:0.0; // source labels: stripes=0, bars=1; ties deterministic 0
  }
  static String quokka(String qasm,int shots)throws Exception{
    String body="{\"count\":"+shots+",\"script\":\""+esc(qasm)+"\"}";
    HttpRequest req=HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(90)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
    HttpResponse<String> response=HTTP.send(req,HttpResponse.BodyHandlers.ofString());
    if(response.statusCode()/100!=2)throw new RuntimeException("Quokka HTTP "+response.statusCode()+": "+response.body());
    return response.body();
  }
  public static void main(String[] a) {
    try {
      switch(arg(a,0)) {
        case "circuit": System.out.print(circuit(arg(a,1),angles(arg(a,2))));break;
        case "classical": System.out.println(classical(arg(a,1),Integer.parseInt(arg(a,2))));break;
        case "quokka": System.out.print(quokka(circuit(arg(a,1),angles(arg(a,2))),Integer.parseInt(arg(a,3))));break;
        default:throw new IllegalArgumentException("Use circuit|classical|quokka");
      }
    }catch(Exception ex){System.err.println(ex.getMessage());System.exit(1);}
  }
}
