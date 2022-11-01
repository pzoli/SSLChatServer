import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class ChatServer implements IMsg {
	private ServerSocket ss = null;
	private boolean exit = false;
	private boolean isSecureSocker;
	private ConcurrentLinkedDeque<ConnectionHandler> userList = new ConcurrentLinkedDeque<>();

	static SSLContext createSSLContext(String serverJks, String trustJks, String serverPwd, String trustPwd)
			throws Exception {
		KeyManagerFactory mgrFact = KeyManagerFactory.getInstance("SunX509");
		KeyStore serverStore = KeyStore.getInstance("JKS");
		serverStore.load(new FileInputStream(serverJks), serverPwd.toCharArray());
		mgrFact.init(serverStore, serverPwd.toCharArray());

		TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(new FileInputStream(trustJks), trustPwd.toCharArray());
		trustFact.init(trustStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(mgrFact.getKeyManagers(), trustFact.getTrustManagers(), null);
		return sslContext;
	}

	ChatServer(String host, int port) {
		try {
			this.ss = new ServerSocket(port, 0, InetAddress.getByName(host));
		} catch (Exception e) {
			e.printStackTrace();
			printHelp(getCommandLineOptions());
		}
	}

	ChatServer(String host, int port, String serverJks, String trustJks, String serverPwd, String trustPwd) {
		try {
			if (serverPwd == null || trustPwd == null) {
				throw new Exception("missing password params!");
			}
			SSLContext sslContext = createSSLContext(serverJks, trustJks, serverPwd, trustPwd);
			SSLServerSocketFactory fact = sslContext.getServerSocketFactory();
			this.ss = fact.createServerSocket(port, 0, InetAddress.getByName(host));
			((SSLServerSocket) this.ss).setNeedClientAuth(true);
		} catch (Exception e) {
			e.printStackTrace();
			printHelp(getCommandLineOptions());
		}
	}

	protected boolean NameIsUnique(String aName) {
		boolean result = true;
		Iterator<ConnectionHandler> iter = this.userList.iterator();
		while (iter.hasNext()) {
			ConnectionHandler item = iter.next();
			if (item.getName().equals(aName)) {
				result = false;
				break;
			}
		}
		return result;
	}

	private void dropAroundMsg(Object sender, String msg) {
		Iterator<ConnectionHandler> iter = this.userList.iterator();
		while (iter.hasNext()) {
			ConnectionHandler item = iter.next();
			if (!item.equals(sender)) {
				item.sendMsg("[COMMON]<" + ((ConnectionHandler) sender).getName() + "> " + msg);
			}
		}
	}

	private void disconnectAll(Object sender) {
		Iterator<ConnectionHandler> iter = this.userList.iterator();
		while (iter.hasNext()) {
			ConnectionHandler item = iter.next();
			item.sendMsg("[DC]");
			exit();
		}
	}

	private boolean isLocalINetAddress(ConnectionHandler sender) {
		InetAddress inetAddress = sender.cs.getInetAddress();
		return !(!inetAddress.isAnyLocalAddress() && !inetAddress.isLinkLocalAddress()
				&& !inetAddress.isLoopbackAddress());
	}

	public boolean sendMsg(Object sender, String msg) {
		if (msg.startsWith("[DC-ALL]") && isLocalINetAddress((ConnectionHandler) sender)) {
			disconnectAll(sender);
		} else {
			dropAroundMsg(sender, msg);
		}
		return true;
	}

	public boolean Dispatch(Object Sender, String Msg) {
		return sendMsg(Sender, Msg);
	}

	public boolean Login(Object sender) {
		boolean result = NameIsUnique(((ConnectionHandler) sender).getName());
		if (result) {
			this.userList.add((ConnectionHandler) sender);
			Iterator<ConnectionHandler> iter = this.userList.iterator();
			while (iter.hasNext()) {
				ConnectionHandler item = iter.next();
				item.sendMsg("[USERS+]" + ((ConnectionHandler) sender).getName());
				if (!item.equals(sender)) {
					((ConnectionHandler) sender).sendMsg("[USERS+]" + item.getName());
				}
			}
		}
		return result;
	}

	public boolean Logout(Object sender) {
		this.userList.remove(sender);
		Iterator<ConnectionHandler> iter = this.userList.iterator();
		while (iter.hasNext()) {
			ConnectionHandler item = iter.next();
			if (!item.equals(sender)) {
				item.sendMsg("[USERS-]" + ((ConnectionHandler) sender).getName());
			}
		}
		return true;
	}

	public void exit() {
		this.exit = true;
		finalize();
	}

	public void accept() {
		try {
			System.out.println("Service started.");
			while (!this.exit) {
				ConnectionHandler sh;
				if (this.isSecureSocker) {
					sh = new SSLConnectionHandler(this.ss.accept(), this);
				} else {
					sh = new ConnectionHandler(this.ss.accept(), this);
				}
				sh.start();
			}
		} catch (IOException e) {
			System.err.println("Socket accept broken down." + e.getLocalizedMessage());
		}
	}

	protected void finalize() {
		try {
			this.ss.close();
		} catch (IOException ex) {
			System.err.println("Szerver-port lezarasi hiba!");
		}
	}

	private static Options getCommandLineOptions() {
		Options options = new Options();
		OptionBuilder.isRequired(true);
		OptionBuilder.withArgName("host");
		OptionBuilder.withLongOpt("host");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Chat server host (IP or name)");
		options.addOption(OptionBuilder.create());
		OptionBuilder.isRequired(true);
		OptionBuilder.withArgName("port");
		OptionBuilder.withLongOpt("port");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Listening port number");
		options.addOption(OptionBuilder.create());
		OptionBuilder.isRequired(false);
		OptionBuilder.withArgName("serverjks");
		OptionBuilder.withLongOpt("serverjks");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("JavaKeyStore location where server private and public key stored.");
		options.addOption(OptionBuilder.create());
		OptionBuilder.isRequired(false);
		OptionBuilder.withArgName("trustjks");
		OptionBuilder.withLongOpt("trustjks");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("JavaKeyStore location where trusted clients public key stored.");
		options.addOption(OptionBuilder.create());
		OptionBuilder.isRequired(false);
		OptionBuilder.withArgName("serverpwd");
		OptionBuilder.withLongOpt("serverpwd");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("JavaKeyStore password for server key stored.");
		options.addOption(OptionBuilder.create());
		OptionBuilder.isRequired(false);
		OptionBuilder.withArgName("trustpwd");
		OptionBuilder.withLongOpt("trustpwd");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("JavaKeyStore password for trusted clients key stored.");
		options.addOption(OptionBuilder.create());
		return options;
	}

	public static void main(String[] args) {
		GnuParser gnuParser = new GnuParser();
		Options options = getCommandLineOptions();
		try {
			ChatServer server;
			CommandLine commandLine = gnuParser.parse(options, args);
			String host = commandLine.getOptionValue("host");
			String port = commandLine.getOptionValue("port");
			String serverJks = commandLine.getOptionValue("serverjks");
			String serverPwd = commandLine.getOptionValue("serverpwd");
			String trustJks = commandLine.getOptionValue("trustjks");
			String trustPwd = commandLine.getOptionValue("trustpwd");

			if (serverJks != null && trustJks != null) {
				server = new ChatServer(host, Integer.parseInt(port), serverJks, trustJks, serverPwd, trustPwd);
				server.isSecureSocker = true;
			} else {
				server = new ChatServer(host, Integer.parseInt(port));
				server.isSecureSocker = false;
			}
			server.accept();
		} catch (NumberFormatException | org.apache.commons.cli.ParseException e) {
			printHelp(options);
		}

		System.out.println("Service stopped.");
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("chatserver", options);
		System.exit(-1);
	}
}
