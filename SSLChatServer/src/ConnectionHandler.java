
/*    */ import java.io.BufferedReader;
/*    */ import java.io.InputStreamReader;
/*    */ import java.io.PrintWriter;
/*    */ import java.net.Socket;

/*    */
/*    */ public class ConnectionHandler extends Thread {
	protected BufferedReader input;
	protected PrintWriter output;
	protected Socket cs;
	protected IMsg Owner;
	protected String received;

	ConnectionHandler(Socket Acs, IMsg AOwner) {
		this.cs = Acs;
		this.Owner = AOwner;
	}

	public boolean sendMsg(String Msg) {
		try {
			this.output.println(Msg);
			this.output.flush();
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}

		return true;
	}

	protected boolean OpenSocket() throws Exception {
		boolean result = true;
		System.out.println("Opening connection...");
		this.input = new BufferedReader(new InputStreamReader(this.cs.getInputStream()));
		this.output = new PrintWriter(this.cs.getOutputStream());
		this.received = this.input.readLine();
		setName(this.received);
		if (this.Owner.Login(this)) {
			System.out.println(String.valueOf(this.received) + " Connected.");
			this.received = "Connected.";
		} else {
			System.out.println("Name (" + this.received + ") allready exist.");
			sendMsg("[COMMON]Name (" + this.received + ") allready exist.");
			result = false;
		}
		return result;
	}

	public void CloseSocket(boolean LoginSucc) {
		try {
			if (LoginSucc) {
				this.Owner.Logout(this);
				this.Owner.Dispatch(this, "Disconnected.");
				System.out.println(String.valueOf(getName()) + " Disconnected.");
			}
			sendMsg("DC");

			this.input.close();
			this.output.close();
			this.cs.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void run() {
		try {
			boolean result = OpenSocket();
			if (result)
				do {
					this.Owner.Dispatch(this, this.received);
					this.received = this.input.readLine();
				} while (!this.received.equals("DC") && !interrupted());
			CloseSocket(result);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
