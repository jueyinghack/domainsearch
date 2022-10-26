import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;
public class RedTeamTools{
	JProgressBar jpb;
	// 现在遍历到多少了
	static int now;
	// 子域名的总数量
	static int total;
	// 存放要遍历的URL
	LinkedList<String> sds = new LinkedList<>();
	// 存放成功的结果
	java.util.List<OkHttp> list = new ArrayList<>();
	//输出的文本框
	JTextArea ja;
	class searchOK extends Thread{
		String url;
		public searchOK(String url){
			this.url = url;
		}
		@Override
		public void run(){
			System.out.println(url);
			getCode(url);
		}
	}
	// 构造方法
	public RedTeamTools(String str,JTextArea ja,JProgressBar jpb)throws Exception{
		this.ja = ja;
		this.jpb = jpb;
		this.now = 0;
		ExecutorService es = Executors.newFixedThreadPool(30);
		init(str);

		for(String url:sds){
			searchOK ok = new searchOK(url);
			es.submit(ok);
		}
		es.shutdown();
	}
	// 对传入的URL进行完整性检验
	public String checkURL(String str){

		if(str==null){
			return null;
		}
		if(str.contains("http://") || str.contains("https://")){
			if(str.contains("https://www.")||str.contains("http://www.")){
				return str.substring(str.indexOf('/')+6);
			}
			return str.substring(str.indexOf('/')+2);
		}
		return str;
	}


	// 对单个的URL进行响应测试
	public void getCode(String str){
		try{
			URL url = new URL(str);
			URLConnection uc = url.openConnection();
			HttpURLConnection huc = (HttpURLConnection)uc;
			//设置请求方式
			huc.setRequestMethod("GET");
			//设置连接超时时间
			huc.setConnectTimeout(1500);
			//设置读取超时时间
			huc.setReadTimeout(1500);
			huc.connect();
			String message = huc.getHeaderField("content-length");
			int code = huc.getResponseCode();
			OkHttp ok = new OkHttp(str,message,code);
			list.add(ok);
			String old = ja.getText();
			old += ok.toString();
			ja.setText(old+"\n");
		}catch(Exception e){

		}finally{
			synchronized("a"){
				now++;
				jpb.setValue((int)(now*100L/total));
			}
		}
	}

	// 初始化要检查的URLs
	public void init(String str){
		String suburl = checkURL(str);
		try{
			BufferedReader br = new BufferedReader(new FileReader("subdomain.txt"));
			while((str=br.readLine())!=null){
				String url = "http://" + str.trim() + "." + suburl;
				sds.add(url);
			}
			total = sds.size();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	// 主方法
	public static void main(String[] agrs)throws Exception{
		new GUI();
	}
}

class GUI{
	JFrame frame,subDomain;
	public GUI()throws Exception{
		createSubDomainJPanel();
	}
	public void createSubDomainJPanel()throws Exception{
		subDomain = new JFrame();
		createMenu(subDomain);
		JPanel infoInput,showResult;
		JButton start;
		JProgressBar jpb = new JProgressBar();
		Font font = new Font("宋体",1,20);
		jpb.setFont(font);
		jpb.setStringPainted(true);//设置显示文字

		JTextField urlText = new JTextField(24);
		JTextArea ja = new JTextArea(24,42);
		ja.setLineWrap(true);
		ja.setEditable(false);
		JScrollPane js = new JScrollPane(ja);
		showResult = new JPanel();
		infoInput = new JPanel(new FlowLayout());

		start = new JButton("开始");

			start.addActionListener(ae->{
				String url = urlText.getText();
				try{
					RedTeamTools r = new RedTeamTools(url,ja,jpb);
				}catch(Exception e){
					e.printStackTrace();
				}
			});


		JLabel lab1 = new JLabel("输入URL：");

		infoInput.add(lab1);
		infoInput.add(urlText);
		infoInput.add(start);

		showResult.add(js);

		subDomain.add(infoInput,"North");
		subDomain.add(showResult,"Center");
		subDomain.add(new JPanel().add(jpb),"South");
		subDomain.setSize(800,600);
		subDomain.setVisible(true);
		subDomain.setDefaultCloseOperation(3);
	}


	public void createMenu(JFrame frame)throws Exception{
		JMenuBar jb = new JMenuBar();
		JMenu jm = new JMenu("信息收集");
		JMenuItem jmi = new JMenuItem("子域名收集");
		jm.add(jmi);
		jb.add(jm);
		frame.setJMenuBar(jb);
	}
}


class OkHttp{
	String url;
	String length;
	int code;
	public OkHttp(String url,String length,int code){
		this.url = url;
		this.length = length;
		this.code = code;
	}
	public String toString(){
		return this.url + " - " + this.code + " - " + this.length;
	}
}
