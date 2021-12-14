package utilities;

import java.util.List;

import edu.jhu.isi.grothsahai.entities.Proof;
import edu.jhu.isi.grothsahai.entities.Statement;
import it.unisa.dia.gas.jpbc.Element;

public class MessageSigned {
	
	private Element M;
	private Proof[] proofs;
	private List<Statement> statements;

	
	public MessageSigned(Message msg) {

		this.M = msg.getM();
		this.proofs = msg.getProofs();
		this.statements = msg.getStatements();
		
	}

	public Element getM() {
		return M;
	}

	public Proof[] getProofs() {
		return proofs;
	}

	public List<Statement> getStatements() {
		return statements;
	}


}
