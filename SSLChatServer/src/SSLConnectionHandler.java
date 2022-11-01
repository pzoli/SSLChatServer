import java.io.IOException;
import java.net.Socket;
import java.security.Principal;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;


public class SSLConnectionHandler extends ConnectionHandler {
	static boolean isEndEntity(SSLSession session) throws SSLPeerUnverifiedException {
		Principal id = session.getPeerPrincipal();
		if (id instanceof X500Principal) {
			X500Principal x500 = (X500Principal) id;
			return x500.getName().equals("CN=pappzdev.integrity.hu");
		}

		return false;
	}

	SSLConnectionHandler(Socket Acs, IMsg AOwner) {
		super(Acs, AOwner);
		try {
			((SSLSocket) this.cs).startHandshake();
			isEndEntity(((SSLSocket) this.cs).getSession());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
